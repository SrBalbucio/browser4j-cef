package balbucio.browser4j.browser.profile;

import balbucio.browser4j.persistence.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Handles persistence of ProfileEntry via SQLite.
 *
 * <p>Each profile directory contains a 'profile.db' file.
 */
public class ProfileSerializer {

    private static final Logger LOG = Logger.getLogger(ProfileSerializer.class.getName());
    private static final String PROFILE_DB = "profile.db";
    private static final Gson GSON = new GsonBuilder().create();

    private static final String INIT_DDL = """
            CREATE TABLE IF NOT EXISTS profile (
                profile_id TEXT PRIMARY KEY,
                display_name TEXT,
                profile_path TEXT,
                preferences_json TEXT
            );
            CREATE TABLE IF NOT EXISTS permissions (
                origin TEXT,
                permission_type TEXT,
                status TEXT,
                last_modified DATETIME,
                PRIMARY KEY (origin, permission_type)
            );
            """;

    /** Saves a ProfileEntry to &lt;profileDir&gt;/profile.db */
    public static void save(ProfileEntry entry) throws IOException {
        Path dir = entry.getProfileDir();
        Files.createDirectories(dir);
        DatabaseManager dm = new DatabaseManager(dir.resolve(PROFILE_DB));
        dm.initialize(INIT_DDL);

        String sql = "INSERT OR REPLACE INTO profile (profile_id, display_name, profile_path, preferences_json) VALUES (?, ?, ?, ?)";
        try (Connection conn = dm.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getProfileId());
            pstmt.setString(2, entry.getDisplayName());
            pstmt.setString(3, entry.getProfilePath());
            
            // Serialize preferences to JSON string for storage in its column
            ProfilePreferences p = entry.getPreferences();
            PreferencesDto dto = new PreferencesDto(p);
            pstmt.setString(4, GSON.toJson(dto));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to save profile to SQLite", e);
        }
    }

    /** Reads a ProfileEntry from &lt;profileDir&gt;/profile.db, or null if not found. */
    public static ProfileEntry load(Path profileDir) throws IOException {
        Path dbFile = profileDir.resolve(PROFILE_DB);
        if (!Files.exists(dbFile)) return null;

        DatabaseManager dm = new DatabaseManager(dbFile);
        String sql = "SELECT profile_id, display_name, profile_path, preferences_json FROM profile LIMIT 1";
        
        try (Connection conn = dm.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                String id = rs.getString("profile_id");
                String name = rs.getString("display_name");
                String path = rs.getString("profile_path");
                String json = rs.getString("preferences_json");
                
                PreferencesDto dto = GSON.fromJson(json, PreferencesDto.class);
                return new ProfileEntry(id, name, path, dto.toPreferences());
            }
        } catch (SQLException e) {
            LOG.warning("[Profile] Failed to load profile from SQLite: " + e.getMessage());
        }
        return null;
    }

    /** Internal flat DTO for serializing preferences to/from the SQLite JSON column. */
    private static class PreferencesDto {
        String theme;
        String language;
        String timezone;
        double zoomLevel;
        java.util.Map<String, Object> customFlags;

        PreferencesDto(ProfilePreferences p) {
            this.theme       = p.getTheme().getValue();
            this.language    = p.getLanguage();
            this.timezone    = p.getTimezone();
            this.zoomLevel   = p.getZoomLevel();
            this.customFlags = p.getCustomFlags();
        }

        ProfilePreferences toPreferences() {
            ProfilePreferences.Theme themeEnum = java.util.Arrays.stream(ProfilePreferences.Theme.values())
                    .filter(t -> t.getValue().equalsIgnoreCase(theme))
                    .findFirst()
                    .orElse(ProfilePreferences.Theme.SYSTEM);

            ProfilePreferences.Builder builder = ProfilePreferences.builder()
                    .theme(themeEnum).language(language).timezone(timezone).zoomLevel(zoomLevel);
            if (customFlags != null) customFlags.forEach(builder::flag);
            return builder.build();
        }
    }
}
