package balbucio.browser4j.network.proxy;

public class ProxyConfig {
    private final String host;
    private final int port;
    private final String type;
    private final String username;
    private final String password;
    private final String bypassList;

    private ProxyConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.type = builder.type;
        this.username = builder.username;
        this.password = builder.password;
        this.bypassList = builder.bypassList;
    }

    public static ProxyConfig http(String host, int port) {
        return new Builder().type("http").host(host).port(port).build();
    }

    public static ProxyConfig socks5(String host, int port) {
        return new Builder().type("socks5").host(host).port(port).build();
    }

    public String getServerString() {
        if (type == null || host == null) return null;
        return type + "://" + host + ":" + port;
    }

    public String getBypassList() { return bypassList; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public static class Builder {
        private String host;
        private int port;
        private String type; // e.g. http, https, socks5
        private String username;
        private String password;
        private String bypassList;

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }
        public Builder bypass(String bypassList) {
            this.bypassList = bypassList;
            return this;
        }
        public ProxyConfig build() { return new ProxyConfig(this); }
    }
}
