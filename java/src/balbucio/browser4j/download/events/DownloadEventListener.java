package balbucio.browser4j.download.events;

import balbucio.browser4j.download.model.DownloadTask;

/**
 * Observer interface for download lifecycle events.
 * All methods have empty default implementations so you only override what you need.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * browser.downloads().addEventListener(new DownloadEventListener() {
 *     public void onDownloadProgress(DownloadTask task) {
 *         System.out.printf("[%s] %.1f%% %s%n",
 *             task.getDownloadId(), task.getProgressPercent() * 1.0, task.getFileName());
 *     }
 *     public void onDownloadComplete(DownloadTask task) {
 *         System.out.println("Done! File at: " + task.getFullPath());
 *     }
 * });
 * }</pre>
 */
public interface DownloadEventListener {

    /** Fired once when the download begins (transitions to IN_PROGRESS). */
    default void onDownloadStart(DownloadTask task) {}

    /** Fired at each progress update — may be called hundreds of times per download. */
    default void onDownloadProgress(DownloadTask task) {}

    /** Fired when the download finishes successfully (COMPLETED). */
    default void onDownloadComplete(DownloadTask task) {}

    /** Fired when the download fails (FAILED). {@code reason} contains the error description. */
    default void onDownloadError(DownloadTask task, String reason) {}

    /** Fired when the download is cancelled by the user or the API. */
    default void onDownloadCanceled(DownloadTask task) {}

    /** Fired when a download is validated and placed in the QUEUED state. */
    default void onDownloadQueued(DownloadTask task) {}

    /** Fired when a download is paused. */
    default void onDownloadPaused(DownloadTask task) {}

    /** Fired when a paused download is resumed. */
    default void onDownloadResumed(DownloadTask task) {}

    /** Fired when a download is blocked by the security layer. */
    default void onDownloadBlocked(String url, String reason) {}
}
