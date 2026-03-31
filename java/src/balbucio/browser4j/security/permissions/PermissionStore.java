package balbucio.browser4j.security.permissions;

import balbucio.browser4j.persistence.DatabaseManager;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Persists and retrieves permissions in the profile's profile.db.
 */
public class PermissionStore {
    private static final Logger LOG = Logger.getLogger(PermissionStore.class.getName());
    private final Path dbFile;

    public PermissionStore(Path profileDir) {
        this.dbFile = profileDir.resolve("profile.db");
    }

    public void set(String origin, PermissionType type, PermissionStatus status) {
        DatabaseManager dm = new DatabaseManager(dbFile);
        String sql = "INSERT OR REPLACE INTO permissions (origin, permission_type, status, last_modified) VALUES (?, ?, ?, ?)";
        try (Connection conn = dm.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, type.name());
            pstmt.setString(3, status.name());
            pstmt.setString(4, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("[PermissionStore] Failed to save permission: " + e.getMessage());
        }
    }

    public PermissionStatus get(String origin, PermissionType type) {
        DatabaseManager dm = new DatabaseManager(dbFile);
        String sql = "SELECT status FROM permissions WHERE origin = ? AND permission_type = ?";
        try (Connection conn = dm.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, type.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return PermissionStatus.fromString(rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            LOG.warning("[PermissionStore] Failed to load permission: " + e.getMessage());
        }
        return PermissionStatus.ASK;
    }
}
