package balbucio.browser4j.history.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Represents a single visit record in the browser history.
 */
@Data
@Builder(toBuilder = true)
public class HistoryEntry {
    /** Unique record ID (usually UUID or derived from URL+Profile). */
    private final String id;
    /** The navigated URL. */
    private final String url;
    /** The page title (may be updated asynchronously). */
    private final String title;
    /** Total number of times this URL was visited by the specific profile. */
    private final int visitCount;
    /** Timestamp of the most recent visit. */
    private final Instant lastVisitTime;
    /** The ID of the profile that performed the visit. */
    private final String profileId;

    /** 
     * Helper to check if the entry has a valid title. 
     */
    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }
}
