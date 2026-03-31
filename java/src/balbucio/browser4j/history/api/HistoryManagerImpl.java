package balbucio.browser4j.history.api;

import balbucio.browser4j.history.model.HistoryEntry;
import balbucio.browser4j.history.repository.HistoryRepository;

import java.nio.file.Path;
import java.util.List;

/**
 * Common implementation for browser history management.
 */
public class HistoryManagerImpl implements HistoryManager {

    private final HistoryRepository repository;
    private Integer maxEntries = null;

    public HistoryManagerImpl(Path historyPath) {
        this.repository = new HistoryRepository(historyPath);
    }

    @Override
    public void recordVisit(String url, String profileId) {
        if (isInvalidUrl(url)) return;

        repository.upsert(url, profileId);
        
        // Trim if limit reached
        if (maxEntries != null && maxEntries > 0) {
            repository.trim(profileId, maxEntries);
        }
    }

    @Override
    public void updateTitle(String url, String title, String profileId) {
        if (isInvalidUrl(url)) return;
        repository.updateTitle(url, title, profileId);
    }

    @Override
    public List<HistoryEntry> search(String query, String profileId, int limit) {
        if (query == null || query.isBlank()) {
            return getRecent(profileId, limit);
        }
        return repository.search(query, profileId, limit);
    }

    @Override
    public List<HistoryEntry> getRecent(String profileId, int limit) {
        return repository.getRecent(profileId, limit);
    }

    @Override
    public void clear(String profileId) {
        repository.deleteByProfile(profileId);
    }

    @Override
    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
    }

    private boolean isInvalidUrl(String url) {
        if (url == null) return true;
        String lower = url.toLowerCase();
        return lower.startsWith("about:")
                || lower.startsWith("data:")
                || lower.startsWith("chrome:")
                || lower.startsWith("devtools:")
                || lower.startsWith("blob:")
                || lower.startsWith("javascript:");
    }
}
