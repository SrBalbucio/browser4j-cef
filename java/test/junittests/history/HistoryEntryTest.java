package tests.junittests.history;

import balbucio.browser4j.history.model.HistoryEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HistoryEntryTest {

    @Test
    void shouldReturnTrueWhenTitleIsPresent() {
        HistoryEntry entry = HistoryEntry.builder()
                .id("1")
                .url("https://example.com")
                .title("Example")
                .visitCount(1)
                .lastVisitTime(Instant.now())
                .profileId("default")
                .build();

        assertTrue(entry.hasTitle());
        assertEquals("Example", entry.getTitle());
    }

    @Test
    void shouldReturnFalseWhenTitleIsNull() {
        HistoryEntry entry = HistoryEntry.builder()
                .id("2")
                .url("https://example.com")
                .title(null)
                .visitCount(1)
                .lastVisitTime(Instant.now())
                .profileId("default")
                .build();

        assertFalse(entry.hasTitle());
    }

    @Test
    void shouldReturnFalseWhenTitleIsBlank() {
        HistoryEntry entry = HistoryEntry.builder()
                .id("3")
                .url("https://example.com")
                .title("   ")
                .visitCount(1)
                .lastVisitTime(Instant.now())
                .profileId("default")
                .build();

        assertFalse(entry.hasTitle());
    }

    @Test
    void shouldPreserveAllFields() {
        Instant now = Instant.now();
        HistoryEntry entry = HistoryEntry.builder()
                .id("uuid-1")
                .url("https://browser4j.example")
                .title("Browser4J")
                .visitCount(5)
                .lastVisitTime(now)
                .profileId("profile-A")
                .build();

        assertEquals("uuid-1",                  entry.getId());
        assertEquals("https://browser4j.example", entry.getUrl());
        assertEquals("Browser4J",               entry.getTitle());
        assertEquals(5,                          entry.getVisitCount());
        assertEquals(now,                        entry.getLastVisitTime());
        assertEquals("profile-A",               entry.getProfileId());
    }
}
