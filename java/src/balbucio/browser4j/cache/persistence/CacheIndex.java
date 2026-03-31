package balbucio.browser4j.cache.persistence;

import balbucio.browser4j.cache.model.CacheEntry;
import balbucio.browser4j.persistence.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Persist cache metadata in a SQLite database.
 */
public class CacheIndex {

    private static final Logger LOG = Logger.getLogger(CacheIndex.class.getName());
    private static final Gson GSON = new Gson();
    private static final String INDEX_DB = "cache_index.db";

    private static final String DDL_CACHE = """
            CREATE TABLE IF NOT EXISTS cache (
                key TEXT PRIMARY KEY,
                url TEXT NOT NULL,
                hash TEXT NOT NULL,
                mime_type TEXT,
                status INTEGER,
                size INTEGER,
                headers_json TEXT,
                created_at INTEGER,
                expires_at INTEGER,
                last_access INTEGER,
                compressed INTEGER DEFAULT 0,
                is_shared INTEGER DEFAULT 0
            );
            CREATE INDEX IF NOT EXISTS idx_cache_url ON cache(url);
            CREATE INDEX IF NOT EXISTS idx_cache_expiration ON cache(expires_at);
            """;

    private final DatabaseManager dbManager;

    public CacheIndex(java.nio.file.Path cachePath) {
        this.dbManager = new DatabaseManager(cachePath.resolve(INDEX_DB));
        this.dbManager.initialize(DDL_CACHE);
    }

    public synchronized void upsert(CacheEntry entry) {
        String sql = """
                INSERT INTO cache (key, url, hash, mime_type, status, size, headers_json, created_at, expires_at, last_access, compressed, is_shared)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    hash = excluded.hash,
                    mime_type = excluded.mime_type,
                    status = excluded.status,
                    size = excluded.size,
                    headers_json = excluded.headers_json,
                    expires_at = excluded.expires_at,
                    last_access = excluded.last_access,
                    compressed = excluded.compressed
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entry.getKey());
            pstmt.setString(2, entry.getUrl());
            pstmt.setString(3, entry.getHash());
            pstmt.setString(4, entry.getMimeType());
            pstmt.setInt(5, entry.getStatus());
            pstmt.setLong(6, entry.getSize());
            pstmt.setString(7, GSON.toJson(entry.getHeaders()));
            pstmt.setLong(8, entry.getCreatedAt().getEpochSecond());
            pstmt.setLong(9, entry.getExpiresAt() != null ? entry.getExpiresAt().getEpochSecond() : 0);
            pstmt.setLong(10, Instant.now().getEpochSecond());
            pstmt.setInt(11, entry.isCompressed() ? 1 : 0);
            pstmt.setInt(12, entry.isShared() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.severe("[Cache] Failed to upsert index: " + e.getMessage());
        }
    }

    public Optional<CacheEntry> get(String key) {
        String sql = "SELECT * FROM cache WHERE key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    updateLastAccess(key);
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LOG.warning("[Cache] Failed to get index: " + e.getMessage());
        }
        return Optional.empty();
    }

    private void updateLastAccess(String key) {
        String sql = "UPDATE cache SET last_access = ? WHERE key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, Instant.now().getEpochSecond());
            pstmt.setString(2, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public void delete(String key) {
        String sql = "DELETE FROM cache WHERE key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public List<String> getExpiredHashes() {
        List<String> hashes = new ArrayList<>();
        String sql = "SELECT hash FROM cache WHERE expires_at > 0 AND expires_at < ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, Instant.now().getEpochSecond());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) hashes.add(rs.getString("hash"));
            }
        } catch (SQLException e) {}
        return hashes;
    }

    public long getTotalSize() {
        String sql = "SELECT SUM(size) FROM cache";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {}
        return 0;
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM cache";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }

    private CacheEntry mapRow(ResultSet rs) throws SQLException {
        return CacheEntry.builder()
                .key(rs.getString("key"))
                .url(rs.getString("url"))
                .hash(rs.getString("hash"))
                .mimeType(rs.getString("mime_type"))
                .status(rs.getInt("status"))
                .size(rs.getLong("size"))
                .headers(GSON.fromJson(rs.getString("headers_json"), new TypeToken<Map<String, String>>() {}.getType()))
                .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")))
                .expiresAt(rs.getLong("expires_at") > 0 ? Instant.ofEpochSecond(rs.getLong("expires_at")) : null)
                .lastAccessedAt(Instant.ofEpochSecond(rs.getLong("last_access")))
                .compressed(rs.getInt("compressed") == 1)
                .shared(rs.getInt("is_shared") == 1)
                .build();
    }
}
