package balbucio.browser4j.storage.api;

import balbucio.browser4j.bridge.messaging.JSBridge;

public class StorageModuleImpl implements StorageModule {
    private final Storage localStorage;
    private final Storage sessionStorage;

    public StorageModuleImpl(JSBridge jsBridge) {
        this.localStorage = new StorageImpl(jsBridge, "localStorage");
        this.sessionStorage = new StorageImpl(jsBridge, "sessionStorage");
    }

    @Override
    public Storage getLocalStorage() {
        return localStorage;
    }

    @Override
    public Storage getSessionStorage() {
        return sessionStorage;
    }
}
