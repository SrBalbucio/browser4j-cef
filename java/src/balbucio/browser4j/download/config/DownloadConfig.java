package balbucio.browser4j.download.config;

import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for the Download Manager.
 * Apply once at construction time of {@link balbucio.browser4j.download.api.DownloadManagerImpl}.
 */
public class DownloadConfig {

    /** Dangerous extensions blocked by default. Configurable. */
    public static final Set<String> DEFAULT_BLOCKED_EXTENSIONS = Set.of(
            "exe","bat","cmd","com","msi","ps1","psm1","psd1","sh","bash","zsh","fish",
            "vbs","vbe","js","jse","wsf","wsh","reg","dll","so","dylib","scr","pif","cpl",
            "hta","jar","apk","ipa"
    );

    private final int        maxConcurrentDownloads;
    private final long       maxFileSizeBytes;        // -1 = unlimited
    private final Set<String> blockedExtensions;
    private final Set<String> blacklistDomains;
    private final Path       defaultDownloadDir;
    private final int        maxRetries;
    private final boolean    organizeByCategory;      // subfolders: images/, videos/, etc.

    private DownloadConfig(Builder b) {
        this.maxConcurrentDownloads = b.maxConcurrentDownloads;
        this.maxFileSizeBytes       = b.maxFileSizeBytes;
        this.blockedExtensions      = Set.copyOf(b.blockedExtensions);
        this.blacklistDomains       = Set.copyOf(b.blacklistDomains);
        this.defaultDownloadDir     = b.defaultDownloadDir;
        this.maxRetries             = b.maxRetries;
        this.organizeByCategory     = b.organizeByCategory;
    }

    public int        getMaxConcurrentDownloads() { return maxConcurrentDownloads; }
    public long       getMaxFileSizeBytes()       { return maxFileSizeBytes;       }
    public Set<String> getBlockedExtensions()     { return blockedExtensions;      }
    public Set<String> getBlacklistDomains()      { return blacklistDomains;       }
    public Path       getDefaultDownloadDir()     { return defaultDownloadDir;     }
    public int        getMaxRetries()             { return maxRetries;             }
    public boolean    isOrganizeByCategory()      { return organizeByCategory;     }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int        maxConcurrentDownloads = 3;
        private long       maxFileSizeBytes       = -1;
        private Set<String> blockedExtensions     = new HashSet<>(DEFAULT_BLOCKED_EXTENSIONS);
        private Set<String> blacklistDomains      = new HashSet<>();
        private Path       defaultDownloadDir     = Path.of(System.getProperty("user.home"), "Downloads");
        private int        maxRetries             = 3;
        private boolean    organizeByCategory     = false;

        public Builder maxConcurrentDownloads(int n)     { this.maxConcurrentDownloads = n;     return this; }
        /** Max file size in bytes. Use -1 for unlimited. */
        public Builder maxFileSizeBytes(long bytes)      { this.maxFileSizeBytes = bytes;        return this; }
        public Builder blockedExtensions(Set<String> s)  { this.blockedExtensions = new HashSet<>(s); return this; }
        public Builder blockExtension(String ext)        { this.blockedExtensions.add(ext.toLowerCase()); return this; }
        public Builder allowExtension(String ext)        { this.blockedExtensions.remove(ext.toLowerCase()); return this; }
        public Builder blacklistDomain(String domain)    { this.blacklistDomains.add(domain);    return this; }
        public Builder defaultDownloadDir(Path dir)      { this.defaultDownloadDir = dir;        return this; }
        public Builder maxRetries(int n)                 { this.maxRetries = n;                  return this; }
        public Builder organizeByCategory(boolean flag)  { this.organizeByCategory = flag;       return this; }

        public DownloadConfig build() { return new DownloadConfig(this); }
    }
}
