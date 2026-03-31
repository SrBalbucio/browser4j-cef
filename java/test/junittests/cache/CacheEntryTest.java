package tests.junittests.cache;

import balbucio.browser4j.cache.model.CacheEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheEntryTest {

    @Test
    void shouldNotBeExpiredWhenExpiresAtIsInTheFuture() {
        CacheEntry entry = CacheEntry.builder()
                .key("https://example.com/style.css")
                .url("https://example.com/style.css")
                .hash("abc123")
                .mimeType("text/css")
                .size(4096)
                .status(200)
                .headers(Map.of())
                .createdAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .lastAccessedAt(Instant.now())
                .compressed(false)
                .shared(false)
                .build();

        assertFalse(entry.isExpired());
    }

    @Test
    void shouldBeExpiredWhenExpiresAtIsInThePast() {
        CacheEntry entry = CacheEntry.builder()
                .key("https://example.com/old.js")
                .url("https://example.com/old.js")
                .mimeType("application/javascript")
                .size(1024)
                .status(200)
                .headers(Map.of())
                .createdAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(1))
                .lastAccessedAt(Instant.now().minusSeconds(3600))
                .compressed(false)
                .shared(false)
                .build();

        assertTrue(entry.isExpired());
    }

    @Test
    void shouldNotBeExpiredWhenExpiresAtIsNull() {
        CacheEntry entry = CacheEntry.builder()
                .key("no-expiry-key")
                .mimeType("image/png")
                .size(8192)
                .status(200)
                .headers(Map.of())
                .createdAt(Instant.now())
                .expiresAt(null)
                .build();

        assertFalse(entry.isExpired());
    }

    @Test
    void shouldStoreFlagsCorrectly() {
        CacheEntry compressed = CacheEntry.builder()
                .key("key")
                .mimeType("text/html")
                .size(0)
                .status(200)
                .headers(Map.of())
                .createdAt(Instant.now())
                .compressed(true)
                .shared(true)
                .build();

        assertTrue(compressed.isCompressed());
        assertTrue(compressed.isShared());
    }
}
