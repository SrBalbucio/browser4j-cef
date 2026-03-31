package balbucio.browser4j.history.api;

import balbucio.browser4j.history.model.HistoryEntry;
import java.util.List;

/**
 * High-level API to manage browser history for a specific profile.
 */
public interface HistoryManager {

    /** Records a new visit to a URL. Increments count if already exists. */
    void recordVisit(String url, String profileId);

    /** Updates the title for a URL. */
    void updateTitle(String url, String title, String profileId);

    /** Performs a full-text search with ranking. */
    List<HistoryEntry> search(String query, String profileId, int limit);

    /** Returns recently visited pages. */
    List<HistoryEntry> getRecent(String profileId, int limit);

    /** 
     * Removes all history for a specific profile. 
     */
    void clear(String profileId);

    /** Sets the maximum number of history entries allowed (default: infinite if null). */
    void setMaxEntries(Integer maxEntries);
}
