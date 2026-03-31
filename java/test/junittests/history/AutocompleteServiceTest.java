package tests.junittests.history;

import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.model.HistoryEntry;
import balbucio.browser4j.history.service.AutocompleteService;
import balbucio.browser4j.history.service.Suggestion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutocompleteServiceTest {

    // ── stub ──────────────────────────────────────────────────────────────────

    private static HistoryEntry entry(String url, String title) {
        return HistoryEntry.builder()
                .id("x")
                .url(url)
                .title(title)
                .visitCount(1)
                .lastVisitTime(Instant.now())
                .profileId("default")
                .build();
    }

    // ── blank input ───────────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyListForNullInput() {
        HistoryManager manager = mock(HistoryManager.class);
        AutocompleteService svc = new AutocompleteService(manager);

        assertTrue(svc.suggest(null, "default", 10).isEmpty());
        verifyNoInteractions(manager);
    }

    @Test
    void shouldReturnEmptyListForBlankInput() {
        HistoryManager manager = mock(HistoryManager.class);
        AutocompleteService svc = new AutocompleteService(manager);

        assertTrue(svc.suggest("   ", "default", 10).isEmpty());
        verifyNoInteractions(manager);
    }

    // ── history suggestions ───────────────────────────────────────────────────

    @Test
    void shouldReturnHistorySuggestionsFromManager() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("example", "default", 5))
                .thenReturn(List.of(entry("https://example.com", "Example")));

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("example", "default", 5);

        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream()
                .anyMatch(s -> s.getType() == Suggestion.SuggestionType.HISTORY));
    }

    @Test
    void shouldUseTitleFromHistoryWhenAvailable() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("wiki", "default", 10))
                .thenReturn(List.of(entry("https://en.wikipedia.org", "Wikipedia")));

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("wiki", "default", 10);

        assertEquals("Wikipedia", suggestions.get(0).getTitle());
    }

    @Test
    void shouldFallbackToUrlWhenTitleIsMissing() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("no-title", "default", 10))
                .thenReturn(List.of(entry("https://no-title.example.com", null)));

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("no-title", "default", 10);

        assertEquals("https://no-title.example.com", suggestions.get(0).getTitle());
    }

    // ── direct URL detection ──────────────────────────────────────────────────

    @Test
    void shouldPrependDirectSuggestionForHttpsUrl() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("https://new.example.com", "default", 10)).thenReturn(List.of());

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("https://new.example.com", "default", 10);

        assertFalse(suggestions.isEmpty());
        assertEquals(Suggestion.SuggestionType.DIRECT, suggestions.get(0).getType());
        assertEquals("https://new.example.com", suggestions.get(0).getUrl());
    }

    @Test
    void shouldNormalizeDomainInputToHttps() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("browser4j.dev", "default", 10)).thenReturn(List.of());

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("browser4j.dev", "default", 10);

        assertFalse(suggestions.isEmpty());
        assertEquals(Suggestion.SuggestionType.DIRECT, suggestions.get(0).getType());
        assertEquals("https://browser4j.dev", suggestions.get(0).getUrl());
    }

    @Test
    void shouldNotAddDirectSuggestionForInputWithSpaces() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("hello world", "default", 10)).thenReturn(List.of());

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("hello world", "default", 10);

        assertTrue(suggestions.stream().noneMatch(s -> s.getType() == Suggestion.SuggestionType.DIRECT));
    }

    @Test
    void shouldNotAddDuplicateDirectSuggestionIfAlreadyInHistory() {
        HistoryManager manager = mock(HistoryManager.class);
        when(manager.search("https://example.com", "default", 10))
                .thenReturn(List.of(entry("https://example.com", "Example")));

        AutocompleteService svc = new AutocompleteService(manager);
        List<Suggestion> suggestions = svc.suggest("https://example.com", "default", 10);

        long directCount = suggestions.stream()
                .filter(s -> s.getType() == Suggestion.SuggestionType.DIRECT)
                .count();
        assertEquals(0, directCount, "Direct suggestion should not be added when URL already in history");
    }
}
