package balbucio.browser4j.cache.model;

import lombok.Builder;
import lombok.Data;

/**
 * Statistics for cache usage.
 */
@Data
@Builder
public class CacheStats {
    /** Hits count. */
    private final long hits;
    /** Misses count. */
    private final long misses;
    /** Current size in bytes in use. */
    private final long totalSizeBytes;
    /** Current number of active entries. */
    private final int entriesCount;
}
