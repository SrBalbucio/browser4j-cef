package balbucio.browser4j.network.api;

import java.util.Map;

public interface NetworkModule {
    void onRequest(RequestHandler handler);
    void onResponse(ResponseHandler handler);
    void setCacheInterceptor(balbucio.browser4j.cache.interception.CacheInterceptor interceptor);

    void addFakeDnsEntry(String hostname, String destination);
    void removeFakeDnsEntry(String hostname);
    void clearFakeDnsEntries();
    Map<String, String> getFakeDnsEntries();
}
