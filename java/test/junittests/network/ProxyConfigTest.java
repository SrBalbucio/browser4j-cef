package tests.junittests.network;

import balbucio.browser4j.network.proxy.ProxyConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProxyConfigTest {

    @Test
    void shouldBuildHttpProxyServerString() {
        ProxyConfig config = ProxyConfig.http("proxy.example.com", 8080);
        assertEquals("http://proxy.example.com:8080", config.getServerString());
    }

    @Test
    void shouldBuildSocks5ProxyServerString() {
        ProxyConfig config = ProxyConfig.socks5("127.0.0.1", 1080);
        assertEquals("socks5://127.0.0.1:1080", config.getServerString());
    }

    @Test
    void shouldReturnNullServerStringWhenHostIsNull() {
        ProxyConfig config = new ProxyConfig.Builder()
                .type("http")
                .build();
        assertNull(config.getServerString());
    }

    @Test
    void shouldReturnNullServerStringWhenTypeIsNull() {
        ProxyConfig config = new ProxyConfig.Builder()
                .host("proxy.example.com")
                .port(8080)
                .build();
        assertNull(config.getServerString());
    }

    @Test
    void shouldStoreCredentials() {
        ProxyConfig config = new ProxyConfig.Builder()
                .host("proxy.example.com")
                .port(8080)
                .type("http")
                .credentials("user", "pass")
                .build();

        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
    }

    @Test
    void shouldStoreBypassList() {
        ProxyConfig config = new ProxyConfig.Builder()
                .host("proxy.example.com")
                .port(8080)
                .type("http")
                .bypass("localhost,127.0.0.1")
                .build();

        assertEquals("localhost,127.0.0.1", config.getBypassList());
    }

    @Test
    void shouldHaveNullCredentialsByDefault() {
        ProxyConfig config = ProxyConfig.http("proxy.example.com", 3128);
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getBypassList());
    }
}
