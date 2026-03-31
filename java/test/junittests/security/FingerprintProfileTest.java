package tests.junittests.security;

import balbucio.browser4j.security.profile.FingerprintProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FingerprintProfileTest {

    @Test
    void shouldHaveDefaultValues() {
        FingerprintProfile fp = FingerprintProfile.builder().build();

        assertEquals("Mozilla/5.0", fp.getUserAgent());
        assertEquals("en-US,en;q=0.9", fp.getAcceptLanguage());
        assertEquals("America/New_York", fp.getTimezone());
        assertEquals(1920, fp.getScreenWidth());
        assertEquals(1080, fp.getScreenHeight());
        assertEquals("Win32", fp.getPlatform());
        assertEquals(4, fp.getHardwareConcurrency());
        assertEquals(8, fp.getDeviceMemory());
    }

    @Test
    void shouldOverrideUserAgent() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .userAgent("CustomAgent/1.0")
                .build();

        assertEquals("CustomAgent/1.0", fp.getUserAgent());
        // Other fields remain at their defaults
        assertEquals("en-US,en;q=0.9", fp.getAcceptLanguage());
    }

    @Test
    void shouldSetCustomScreenResolution() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .screen(2560, 1440)
                .build();

        assertEquals(2560, fp.getScreenWidth());
        assertEquals(1440, fp.getScreenHeight());
    }

    @Test
    void shouldSetCustomHardwareConcurrency() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .hardwareConcurrency(16)
                .build();

        assertEquals(16, fp.getHardwareConcurrency());
    }

    @Test
    void shouldSetCustomDeviceMemory() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .deviceMemory(32)
                .build();

        assertEquals(32, fp.getDeviceMemory());
    }

    @Test
    void shouldSetCustomTimezoneAndLanguage() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .timezone("America/Sao_Paulo")
                .acceptLanguage("pt-BR,pt;q=0.9")
                .build();

        assertEquals("America/Sao_Paulo", fp.getTimezone());
        assertEquals("pt-BR,pt;q=0.9", fp.getAcceptLanguage());
    }

    @Test
    void shouldSetCustomPlatform() {
        FingerprintProfile fp = FingerprintProfile.builder()
                .platform("Linux x86_64")
                .build();

        assertEquals("Linux x86_64", fp.getPlatform());
    }
}
