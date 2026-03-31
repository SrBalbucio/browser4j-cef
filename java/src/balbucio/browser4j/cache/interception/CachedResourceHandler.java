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
        response.setMimeType(entry.getMimeType());
        
        // Restore cached headers
        Map<String, String> hdrs = entry.getHeaders();
        if (hdrs != null) {
            hdrs.forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Content-Length")) {
                    response.setHeaderByName(k, v, true);
                }
            });
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
}
