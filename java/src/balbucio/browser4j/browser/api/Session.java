package balbucio.browser4j.browser.api;

import balbucio.browser4j.security.profile.BrowserProfile;
import balbucio.browser4j.network.proxy.ProxyConfig;
import balbucio.browser4j.network.proxy.pool.ProxyPool;

public class Session {
    private final String sessionId;
    private final BrowserProfile profile;
    private final ProxyConfig proxy;
    private final boolean incognito;
    private Object nativeContext; // Stores the CefRequestContext if isolated

    private Session(String sessionId, BrowserProfile profile, ProxyConfig proxy, boolean incognito) {
        this.sessionId = sessionId;
        this.profile = profile;
        this.proxy = proxy;
        this.incognito = incognito;
    }

    public static Session create(BrowserProfile profile) {
        return new Session(java.util.UUID.randomUUID().toString(), profile, null, false);
    }

    public static Session create(BrowserProfile profile, ProxyPool proxyPool) {
        String sId = java.util.UUID.randomUUID().toString();
        ProxyConfig assigned = (proxyPool != null) ? proxyPool.assignToSession(sId) : null;
        return new Session(sId, profile, assigned, false);
    }

    public static Session createIncognito(BrowserProfile profile) {
        return new Session(java.util.UUID.randomUUID().toString(), profile, null, true);
    }

    public static Session createIncognito(BrowserProfile profile, ProxyPool proxyPool) {
        String sId = java.util.UUID.randomUUID().toString();
        ProxyConfig assigned = (proxyPool != null) ? proxyPool.assignToSession(sId) : null;
        return new Session(sId, profile, assigned, true);
    }

    public String getSessionId() {
        return sessionId;
    }

    public BrowserProfile getProfile() {
        return profile;
    }

    public ProxyConfig getProxy() {
        return proxy;
    }

    public boolean isIncognito() {
        return incognito;
    }

    public Object getNativeContext() {
        return nativeContext;
    }

    public void setNativeContext(Object nativeContext) {
        this.nativeContext = nativeContext;
    }
}
