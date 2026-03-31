package balbucio.browser4j.cache.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * Metadata for a single cached resource.
 */
@Data
@Builder
public class CacheEntry {
    /** Unique key (usually URL or combined URL+Method). */
    private final String key;
    /** The actual URL of the resource. */
    private final String url;
    /** Content hash (SHA-256) used for file deduplication. */
    private final String hash;
    /** MIME Type. */
    private final String mimeType;
    /** Total size in bytes of the original file. */
    private final long size;
    /** HTTP Status code when cached. */
    private final int status;
    /** Captured response headers. */
    private final Map<String, String> headers;
    /** Creation timestamp. */
    private final Instant createdAt;
    /** Expiration timestamp. */
    private final Instant expiresAt;
    /** Last time this resource was hit in the cache. */
    private final Instant lastAccessedAt;
    /** If true, the file on disk is GZIP compressed. */
    private final boolean compressed;
    /** If true, this item is shared globally. */
    private final boolean shared;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
