package balbucio.browser4j.history.repository;

import balbucio.browser4j.history.model.HistoryEntry;
import balbucio.browser4j.persistence.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Stores and searches browser history using SQLite with FTS5 for fast full-text search.
 */
public class HistoryRepository {

    private static final Logger LOG = Logger.getLogger(HistoryRepository.class.getName());
    private static final String HISTORY_DB = "history.db";

    private static final String DDL_HISTORY = """
            CREATE TABLE IF NOT EXISTS history (
                id TEXT PRIMARY KEY,
                url TEXT NOT NULL,
                title TEXT,
                visit_count INTEGER DEFAULT 1,
                last_visit_time INTEGER,
                profile_id TEXT
            );
            """;

    private static final String DDL_FTS = """
            CREATE VIRTUAL TABLE IF NOT EXISTS history_fts USING fts5(
                url,
                title,
                content='history',
                content_rowid='history_rowid'
            );
            """;

    // FTS5 content='history' triggers to keep them in sync
    private static final String DDL_TRIGGERS = """
            CREATE TRIGGER IF NOT EXISTS history_ai AFTER INSERT ON history BEGIN
                INSERT INTO history_fts(rowid, url, title) VALUES (new.rowid, new.url, new.title);
            END;

            CREATE TRIGGER IF NOT EXISTS history_ad AFTER DELETE ON history BEGIN
                INSERT INTO history_fts(history_fts, rowid, url, title) VALUES('delete', old.rowid, old.url, old.title);
            END;

            CREATE TRIGGER IF NOT EXISTS history_au AFTER UPDATE ON history BEGIN
                INSERT INTO history_fts(history_fts, rowid, url, title) VALUES('delete', old.rowid, old.url, old.title);
                INSERT INTO history_fts(rowid, url, title) VALUES (new.rowid, new.url, new.title);
            END;
            """;

    private final DatabaseManager dbManager;

    public HistoryRepository(java.nio.file.Path historyPath) {
        this.dbManager = new DatabaseManager(historyPath.resolve(HISTORY_DB));
        this.dbManager.initialize(DDL_HISTORY);
        this.dbManager.initialize(DDL_FTS);
        this.dbManager.initialize(DDL_TRIGGERS);
    }

    /**
     * Updates an existing entry (increments visitCount) or inserts a new one.
     */
    public synchronized void upsert(String url, String profileId) {
        String id = generateId(url, profileId);
        String sql = """
                INSERT INTO history (id, url, visit_count, last_visit_time, profile_id)
                VALUES (?, ?, 1, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    visit_count = visit_count + 1,
                    last_visit_time = excluded.last_visit_time
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, url);
            pstmt.setLong(3, Instant.now().getEpochSecond());
            pstmt.setString(4, profileId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.severe("[History] Failed to record visit: " + e.getMessage());
        }
    }

    /**
     * Updates the title for an existing history entry. 
     */
    public synchronized void updateTitle(String url, String title, String profileId) {
        if (title == null || title.isBlank()) return;
        String id = generateId(url, profileId);
        String sql = "UPDATE history SET title = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.severe("[History] Failed to update title: " + e.getMessage());
        }
    }

    /**
     * Intelligent search using FTS5 ranking + popularity weighting.
     */
    public List<HistoryEntry> search(String query, String profileId, int limit) {
        List<HistoryEntry> results = new ArrayList<>();
        // Query ranking logic based on BM25 (-1 because BM25 lower is better), popularity, and recency
        String sql = """
                SELECT h.*,
                       (bm25(history_fts) * -1) +
                       (h.visit_count * 0.5) +
                       ((strftime('%s','now') - h.last_visit_time) * -0.00001)
                       AS score
                FROM history h
                JOIN history_fts ON h.rowid = history_fts.rowid
                WHERE history_fts MATCH ?
                  AND h.profile_id = ?
                ORDER BY score DESC
                LIMIT ?
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, query + "*");
            pstmt.setString(2, profileId);
            pstmt.setInt(3, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LOG.warning("[History] Search failed: " + e.getMessage());
        }
        return results;
    }

    /** Returns most recent visited pages. */
    public List<HistoryEntry> getRecent(String profileId, int limit) {
        List<HistoryEntry> results = new ArrayList<>();
        String sql = "SELECT * FROM history WHERE profile_id = ? ORDER BY last_visit_time DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profileId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LOG.warning("[History] Recent load failed: " + e.getMessage());
        }
        return results;
    }

    public void deleteByProfile(String profileId) {
        String sql = "DELETE FROM history WHERE profile_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profileId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.severe("[History] Clear failed: " + e.getMessage());
        }
    }

    /** Trims history to keep only the latest N entries for a profile. */
    public void trim(String profileId, int limit) {
        String sql = """
            DELETE FROM history 
            WHERE profile_id = ? 
            AND id NOT IN (
                SELECT id FROM history 
                WHERE profile_id = ? 
                ORDER BY last_visit_time DESC 
                LIMIT ?
            )
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profileId);
            pstmt.setString(2, profileId);
            pstmt.setInt(3, limit);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[History] Trim failed: " + e.getMessage());
        }
    }

    private HistoryEntry mapRow(ResultSet rs) throws SQLException {
        return HistoryEntry.builder()
                .id(rs.getString("id"))
                .url(rs.getString("url"))
                .title(rs.getString("title"))
                .visitCount(rs.getInt("visit_count"))
                .lastVisitTime(Instant.ofEpochSecond(rs.getLong("last_visit_time")))
                .profileId(rs.getString("profile_id"))
                .build();
    }

    private String generateId(String url, String profileId) {
        // Simple stable ID based on URL and Profile
        return UUID.nameUUIDFromBytes((profileId + ":" + url).getBytes()).toString();
    }
}
