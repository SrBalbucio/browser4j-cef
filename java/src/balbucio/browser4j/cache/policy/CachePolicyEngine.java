package balbucio.browser4j.cache.policy;

import balbucio.browser4j.cache.config.CacheConfig;
import org.cef.network.CefRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Intelligent engine to decide cache policies per resource.
 */
public class CachePolicyEngine {

    private final CacheConfig config;

    public CachePolicyEngine(CacheConfig config) {
        this.config = config;
    }

    public boolean shouldCache(CefRequest request, int status, String mime, Map<String, String> headers) {
        if (!config.isEnabled()) return false;

        // 1. Only GET method for cache (standard)
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;

        // 2. Only status 200 (standard)
        if (status != 200) return false;

        // 3. Check for Cache-Control: no-cache / no-store
        String cc = headers.getOrDefault("Cache-Control", "");
        if (cc.toLowerCase().contains("no-store") || cc.toLowerCase().contains("no-cache")) {
            if (config.isRespectHttpHeaders()) return false;
        }

        // 4. Exclude Authorization / Set-Cookie headers for security
        if (headers.containsKey("Authorization") || headers.containsKey("Set-Cookie")) return false;

        // 5. Exclude blacklisted MIME types (streaming or multi-part)
        if (mime != null) {
            String lowerMime = mime.toLowerCase();
            return !lowerMime.startsWith("video/") 
                    && !lowerMime.startsWith("audio/") 
                    && !lowerMime.contains("multipart/");
        }

        return true;
    }

    public Instant computeExpiresAt(int status, String mime, Map<String, String> headers) {
        // a. Try Cache-Control: max-age
        String cc = headers.getOrDefault("Cache-Control", "");
        if (cc.toLowerCase().contains("max-age=")) {
            try {
                int maxAge = Integer.parseInt(cc.split("max-age=")[1].split("[,; ]")[0]);
                return Instant.now().plus(Duration.ofSeconds(maxAge));
            } catch (Exception e) {}
        }

        // b. Fallback to default TTL based on MIME
        if (mime != null) {
            String lower = mime.toLowerCase();
            if (lower.contains("javascript") || lower.contains("css")) return Instant.now().plus(Duration.ofHours(1));
            if (lower.contains("image")) return Instant.now().plus(Duration.ofDays(7));
            if (lower.contains("font")) return Instant.now().plus(Duration.ofDays(30));
        }

        return Instant.now().plus(Duration.ofMinutes(10)); // Default fallback
    }

    public boolean shouldCompress(String mime, long size) {
        if (size < config.getCompressionThresholdBytes()) return false;
        if (mime == null) return false;

        String lower = mime.toLowerCase();
        // Exclusion list: Don't compress already compressed or binary formats
        return !lower.startsWith("image/") 
                && !lower.startsWith("video/") 
                && !lower.startsWith("audio/") 
                && !lower.contains("zip") 
                && !lower.contains("pdf");
    }
}
