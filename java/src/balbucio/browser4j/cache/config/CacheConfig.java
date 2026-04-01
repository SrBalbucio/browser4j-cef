package balbucio.browser4j.cache.config;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Global configuration for the Advanced Cache Module.
 */
@Data
@Builder
@Getter
public class CacheConfig {
    /** Enable the entire caching system. */
    @Builder.Default
    private boolean enabled = false;

    /** If true, assets like common JS/CSS can be shared across multiple profiles. */
    @Builder.Default
    private boolean sharedCacheEnabled = false;

    /** Max disk usage in bytes for the cache (default 1GB). */
    @Builder.Default
    private long maxCacheSizeBytes = 1024L * 1024 * 1024; // 1GB

    /** Max number of individual files to keep in cache. */
    @Builder.Default
    private int maxEntries = 50_000;

    /** If true, the system will strictly follow Cache-Control and Expires headers. */
    @Builder.Default
    private boolean respectHttpHeaders = true;

    /** Minimum file size (in bytes) to consider GZIP compression. */
    @Builder.Default
    private int compressionThresholdBytes = 1024 * 5; // 5KB
}
