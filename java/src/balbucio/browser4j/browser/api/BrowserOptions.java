package balbucio.browser4j.browser.api;

import balbucio.browser4j.network.proxy.ProxyConfig;

public class BrowserOptions {
    private final String userAgent;
    private final ProxyConfig proxy;
    private final String profilePath;
    private final Session session;

    private BrowserOptions(Builder builder) {
        this.userAgent = builder.userAgent;
        this.proxy = builder.proxy;
        this.profilePath = builder.profilePath;
        this.session = builder.session;
    }

    public String getUserAgent() { return userAgent; }
    public ProxyConfig getProxy() { return proxy; }
    public String getProfilePath() { return profilePath; }
    public Session getSession() { return session; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userAgent;
        private ProxyConfig proxy;
        private String profilePath;
        private Session session;

        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder proxy(ProxyConfig proxy) { this.proxy = proxy; return this; }
        public Builder profile(String profilePath) { this.profilePath = profilePath; return this; }
        public Builder session(Session session) { 
            this.session = session; 
            if (session != null && session.getProxy() != null) {
                this.proxy = session.getProxy();
            }
            if (session != null && session.getProfile() != null && session.getProfile().getFingerprint() != null) {
                this.userAgent = session.getProfile().getFingerprint().getUserAgent();
            }
            return this; 
        }

        public BrowserOptions build() { return new BrowserOptions(this); }
    }
}
