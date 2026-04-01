package balbucio.browser4j.cache.interception;

import balbucio.browser4j.cache.api.CacheManager;
import balbucio.browser4j.cache.model.CacheEntry;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.util.Map;

/**
 * Serves resources directly from the local Advanced Cache back to Chromium.
 */
public class CachedResourceHandler extends CefResourceHandlerAdapter {

    private final CacheManager cacheManager;
    private final CacheEntry entry;
    private byte[] data;
    private int offset = 0;

    public CachedResourceHandler(CacheManager cacheManager, CacheEntry entry) {
        this.cacheManager = cacheManager;
        this.entry = entry;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        this.data = cacheManager.loadData(entry);
        if (this.data != null) {
            callback.Continue();
            return true;
        }
        return false;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef response_length, StringRef redirectUrl) {
        response.setStatus(entry.getStatus());
        response.setMimeType(normalizeMimeType(entry.getMimeType()));
        
        // Restore cached headers
        Map<String, String> hdrs = entry.getHeaders();
        String contentTypeHeader = null;
        if (hdrs != null) {
            contentTypeHeader = hdrs.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("Content-Type"))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            hdrs.forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Content-Length") && !k.equalsIgnoreCase("Content-Type")) {
                    response.setHeaderByName(k, v, true);
                }
            });
        }

        if (contentTypeHeader != null && !contentTypeHeader.isBlank()) {
            response.setHeaderByName("Content-Type", contentTypeHeader, true);
        }
        
        response.setHeaderByName("X-Cache-Status", "HIT", true);
        response_length.set(data.length);
    }

    @Override
    public boolean readResponse(byte[] data_out, int bytes_to_read, IntRef bytes_read, CefCallback callback) {
        if (data == null) return false;
        
        int available = data.length - offset;
        if (available <= 0) return false;

        int toCopy = Math.min(available, bytes_to_read);
        System.arraycopy(data, offset, data_out, 0, toCopy);
        offset += toCopy;
        bytes_read.set(toCopy);
        return true;
    }

    @Override
    public void cancel() {
        data = null;
    }

    private static String normalizeMimeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }

        int separator = contentType.indexOf(';');
        String base = separator >= 0 ? contentType.substring(0, separator) : contentType;
        base = base.trim();
        return base.isEmpty() ? "application/octet-stream" : base;
    }
}
