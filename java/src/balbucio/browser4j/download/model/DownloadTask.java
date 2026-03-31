package balbucio.browser4j.download.model;

import java.time.Instant;

/**
 * Immutable snapshot model of a single download, updated via {@link #toBuilder()}.
 * All fields from the INSTRUCTIONS.md spec are present.
 */
public class DownloadTask {

    private final String         downloadId;
    private final String         url;
    private final String         fileName;
    private final String         fullPath;
    private final String         mimeType;
    private final long           totalBytes;
    private final long           receivedBytes;
    private final DownloadStatus status;
    private final DownloadCategory category;
    private final Instant        createdAt;
    private final Instant        updatedAt;
    private final String         profileId;
    private final int            priority;       // higher = runs first
    private final int            retryCount;
    private final String         errorReason;

    private DownloadTask(Builder b) {
        this.downloadId    = b.downloadId;
        this.url           = b.url;
        this.fileName      = b.fileName;
        this.fullPath      = b.fullPath;
        this.mimeType      = b.mimeType;
        this.totalBytes    = b.totalBytes;
        this.receivedBytes = b.receivedBytes;
        this.status        = b.status;
        this.category      = b.category;
        this.createdAt     = b.createdAt;
        this.updatedAt     = b.updatedAt;
        this.profileId     = b.profileId;
        this.priority      = b.priority;
        this.retryCount    = b.retryCount;
        this.errorReason   = b.errorReason;
    }

    public String          getDownloadId()    { return downloadId;    }
    public String          getUrl()           { return url;           }
    public String          getFileName()      { return fileName;      }
    public String          getFullPath()      { return fullPath;      }
    public String          getMimeType()      { return mimeType;      }
    public long            getTotalBytes()    { return totalBytes;    }
    public long            getReceivedBytes() { return receivedBytes; }
    public DownloadStatus  getStatus()        { return status;        }
    public DownloadCategory getCategory()     { return category;      }
    public Instant         getCreatedAt()     { return createdAt;     }
    public Instant         getUpdatedAt()     { return updatedAt;     }
    public String          getProfileId()     { return profileId;     }
    public int             getPriority()      { return priority;      }
    public int             getRetryCount()    { return retryCount;    }
    public String          getErrorReason()   { return errorReason;   }

    /** 0–100 progress percentage, or -1 if total is unknown. */
    public int getProgressPercent() {
        if (totalBytes <= 0) return -1;
        return (int) Math.min(100, (receivedBytes * 100L) / totalBytes);
    }

    /** Returns a builder pre-populated with this task's values for creating an updated copy. */
    public Builder toBuilder() {
        return new Builder()
                .downloadId(downloadId).url(url).fileName(fileName).fullPath(fullPath)
                .mimeType(mimeType).totalBytes(totalBytes).receivedBytes(receivedBytes)
                .status(status).category(category).createdAt(createdAt).updatedAt(updatedAt)
                .profileId(profileId).priority(priority).retryCount(retryCount).errorReason(errorReason);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String          downloadId    = java.util.UUID.randomUUID().toString();
        private String          url           = "";
        private String          fileName      = "";
        private String          fullPath      = "";
        private String          mimeType      = "application/octet-stream";
        private long            totalBytes    = -1;
        private long            receivedBytes = 0;
        private DownloadStatus  status        = DownloadStatus.QUEUED;
        private DownloadCategory category     = DownloadCategory.OTHER;
        private Instant         createdAt     = Instant.now();
        private Instant         updatedAt     = Instant.now();
        private String          profileId     = "global";
        private int             priority      = 0;
        private int             retryCount    = 0;
        private String          errorReason   = null;

        public Builder downloadId(String v)         { this.downloadId    = v; return this; }
        public Builder url(String v)                { this.url           = v; return this; }
        public Builder fileName(String v)           { this.fileName      = v; return this; }
        public Builder fullPath(String v)           { this.fullPath      = v; return this; }
        public Builder mimeType(String v)           { this.mimeType      = v; return this; }
        public Builder totalBytes(long v)           { this.totalBytes    = v; return this; }
        public Builder receivedBytes(long v)        { this.receivedBytes = v; return this; }
        public Builder status(DownloadStatus v)     { this.status        = v; return this; }
        public Builder category(DownloadCategory v) { this.category      = v; return this; }
        public Builder createdAt(Instant v)         { this.createdAt     = v; return this; }
        public Builder updatedAt(Instant v)         { this.updatedAt     = v; return this; }
        public Builder profileId(String v)          { this.profileId     = v; return this; }
        public Builder priority(int v)              { this.priority      = v; return this; }
        public Builder retryCount(int v)            { this.retryCount    = v; return this; }
        public Builder errorReason(String v)        { this.errorReason   = v; return this; }

        public DownloadTask build() { return new DownloadTask(this); }
    }
}
