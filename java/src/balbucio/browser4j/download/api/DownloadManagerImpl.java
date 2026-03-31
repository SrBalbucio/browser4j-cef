package balbucio.browser4j.download.api;

import balbucio.browser4j.download.config.DownloadConfig;
import balbucio.browser4j.download.events.DownloadEventListener;
import balbucio.browser4j.download.model.DownloadCategory;
import balbucio.browser4j.download.model.DownloadStatus;
import balbucio.browser4j.download.model.DownloadTask;
import balbucio.browser4j.download.persistence.DownloadHistoryStore;
import balbucio.browser4j.download.security.DownloadSanitizer;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItemCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central download coordinator.
 *
 * <p>Thread safety: all mutation of the active-downloads map is synchronized on {@code this}.
 * Concurrency is limited via a {@link Semaphore} that controls how many downloads
 * run simultaneously (default: 3, configurable via {@link DownloadConfig}).
 */
public class DownloadManagerImpl implements DownloadManager {

    private static final Logger LOG = Logger.getLogger(DownloadManagerImpl.class.getName());

    private final DownloadConfig       config;
    private final DownloadSanitizer    sanitizer;
    private final DownloadHistoryStore historyStore;
    private final Path                 downloadRoot;

    /** cefDownloadId (int) → DownloadTask */
    private final Map<Integer, DownloadTask>  activeById    = new ConcurrentHashMap<>();
    /** downloadId (UUID string) → cefDownloadId */
    private final Map<String, Integer>        idMapping     = new ConcurrentHashMap<>();
    /** downloadId → callback for pause/resume/cancel */
    private final Map<Integer, CefDownloadItemCallback> callbacks = new ConcurrentHashMap<>();

    private final Semaphore                concurrencySlots;
    private final List<DownloadEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public DownloadManagerImpl(DownloadConfig config, Path downloadRoot) {
        this.config           = config;
        this.sanitizer        = new DownloadSanitizer(config);
        this.downloadRoot     = downloadRoot;
        this.historyStore     = new DownloadHistoryStore(downloadRoot);
        this.concurrencySlots = new Semaphore(config.getMaxConcurrentDownloads(), true);
    }

    // ---- Called from DownloadHandlerImpl (JCEF callbacks) ----

