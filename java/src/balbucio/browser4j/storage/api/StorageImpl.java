package balbucio.browser4j.storage.api;

import balbucio.browser4j.bridge.messaging.JSBridge;
import balbucio.browser4j.bridge.serialization.JsonSerializer;
import java.util.concurrent.CompletableFuture;

public class StorageImpl implements Storage {
    private final JSBridge jsBridge;
    private final String storageType; // "localStorage" or "sessionStorage"
    private final JsonSerializer serializer;

    public StorageImpl(JSBridge jsBridge, String storageType) {
        this.jsBridge = jsBridge;
        this.storageType = storageType;
        this.serializer = new JsonSerializer();
    }

    @Override
    public CompletableFuture<String> getItem(String key) {
        try {
            String safeKey = serializer.serialize(key);
            String jsCode = String.format("return window.%s.getItem(%s);", storageType, safeKey);
            return jsBridge.evaluateJavaScript(jsCode).thenApply(result -> result == null ? null : String.valueOf(result));
        } catch (Exception e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public void setItem(String key, String value) {
        try {
            String safeKey = serializer.serialize(key);
            String safeValue = serializer.serialize(value);
            String jsCode = String.format("window.%s.setItem(%s, %s); return null;", storageType, safeKey, safeValue);
            jsBridge.evaluateJavaScript(jsCode);
        } catch (Exception e) {
            // handle error if serialization fails
        }
    }

    @Override
    public void removeItem(String key) {
        try {
            String safeKey = serializer.serialize(key);
            String jsCode = String.format("window.%s.removeItem(%s); return null;", storageType, safeKey);
            jsBridge.evaluateJavaScript(jsCode);
        } catch (Exception e) {
            // handle error
        }
    }

    @Override
    public void clear() {
        String jsCode = String.format("window.%s.clear(); return null;", storageType);
        jsBridge.evaluateJavaScript(jsCode);
    }
}
