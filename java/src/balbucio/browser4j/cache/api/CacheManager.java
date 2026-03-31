package balbucio.browser4j.cache.api;

import balbucio.browser4j.cache.model.CacheEntry;
import balbucio.browser4j.cache.model.CacheStats;
import org.cef.network.CefRequest;

import java.util.Map;
import java.util.Optional;

/**
 * High-level API to interact with the Advanced Cache.
 */
public interface CacheManager {

    /** Returns metadata and allows retrieval of data for a given key. */
    Optional<CacheEntry> get(String key);

    /** Stores a resource in the cache. */
    void put(CefRequest request, int status, String mimeType, Map<String, String> headers, byte[] data);

    /** Loads raw bytes for a cache entry. */
    byte[] loadData(CacheEntry entry);

    /** Invalidates a specific entry. */
    void invalidate(String key);

    /** Returns usage statistics. */
    CacheStats getStats();

    /** Clears all cache entries for this profile. */
    void clearAll();
}