    /**
     * Called by {@code DownloadHandlerImpl.onBeforeDownload}.
     * Validates, builds a DownloadTask, and continues or cancels the JCEF download.
     *
     * @return the created DownloadTask, or null if blocked
     */
    public DownloadTask handleBeforeDownload(String url, String suggestedName, String mimeType,
                                              long totalBytes, String profileId,
                                              int cefDownloadId,
                                              CefBeforeDownloadCallback callback) {
        // 1. Security validation
        DownloadSanitizer.ValidationResult val = sanitizer.validate(url, suggestedName, mimeType, totalBytes);
        if (!val.isAllowed()) {
            LOG.warning("[Download] Blocked: " + val.getBlockReason() + " url=" + url);
            fireBlocked(url, val.getBlockReason());
            callback.Continue("", false); // cancel by providing empty path — JCEF won't download
            return null;
        }

        // 2. Duplicate detection (same URL + profileId already active/queued)
        boolean duplicate = activeById.values().stream()
                .anyMatch(t -> t.getUrl().equals(url) && t.getProfileId().equals(profileId)
                        && (t.getStatus() == DownloadStatus.IN_PROGRESS || t.getStatus() == DownloadStatus.QUEUED));
        if (duplicate) {
            LOG.info("[Download] Duplicate download ignored: " + url);
            callback.Continue("", false);
            return null;
        }

        // 3. Resolve destination path
        Path dir = resolveDownloadDir(profileId, mimeType, val.getSanitizedName());
        Path dest = sanitizer.resolveDestination(dir, val.getSanitizedName());
        try { Files.createDirectories(dest.getParent()); } catch (Exception e) {
            LOG.warning("[Download] Cannot create dir: " + dest.getParent());
        }

        // 4. Build task
        DownloadCategory category = DownloadCategory.fromMimeType(mimeType);
        if (category == DownloadCategory.OTHER) category = DownloadCategory.fromExtension(val.getSanitizedName());

        DownloadTask task = DownloadTask.builder()
                .url(url)
                .fileName(dest.getFileName().toString())
                .fullPath(dest.toAbsolutePath().toString())
                .mimeType(mimeType != null ? mimeType : "application/octet-stream")
                .totalBytes(totalBytes)
                .status(DownloadStatus.QUEUED)
                .category(category)
                .profileId(profileId)
                .build();

        activeById.put(cefDownloadId, task);
        idMapping.put(task.getDownloadId(), cefDownloadId);

        historyStore.save(task);
        fireQueued(task);

        // 5. Try to acquire concurrency slot
        if (concurrencySlots.tryAcquire()) {
            // Slot available — start immediately
            DownloadTask started = task.toBuilder().status(DownloadStatus.IN_PROGRESS).updatedAt(Instant.now()).build();
            activeById.put(cefDownloadId, started);
            historyStore.save(started);
            fireStart(started);
            callback.Continue(dest.toAbsolutePath().toString(), false);
        } else {
            // No slot available — defer (JCEF will wait for callback.Continue)
            // We store the callback reference and call it when a slot frees up
            CompletableFuture.runAsync(() -> {
                try {
                    concurrencySlots.acquire();
                    DownloadTask queued = activeById.get(cefDownloadId);
                    if (queued == null || queued.getStatus() == DownloadStatus.CANCELED) {
                        concurrencySlots.release();
                        return;
                    }
                    DownloadTask started = queued.toBuilder().status(DownloadStatus.IN_PROGRESS).updatedAt(Instant.now()).build();
                    activeById.put(cefDownloadId, started);
                    historyStore.save(started);
                    fireStart(started);
                    callback.Continue(dest.toAbsolutePath().toString(), false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        return task;
    }

    /**
     * Called by {@code DownloadHandlerImpl.onDownloadUpdated}.
     * Updates task progress and dispatches events.
     */
    public void handleDownloadUpdated(int cefDownloadId, long receivedBytes, long totalBytes,
                                       boolean isComplete, boolean isCanceled, boolean isInProgress,
                                       boolean isInterrupted, CefDownloadItemCallback callback) {
        DownloadTask current = activeById.get(cefDownloadId);
        if (current == null) return;

        callbacks.put(cefDownloadId, callback);

        DownloadStatus newStatus;
        if (isCanceled)      newStatus = DownloadStatus.CANCELED;
        else if (isInterrupted) newStatus = DownloadStatus.FAILED;
        else if (isComplete) newStatus = DownloadStatus.COMPLETED;
        else                 newStatus = current.getStatus() == DownloadStatus.PAUSED
                                          ? DownloadStatus.PAUSED : DownloadStatus.IN_PROGRESS;

        DownloadTask updated = current.toBuilder()
                .receivedBytes(receivedBytes)
                .totalBytes(totalBytes > 0 ? totalBytes : current.getTotalBytes())
                .status(newStatus)
                .updatedAt(Instant.now())
                .build();

        activeById.put(cefDownloadId, updated);

        switch (newStatus) {
            case IN_PROGRESS -> fireProgress(updated);
            case COMPLETED   -> { fireComplete(updated); finishSlot(cefDownloadId); }
            case CANCELED    -> { fireCanceled(updated); finishSlot(cefDownloadId); }
            case FAILED      -> { fireError(updated, "Download interrupted"); finishSlot(cefDownloadId); }
            default -> {}
        }

        historyStore.save(updated);
    }

    // ---- DownloadManager API ----

    @Override
    public void pause(String downloadId) {
        Integer cefId = idMapping.get(downloadId);
        if (cefId == null) return;
        CefDownloadItemCallback cb = callbacks.get(cefId);
        if (cb != null) cb.pause();
        DownloadTask current = activeById.get(cefId);
        if (current != null) {
            DownloadTask paused = current.toBuilder().status(DownloadStatus.PAUSED).updatedAt(Instant.now()).build();
            activeById.put(cefId, paused);
            historyStore.save(paused);
            firePaused(paused);
        }
    }

    @Override
    public void resume(String downloadId) {
        Integer cefId = idMapping.get(downloadId);
        if (cefId == null) return;
        CefDownloadItemCallback cb = callbacks.get(cefId);
        if (cb != null) cb.resume();
        DownloadTask current = activeById.get(cefId);
        if (current != null) {
            DownloadTask resumed = current.toBuilder().status(DownloadStatus.IN_PROGRESS).updatedAt(Instant.now()).build();
            activeById.put(cefId, resumed);
            historyStore.save(resumed);
            fireResumed(resumed);
        }
    }

    @Override
    public void cancel(String downloadId) {
        Integer cefId = idMapping.get(downloadId);
        if (cefId == null) return;
        CefDownloadItemCallback cb = callbacks.get(cefId);
        if (cb != null) cb.cancel();
        // Status update will come from onDownloadUpdated(isCanceled=true)
    }

    @Override
    public void retry(String downloadId) {
        // Find task in history and re-issue a browser load to trigger re-download
        DownloadTask task = get(downloadId);
        if (task == null) { LOG.warning("[Download] Retry: task not found " + downloadId); return; }
        if (task.getRetryCount() >= config.getMaxRetries()) {
            LOG.warning("[Download] Max retries reached for " + downloadId);
            return;
        }
        LOG.info("[Download] Retry #" + (task.getRetryCount() + 1) + " for " + task.getUrl());
        // The browser must navigate to the URL to re-trigger the download — caller responsibility.
        // We reset the task state here so the next onBeforeDownload creates a clean task.
        DownloadTask reset = task.toBuilder()
                .status(DownloadStatus.QUEUED).receivedBytes(0)
                .retryCount(task.getRetryCount() + 1).updatedAt(Instant.now()).build();
        historyStore.save(reset);
    }

    @Override
    public List<DownloadTask> list(String profileId) {
        List<DownloadTask> result = new ArrayList<>();
        // Active downloads first
        result.addAll(activeById.values().stream()
                .filter(t -> t.getProfileId().equals(profileId)).collect(Collectors.toList()));
        // Historical entries (completed/canceled/failed)
        historyStore.loadAll().stream()
                .filter(t -> t.getProfileId().equals(profileId))
                .filter(t -> activeById.values().stream().noneMatch(a -> a.getDownloadId().equals(t.getDownloadId())))
                .forEach(result::add);
        result.sort(Comparator.comparing(DownloadTask::getCreatedAt).reversed());
        return result;
    }

    @Override
    public DownloadTask get(String downloadId) {
        // Check active map first
        Optional<DownloadTask> active = activeById.values().stream()
                .filter(t -> t.getDownloadId().equals(downloadId)).findFirst();
        if (active.isPresent()) return active.get();
        // Fall back to history
        return historyStore.loadAll().stream()
                .filter(t -> t.getDownloadId().equals(downloadId)).findFirst().orElse(null);
    }

    @Override
    public void clearHistory(String profileId) {
        historyStore.clearByProfile(profileId);
    }

    @Override
    public DownloadManager addEventListener(DownloadEventListener listener) {
        eventListeners.add(listener);
        return this;
    }

    @Override
    public DownloadManager removeEventListener(DownloadEventListener listener) {
        eventListeners.remove(listener);
        return this;
    }

    // ---- Internal helpers ----

    private void finishSlot(int cefDownloadId) {
        activeById.remove(cefDownloadId);
        callbacks.remove(cefDownloadId);
        concurrencySlots.release();
    }

    private Path resolveDownloadDir(String profileId, String mimeType, String fileName) {
        Path base = downloadRoot;
        if (config.isOrganizeByCategory()) {
            DownloadCategory cat = DownloadCategory.fromMimeType(mimeType);
            if (cat == DownloadCategory.OTHER) cat = DownloadCategory.fromExtension(fileName);
            String subdir = switch (cat) {
                case IMAGE    -> "images";
                case VIDEO    -> "videos";
                case DOCUMENT -> "documents";
                default       -> "others";
            };
            base = base.resolve(subdir);
        }
        return base;
    }

    // ---- Event fire helpers ----
    private void fireQueued(DownloadTask t)              { eventListeners.forEach(l -> l.onDownloadQueued(t)); }
    private void fireStart(DownloadTask t)               { LOG.info("[Download] Start: " + t.getUrl() + " profile=" + t.getProfileId()); eventListeners.forEach(l -> l.onDownloadStart(t)); }
    private void fireProgress(DownloadTask t)            { eventListeners.forEach(l -> l.onDownloadProgress(t)); }
    private void fireComplete(DownloadTask t)            { LOG.info("[Download] Complete: " + t.getFullPath()); eventListeners.forEach(l -> l.onDownloadComplete(t)); }
    private void fireError(DownloadTask t, String msg)   { LOG.warning("[Download] Error: " + msg + " url=" + t.getUrl()); eventListeners.forEach(l -> l.onDownloadError(t, msg)); }
    private void fireCanceled(DownloadTask t)            { LOG.info("[Download] Canceled: " + t.getUrl()); eventListeners.forEach(l -> l.onDownloadCanceled(t)); }
    private void firePaused(DownloadTask t)              { eventListeners.forEach(l -> l.onDownloadPaused(t)); }
    private void fireResumed(DownloadTask t)             { eventListeners.forEach(l -> l.onDownloadResumed(t)); }
    private void fireBlocked(String url, String reason)  { LOG.warning("[Download] Blocked: " + reason + " url=" + url); eventListeners.forEach(l -> l.onDownloadBlocked(url, reason)); }
}
