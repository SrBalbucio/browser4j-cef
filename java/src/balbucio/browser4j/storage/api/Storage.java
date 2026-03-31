package balbucio.browser4j.storage.api;

import java.util.concurrent.CompletableFuture;

public interface Storage {
    CompletableFuture<String> getItem(String key);
    void setItem(String key, String value);
    void removeItem(String key);
    void clear();
}
