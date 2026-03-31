package balbucio.browser4j.cache.interception;

import balbucio.browser4j.cache.api.CacheManager;
import balbucio.browser4j.cache.model.CacheEntry;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.network.CefRequest;

import java.util.Optional;

/**
 * JCEF implementation to route requests through the Advanced Cache.
 */
public class CacheInterceptor extends CefResourceRequestHandlerAdapter {

    private final CacheManager cacheManager;

    public CacheInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public CefResourceHandler getResourceHandler(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, CefRequest request) {
        String key = request.getMethod() + ":" + request.getURL();
        Optional<CacheEntry> entry = cacheManager.get(key);
        
        if (entry.isPresent()) {
            return new CachedResourceHandler(cacheManager, entry.get());
        }
        
        // If it's a candidate for cache (GET request), proxy it to capture the result
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return new NetworkResourceHandler(cacheManager);
        }
        
        return null; // Normal Chromium handling
    }
}
