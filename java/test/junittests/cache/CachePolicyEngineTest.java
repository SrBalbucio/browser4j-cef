package tests.junittests.cache;

import balbucio.browser4j.cache.config.CacheConfig;
import balbucio.browser4j.cache.policy.CachePolicyEngine;
import org.cef.network.CefRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CachePolicyEngine}.
 *
 * {@link CefRequest} is mocked via Mockito because its constructor is package-private
 * (JCEF internal) and cannot be instantiated directly in tests.
 */
class CachePolicyEngineTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CefRequest get() {
        CefRequest req = Mockito.mock(CefRequest.class);
        when(req.getMethod()).thenReturn("GET");
        return req;
    }

    private static CefRequest post() {
        CefRequest req = Mockito.mock(CefRequest.class);
        when(req.getMethod()).thenReturn("POST");
        return req;
    }

    // ── shouldCache ───────────────────────────────────────────────────────────

    @Test
    void shouldNotCacheWhenDisabled() {
        CacheConfig cfg = CacheConfig.builder().enabled(false).build();
        assertFalse(new CachePolicyEngine(cfg).shouldCache(get(), 200, "text/html", Map.of()));
    }

    @Test
    void shouldNotCacheNonGetRequests() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        assertFalse(new CachePolicyEngine(cfg).shouldCache(post(), 200, "text/html", Map.of()));
    }

    @Test
    void shouldNotCacheNon200Status() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        CachePolicyEngine engine = new CachePolicyEngine(cfg);

        assertFalse(engine.shouldCache(get(), 404, "text/html", Map.of()));
        assertFalse(engine.shouldCache(get(), 500, "text/html", Map.of()));
        assertFalse(engine.shouldCache(get(), 301, "text/html", Map.of()));
    }

    @Test
    void shouldRespectNoCacheHeader() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).respectHttpHeaders(true).build();
        assertFalse(new CachePolicyEngine(cfg)
                .shouldCache(get(), 200, "text/html", Map.of("Cache-Control", "no-cache")));
    }

    @Test
    void shouldRespectNoStoreHeader() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).respectHttpHeaders(true).build();
        assertFalse(new CachePolicyEngine(cfg)
                .shouldCache(get(), 200, "text/html", Map.of("Cache-Control", "no-store")));
    }

    @Test
    void shouldIgnoreNoCacheHeaderWhenRespectHeadersDisabled() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).respectHttpHeaders(false).build();
        assertTrue(new CachePolicyEngine(cfg)
                .shouldCache(get(), 200, "text/html", Map.of("Cache-Control", "no-cache")));
    }

    @Test
    void shouldNotCacheWithAuthorizationHeader() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        assertFalse(new CachePolicyEngine(cfg)
                .shouldCache(get(), 200, "application/json", Map.of("Authorization", "Bearer token")));
    }

    @Test
    void shouldNotCacheWithSetCookieHeader() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        assertFalse(new CachePolicyEngine(cfg)
                .shouldCache(get(), 200, "text/html", Map.of("Set-Cookie", "session=abc")));
    }

    @Test
    void shouldNotCacheStreamingMimeTypes() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        CachePolicyEngine engine = new CachePolicyEngine(cfg);

        assertFalse(engine.shouldCache(get(), 200, "video/mp4",          Map.of()));
        assertFalse(engine.shouldCache(get(), 200, "audio/mpeg",         Map.of()));
        assertFalse(engine.shouldCache(get(), 200, "multipart/form-data", Map.of()));
    }

    @Test
    void shouldCacheStaticResources() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).build();
        CachePolicyEngine engine = new CachePolicyEngine(cfg);

        assertTrue(engine.shouldCache(get(), 200, "text/css",              Map.of()));
        assertTrue(engine.shouldCache(get(), 200, "application/javascript", Map.of()));
        assertTrue(engine.shouldCache(get(), 200, "image/png",             Map.of()));
        assertTrue(engine.shouldCache(get(), 200, "font/woff2",            Map.of()));
    }

    // ── computeExpiresAt ──────────────────────────────────────────────────────

    @Test
    void shouldUseMaxAgeFromCacheControlHeader() {
        CachePolicyEngine engine = new CachePolicyEngine(CacheConfig.builder().enabled(true).build());
        Instant before = Instant.now();
        Instant result = engine.computeExpiresAt(200, "text/html",
                Map.of("Cache-Control", "public, max-age=3600"));

        assertTrue(result.isAfter(before.plusSeconds(3599)));
        assertTrue(result.isBefore(before.plusSeconds(3601)));
    }

    @Test
    void shouldApplyOneHourTtlForJavaScript() {
        CachePolicyEngine engine = new CachePolicyEngine(CacheConfig.builder().enabled(true).build());
        Instant result = engine.computeExpiresAt(200, "application/javascript", Map.of());
        assertTrue(result.isAfter(Instant.now().plusSeconds(3500)));
    }

    @Test
    void shouldApplySevenDayTtlForImages() {
        CachePolicyEngine engine = new CachePolicyEngine(CacheConfig.builder().enabled(true).build());
        Instant result = engine.computeExpiresAt(200, "image/webp", Map.of());
        assertTrue(result.isAfter(Instant.now().plusSeconds(60 * 60 * 24 * 6)));
    }

    @Test
    void shouldApply30DayTtlForFonts() {
        CachePolicyEngine engine = new CachePolicyEngine(CacheConfig.builder().enabled(true).build());
        Instant result = engine.computeExpiresAt(200, "font/woff2", Map.of());
        assertTrue(result.isAfter(Instant.now().plusSeconds(60L * 60 * 24 * 29)));
    }

    @Test
    void shouldUseTenMinuteFallbackForUnknownMimeType() {
        CachePolicyEngine engine = new CachePolicyEngine(CacheConfig.builder().enabled(true).build());
        Instant before = Instant.now();
        Instant result = engine.computeExpiresAt(200, null, Map.of());

        assertTrue(result.isAfter(before.plusSeconds(9 * 60)));
        assertTrue(result.isBefore(before.plusSeconds(11 * 60)));
    }

    // ── shouldCompress ────────────────────────────────────────────────────────

    @Test
    void shouldNotCompressBelowThreshold() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).compressionThresholdBytes(5120).build();
        assertFalse(new CachePolicyEngine(cfg).shouldCompress("text/html", 1024));
    }

    @Test
    void shouldNotCompressAlreadyCompressedFormats() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).compressionThresholdBytes(1).build();
        CachePolicyEngine engine = new CachePolicyEngine(cfg);

        assertFalse(engine.shouldCompress("image/jpeg",      100_000));
        assertFalse(engine.shouldCompress("video/mp4",       100_000));
        assertFalse(engine.shouldCompress("audio/ogg",       100_000));
        assertFalse(engine.shouldCompress("application/zip",  100_000));
        assertFalse(engine.shouldCompress("application/pdf",  100_000));
    }

    @Test
    void shouldCompressTextualContentAboveThreshold() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).compressionThresholdBytes(1).build();
        CachePolicyEngine engine = new CachePolicyEngine(cfg);

        assertTrue(engine.shouldCompress("text/html",               100_000));
        assertTrue(engine.shouldCompress("text/css",                100_000));
        assertTrue(engine.shouldCompress("application/javascript",   100_000));
    }

    @Test
    void shouldNotCompressNullMimeType() {
        CacheConfig cfg = CacheConfig.builder().enabled(true).compressionThresholdBytes(1).build();
        assertFalse(new CachePolicyEngine(cfg).shouldCompress(null, 100_000));
    }
}
