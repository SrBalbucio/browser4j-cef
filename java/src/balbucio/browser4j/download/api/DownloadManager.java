package balbucio.browser4j.download.api;

import balbucio.browser4j.download.events.DownloadEventListener;
import balbucio.browser4j.download.model.DownloadTask;

import java.util.List;

/**
 * Public API for managing file downloads triggered by the browser.
 *
 * <p>Obtain an instance via {@code browser.downloads()}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * DownloadManager dm = browser.downloads();
 *
 * // React to download events
 * dm.addEventListener(new DownloadEventListener() {
 *     public void onDownloadProgress(DownloadTask task) {
 *         System.out.printf("%s: %d%%%n", task.getFileName(), task.getProgressPercent());
 *     }
 * });
 *
 * // List all downloads for a profile
 * dm.list("my-profile").forEach(t -> System.out.println(t.getStatus() + " " + t.getFileName()));
 *
 * // Cancel a specific download
 * dm.cancel(someDownloadId);
 * }</pre>
 */
public interface DownloadManager {

    // ---- Lifecycle controls ----

    /** Pauses an active download. No-op if already paused or completed. */
    void pause(String downloadId);

    /** Resumes a paused download. */
    void resume(String downloadId);

    /** Cancels an active or paused download. */
    void cancel(String downloadId);

    /**
     * Manually retries a FAILED or CANCELED download by re-triggering
     * the browser to navigate to the original download URL.
     */
    void retry(String downloadId);

    // ---- Query ----

    /** Returns all tracked downloads for the given profileId. */
    List<DownloadTask> list(String profileId);

    /** Returns a specific download task by its ID, or null if not found. */
    DownloadTask get(String downloadId);

    // ---- History management ----

    /** Clears the on-disk history for the given profileId. Active downloads are not affected. */
    void clearHistory(String profileId);

    // ---- Event listeners ----

    DownloadManager addEventListener(DownloadEventListener listener);
    DownloadManager removeEventListener(DownloadEventListener listener);
}
