package balbucio.browser4j.history.service;

import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.model.HistoryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates suggestions from history and simple pattern matching.
 */
public class AutocompleteService {

    private final HistoryManager historyManager;

    public AutocompleteService(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    /**
     * Searches history and provides direct URL suggestions based on user input.
     */
    public List<Suggestion> suggest(String input, String profileId, int limit) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (input == null || input.isBlank()) return suggestions;

        // 1. History matches (with ranking)
        List<HistoryEntry> entries = historyManager.search(input, profileId, limit);
        for (HistoryEntry e : entries) {
            suggestions.add(new Suggestion(e.getUrl(), 
                    e.hasTitle() ? e.getTitle() : e.getUrl(), 
                    Suggestion.SuggestionType.HISTORY));
        }

        // 2. Direct URL suggestion (if it looks like a URL but not in history)
        if (looksLikeUrl(input)) {
            String normalized = normalizeUrl(input);
            boolean alreadyIn = suggestions.stream().anyMatch(s -> s.getUrl().equalsIgnoreCase(normalized));
            if (!alreadyIn) {
                suggestions.add(0, new Suggestion(normalized, normalized, Suggestion.SuggestionType.DIRECT));
            }
        }

        return suggestions;
    }

    private boolean looksLikeUrl(String input) {
        if (input.contains(" ")) return false;
        if (input.startsWith("http://") || input.startsWith("https://")) return true;
        // Simple dot check: at least one dot not at the end, and no spaces
        int dot = input.indexOf('.');
        return dot > 0 && dot < input.length() - 1;
    }

    private String normalizeUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) return input;
        return "https://" + input;
    }
}
