package tests.junittests.network.dns;

import balbucio.browser4j.network.dns.FakeDnsResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FakeDnsResolverTest {

    @Test
    void shouldAddAndResolveOverride() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.addOverride("example.com", "127.0.0.1");

        assertTrue(resolver.hasOverride("example.com"));
        assertEquals("127.0.0.1", resolver.resolve("example.com"));
    }

    @Test
    void shouldRemoveOverride() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.addOverride("example.com", "127.0.0.1");

        assertNotNull(resolver.removeOverride("example.com"));
        assertNull(resolver.resolve("example.com"));
    }

    @Test
    void shouldClearAllOverrides() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.addOverride("example.com", "127.0.0.1");
        resolver.addOverride("api.example.com", "10.0.0.2");

        resolver.clearOverrides();
        assertTrue(resolver.getEntries().isEmpty());
        assertFalse(resolver.hasOverride("example.com"));
    }

    @Test
    void shouldNormalizeHostnames() {
        FakeDnsResolver resolver = new FakeDnsResolver();
        resolver.addOverride("  EXAMPLE.COM  ", "10.10.10.10");

        assertTrue(resolver.hasOverride("example.com"));
        assertEquals("10.10.10.10", resolver.resolve("example.com"));
        assertEquals("10.10.10.10", resolver.resolve("EXAMPLE.COM"));
    }

    @Test
    void shouldRejectNullOrEmptyValues() {
        FakeDnsResolver resolver = new FakeDnsResolver();

        assertThrows(IllegalArgumentException.class, () -> resolver.addOverride(null, "1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> resolver.addOverride("example.com", null));
        assertThrows(IllegalArgumentException.class, () -> resolver.addOverride("  ", "1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> resolver.addOverride("example.com", "  "));
    }
}
