package balbucio.browser4j.cache.interception;

import balbucio.browser4j.cache.api.CacheManager;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Acts as a proxy to fetch resources from the network when they are MISSING from the cache.
 * Captures the response data and stores it in the Advanced Cache.
 */
public class NetworkResourceHandler extends CefResourceHandlerAdapter {

    private static final Logger LOG = Logger.getLogger(NetworkResourceHandler.class.getName());
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final CacheManager cacheManager;
    private byte[] data;
    private int offset = 0;
    private int statusCode = 0;
    private String contentTypeHeader;
    private String mimeType;
    private final Map<String, String> responseHeaders = new HashMap<>();
    private volatile boolean cancelled = false;
    private volatile CompletableFuture<?> pendingRequest;

    public NetworkResourceHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            // Copy headers from JCEF request to HttpClient request
            Map<String, String> requestHeaders = new HashMap<>();
            request.getHeaderMap(requestHeaders);
            requestHeaders.forEach((k, v) -> {
                // Avoid restricted headers or those that HttpClient manages
                if (!k.equalsIgnoreCase("Content-Length") && !k.equalsIgnoreCase("Host")) {
                    builder.header(k, v);
                }
            });

            pendingRequest = HTTP_CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(response -> {
                        if (cancelled) return;
                        this.statusCode = response.statusCode();
                        this.data = response.body();
                        this.contentTypeHeader = response.headers().firstValue("Content-Type").orElse(null);
                        this.mimeType = normalizeMimeType(contentTypeHeader);

                        // Copy headers for JCEF response
                        response.headers().map().forEach((k, v) -> {
                            if (!v.isEmpty()) responseHeaders.put(k, v.get(0));
                        });

                        // Store in cache (CacheManager/PolicyEngine will check if eligible)
                        cacheManager.put(request, statusCode, mimeType, responseHeaders, data);

                        callback.Continue();
                    })
                    .exceptionally(ex -> {
                        if (!cancelled) {
                            LOG.warning("[Cache] Network proxy failed for " + url + ": " + ex.getMessage());
                            callback.cancel();
                        }
                        return null;
                    });

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef response_length, StringRef redirectUrl) {
        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        responseHeaders.forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Content-Length") && !k.equalsIgnoreCase("Content-Type")) {
                response.setHeaderByName(k, v, true);
            }
        });
        if (contentTypeHeader != null && !contentTypeHeader.isBlank()) {
            response.setHeaderByName("Content-Type", contentTypeHeader, true);
        }
        response.setHeaderByName("X-Cache-Status", "MISS (Proxied)", true);
        response_length.set(data != null ? data.length : 0);
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
        cancelled = true;
        CompletableFuture<?> req = pendingRequest;
        if (req != null) req.cancel(true);
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
