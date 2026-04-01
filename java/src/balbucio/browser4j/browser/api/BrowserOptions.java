package balbucio.browser4j.browser.api;

import balbucio.browser4j.cache.config.CacheConfig;
import balbucio.browser4j.network.proxy.ProxyConfig;
import lombok.Getter;

@Getter
public class BrowserOptions {

    private final String userAgent;
    private final ProxyConfig proxy;
    private final String profilePath;
    private final Session session;
    private final CacheConfig cacheConfig;
    private final String initialUrl;

    private BrowserOptions(Builder builder) {
        this.userAgent = builder.userAgent;
        this.proxy = builder.proxy;
        this.profilePath = builder.profilePath;
        this.session = builder.session;
        this.cacheConfig = builder.cacheConfig;
        this.initialUrl = builder.initialUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userAgent;
        private ProxyConfig proxy;
        private String profilePath;
        private Session session;
        private CacheConfig cacheConfig;
        private String initialUrl;

        public Builder initialUrl(String initialUrl) {
            this.initialUrl = initialUrl;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder proxy(ProxyConfig proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder profile(String profilePath) {
            this.profilePath = profilePath;
            return this;
        }

        public Builder cacheConfig(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
            return this;
        }

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

        public BrowserOptions build() {

            if (cacheConfig == null) {
                cacheConfig = CacheConfig.builder()
                        .enabled(false)
                        .maxCacheSizeBytes(1024L * 1024 * 1024)
                        .build();
            }

            if (initialUrl == null) {
                initialUrl = "about:blank";
            }

            return new BrowserOptions(this);
        }
    }
}
