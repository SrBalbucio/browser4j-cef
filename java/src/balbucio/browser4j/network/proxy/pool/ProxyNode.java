package balbucio.browser4j.network.proxy.pool;

import balbucio.browser4j.network.proxy.ProxyConfig;

public class ProxyNode {
    private final ProxyConfig config;
    private ProxyStatus status;
    private int failCount;
    private long lastChecked;

    public ProxyNode(ProxyConfig config) {
        this.config = config;
        this.status = ProxyStatus.ACTIVE;
        this.failCount = 0;
        this.lastChecked = System.currentTimeMillis();
    }

    public ProxyConfig getConfig() {
        return config;
    }

    public ProxyStatus getStatus() {
        return status;
    }

    public synchronized void fail() {
        this.failCount++;
        if (this.failCount > 3) {
            this.status = ProxyStatus.FAILED;
            // logic could shift to COOLDOWN instead
        }
        this.lastChecked = System.currentTimeMillis();
    }

    public synchronized void success() {
        this.failCount = 0;
        this.status = ProxyStatus.ACTIVE;
        this.lastChecked = System.currentTimeMillis();
    }

    public synchronized void setStatus(ProxyStatus status) {
        this.status = status;
    }

    public synchronized void markCooldown() {
        this.status = ProxyStatus.COOLDOWN;
        this.lastChecked = System.currentTimeMillis();
    }

    public long getLastChecked() {
        return lastChecked;
    }
}
