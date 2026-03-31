package tests.junittests.history;

import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.api.HistoryManagerImpl;
import balbucio.browser4j.history.model.HistoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for {@link HistoryManagerImpl}.
 * Each test gets a fresh in-memory (temp-dir) SQLite database via {@code @TempDir}.
 */
class HistoryManagerImplTest {

    @TempDir
    Path tempDir;

    private HistoryManager manager;

    @BeforeEach
    void setUp() {
        // HistoryRepository internally appends "history.db" to the provided path,
        // so we must pass the directory, not a file path.
        manager = new HistoryManagerImpl(tempDir);
    }

    // ── recordVisit ───────────────────────────────────────────────────────────

    @Test
    void shouldRecordVisitAndRetrieveIt() {
        manager.recordVisit("https://example.com", "profile1");

        List<HistoryEntry> recent = manager.getRecent("profile1", 10);
        assertFalse(recent.isEmpty());
        assertEquals("https://example.com", recent.get(0).getUrl());
    }

    @Test
    void shouldIncrementVisitCountOnDuplicateUrl() {
        manager.recordVisit("https://example.com", "profile1");
        manager.recordVisit("https://example.com", "profile1");

        List<HistoryEntry> recent = manager.getRecent("profile1", 10);
        assertEquals(1, recent.size());
        assertEquals(2, recent.get(0).getVisitCount());
    }

    @Test
    void shouldIgnoreInternalUrlSchemes() {
        manager.recordVisit("about:blank",       "profile1");
        manager.recordVisit("data:text/html,hi", "profile1");
        manager.recordVisit("chrome://settings", "profile1");
        manager.recordVisit("devtools://inspector", "profile1");
        manager.recordVisit("blob:https://x.com/abc", "profile1");
        manager.recordVisit("javascript:void(0)", "profile1");

        assertTrue(manager.getRecent("profile1", 10).isEmpty());
    }

    @Test
    void shouldIgnoreNullUrl() {
        manager.recordVisit(null, "profile1");
        assertTrue(manager.getRecent("profile1", 10).isEmpty());
    }

    // ── updateTitle ───────────────────────────────────────────────────────────

    @Test
    void shouldUpdateTitleAfterRecording() {
        manager.recordVisit("https://example.com", "profile1");
        manager.updateTitle("https://example.com", "Example Site", "profile1");

        List<HistoryEntry> recent = manager.getRecent("profile1", 10);
        assertEquals("Example Site", recent.get(0).getTitle());
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    void shouldFindEntryByUrlFragment() {
        manager.recordVisit("https://docs.example.com/api", "profile1");
        manager.recordVisit("https://unrelated.org",         "profile1");

        List<HistoryEntry> results = manager.search("docs", "profile1", 10);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getUrl().contains("docs")));
    }

    @Test
    void shouldReturnRecentWhenQueryIsBlank() {
        manager.recordVisit("https://a.com", "profile1");
        manager.recordVisit("https://b.com", "profile1");

        List<HistoryEntry> results = manager.search("", "profile1", 10);
        assertEquals(2, results.size());
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    void shouldClearAllHistoryForProfile() {
        manager.recordVisit("https://a.com", "profile1");
        manager.recordVisit("https://b.com", "profile1");
        manager.recordVisit("https://c.com", "other-profile");

        manager.clear("profile1");

        assertTrue(manager.getRecent("profile1", 10).isEmpty());
        // Other profiles must not be affected
        assertFalse(manager.getRecent("other-profile", 10).isEmpty());
    }

    // ── setMaxEntries ─────────────────────────────────────────────────────────

    @Test
    void shouldTrimHistoryToMaxEntries() {
        manager.setMaxEntries(2);

        manager.recordVisit("https://a.com", "profile1");
        manager.recordVisit("https://b.com", "profile1");
        manager.recordVisit("https://c.com", "profile1");

        List<HistoryEntry> recent = manager.getRecent("profile1", 10);
        assertTrue(recent.size() <= 2,
                "Expected at most 2 entries after trim, got " + recent.size());
    }
}
