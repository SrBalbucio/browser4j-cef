package balbucio.browser4j.storage.api;

public interface StorageModule {
    Storage getLocalStorage();
    Storage getSessionStorage();
}
