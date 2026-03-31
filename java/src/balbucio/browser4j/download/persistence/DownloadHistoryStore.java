package balbucio.browser4j.download.persistence;

import balbucio.browser4j.download.model.DownloadCategory;
import balbucio.browser4j.download.model.DownloadStatus;
import balbucio.browser4j.download.model.DownloadTask;
import balbucio.browser4j.persistence.DatabaseManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SQLite-based persistent store for download history.
 *
 * <p>Each profile gets its own {@code <profileDownloadDir>/history.db} file.
 */
public class DownloadHistoryStore {

    private static final Logger LOG = Logger.getLogger(DownloadHistoryStore.class.getName());
    private static final String HISTORY_DB = "history.db";

    private static final String INIT_DDL = """
            CREATE TABLE IF NOT EXISTS download_history (
                download_id TEXT PRIMARY KEY,
                url TEXT,
                file_name TEXT,
                full_path TEXT,
                mime_type TEXT,
                total_bytes INTEGER,
                received_bytes INTEGER,
                status TEXT,
                category TEXT,
                created_at TEXT,
                updated_at TEXT,
                profile_id TEXT,
                priority INTEGER,
                retry_count INTEGER,
                error_reason TEXT
            );
            """;

    private final DatabaseManager dbManager;

    public DownloadHistoryStore(Path downloadDir) {
        Path dbFile = downloadDir.resolve(HISTORY_DB);
        this.dbManager = new DatabaseManager(dbFile);
        this.dbManager.initialize(INIT_DDL);
    }

    /** Saves or updates a single task. */
    public synchronized void save(DownloadTask task) {
        String sql = """
                INSERT OR REPLACE INTO download_history (
                    download_id, url, file_name, full_path, mime_type,
                    total_bytes, received_bytes, status, category,
                    created_at, updated_at, profile_id, priority,
                    retry_count, error_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1,  task.getDownloadId());
            pstmt.setString(2,  task.getUrl());
            pstmt.setString(3,  task.getFileName());
            pstmt.setString(4,  task.getFullPath());
            pstmt.setString(5,  task.getMimeType());
            pstmt.setLong(6,    task.getTotalBytes());
            pstmt.setLong(7,    task.getReceivedBytes());
            pstmt.setString(8,  task.getStatus().name());
            pstmt.setString(9,  task.getCategory().name());
            pstmt.setString(10, task.getCreatedAt().toString());
            pstmt.setString(11, task.getUpdatedAt().toString());
            pstmt.setString(12, task.getProfileId());
            pstmt.setInt(13,    task.getPriority());
            pstmt.setInt(14,    task.getRetryCount());
            pstmt.setString(15, task.getErrorReason());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.severe("[Download] Failed to save history to SQLite: " + e.getMessage());
        }
    }

    /** Loads all tasks for this store. */
    public synchronized List<DownloadTask> loadAll() {
        List<DownloadTask> results = new ArrayList<>();
        String sql = "SELECT * FROM download_history ORDER BY created_at DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                results.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            LOG.warning("[Download] Failed to load history from SQLite: " + e.getMessage());
        }
        return results;
    }

    /** Removes all tasks matching the given profileId. */
    public synchronized void clearByProfile(String profileId) {
        String sql = "DELETE FROM download_history WHERE profile_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profileId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[Download] Failed to clear history: " + e.getMessage());
        }
    }

    /** Removes a single task by downloadId. */
    public synchronized void remove(String downloadId) {
        String sql = "DELETE FROM download_history WHERE download_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, downloadId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[Download] Failed to remove task: " + e.getMessage());
        }
    }

    private DownloadTask mapRowToTask(ResultSet rs) throws SQLException {
        return DownloadTask.builder()
                .downloadId(rs.getString("download_id"))
                .url(rs.getString("url"))
                .fileName(rs.getString("file_name"))
                .fullPath(rs.getString("full_path"))
                .mimeType(rs.getString("mime_type"))
                .totalBytes(rs.getLong("total_bytes"))
                .receivedBytes(rs.getLong("received_bytes"))
                .status(parseEnum(DownloadStatus.class, rs.getString("status"), DownloadStatus.COMPLETED))
                .category(parseEnum(DownloadCategory.class, rs.getString("category"), DownloadCategory.OTHER))
                .createdAt(Instant.parse(rs.getString("created_at")))
                .updatedAt(Instant.parse(rs.getString("updated_at")))
                .profileId(rs.getString("profile_id"))
                .priority(rs.getInt("priority"))
                .retryCount(rs.getInt("retry_count"))
                .errorReason(rs.getString("error_reason"))
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val, E fallback) {
        try { return Enum.valueOf(cls, val); } catch (Exception e) { return fallback; }
    }
}
