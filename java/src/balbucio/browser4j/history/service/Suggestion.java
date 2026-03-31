package balbucio.browser4j.history.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result from the Autocomplete system.
 */
@Data
@AllArgsConstructor
public class Suggestion {

    public enum SuggestionType {
        /** Matches found in navigation history. */
        HISTORY,
        /** Direct navigation to the URL typed. */
        DIRECT,
        /** Search engine suggestion (future expansion). */
        SEARCH
    }

    private final String url;
    private final String title;
    private final SuggestionType type;

}
