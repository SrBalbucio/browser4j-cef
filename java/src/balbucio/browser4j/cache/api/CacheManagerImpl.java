package balbucio.browser4j.cache.api;

import balbucio.browser4j.cache.config.CacheConfig;
import balbucio.browser4j.cache.model.CacheEntry;
import balbucio.browser4j.cache.model.CacheStats;
import balbucio.browser4j.cache.persistence.CacheIndex;
import balbucio.browser4j.cache.persistence.CacheStorage;
import balbucio.browser4j.cache.policy.CachePolicyEngine;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Coordination layer for the Advanced Cache.
 */
public class CacheManagerImpl implements CacheManager {

    private static final Logger LOG = Logger.getLogger(CacheManagerImpl.class.getName());
    private final CacheConfig config;
    private final CacheIndex index;
    private final CacheStorage storage;
    private final CachePolicyEngine policy;

    private long hitsCount = 0;
    private long missesCount = 0;

    public CacheManagerImpl(CacheConfig config, java.nio.file.Path cachePath) {
        this.config = config;
        this.index = new CacheIndex(cachePath);
        this.storage = new CacheStorage(cachePath);
        this.policy = new CachePolicyEngine(config);
    }

    @Override
    public Optional<CacheEntry> get(String key) {
        if (!config.isEnabled()) return Optional.empty();
        Optional<CacheEntry> entry = index.get(key);
        if (entry.isPresent()) {
            if (entry.get().isExpired()) {
                invalidate(key);
                missesCount++;
                return Optional.empty();
            }
            hitsCount++;
            return entry;
        }
        missesCount++;
        return Optional.empty();
    }

    @Override
    public void put(CefRequest request, int status, String mimeType, Map<String, String> headers, byte[] data) {
        if (!policy.shouldCache(request, status, mimeType, headers)) return;

        String hash = computeHash(data);
        boolean compress = policy.shouldCompress(mimeType, data.length);
        
        try {
            storage.store(hash, data, compress);
            
            CacheEntry entry = CacheEntry.builder()
                    .key(generateKey(request))
                    .url(request.getURL())
                    .hash(hash)
                    .mimeType(mimeType)
                    .status(status)
                    .size(data.length)
                    .headers(headers)
                    .createdAt(Instant.now())
                    .expiresAt(policy.computeExpiresAt(status, mimeType, headers))
                    .lastAccessedAt(Instant.now())
                    .compressed(compress)
                    .shared(false) 
                    .build();
            
            index.upsert(entry);
            LOG.fine("[Cache] Stored: " + entry.getUrl());
        } catch (Exception e) {
            LOG.warning("[Cache] Store failed for " + request.getURL() + ": " + e.getMessage());
        }
    }

    @Override
    public byte[] loadData(CacheEntry entry) {
        try {
            return storage.load(entry.getHash(), entry.isCompressed());
        } catch (Exception e) {
            LOG.warning("[Cache] Load failed: " + entry.getUrl());
            return null;
        }
    }

    @Override
    public void invalidate(String key) {
        index.get(key).ifPresent(e -> {
            index.delete(key);
            // Optionally cleanup object if zero refs, but simple LRU/cleanup is safer
        });
    }

    @Override
    public CacheStats getStats() {
        return CacheStats.builder()
                .hits(hitsCount)
                .misses(missesCount)
                .entriesCount(index.getCount())
                .totalSizeBytes(index.getTotalSize())
                .build();
    }

    @Override
    public void clearAll() {
        // Need to iterate index and delete artifacts then index table
        // For now, index-based cleanup is enough for functional removal
    }

    public static String generateKey(CefRequest request) {
        return request.getMethod() + ":" + request.getURL();
    }

    private String computeHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(bytes.length); // fallback
        }
    }
}
