package balbucio.browser4j.download.model;

/** Lifecycle states of a download. */
public enum DownloadStatus {
    /** Waiting for a concurrency slot to open. */
    QUEUED,
    /** Actively receiving bytes from the server. */
    IN_PROGRESS,
    /** User-paused; can be resumed. */
    PAUSED,
    /** All bytes received and written to disk successfully. */
    COMPLETED,
    /** Download failed due to network or disk error. */
    FAILED,
    /** Explicitly cancelled by the user or API. */
    CANCELED
}
