package balbucio.browser4j.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Utility to manage SQLite JDBC connections and table initialization.
 *
 * <p>Since Browser4J uses a "per-profile" directory structure, each
 * instance of DatabaseManager typically handles a single .db file
 * inside a profile's folder.
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            LOG.severe("[SQLite] Driver not found in classpath!");
        }
    }

    private final String url;

    public DatabaseManager(Path dbFile) {
        this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath().toString().replace("\\", "/");
    }

    /** Returns a new ready-to-use Connection. Caller is responsible for closing. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /** Executes an initialization DDL script (e.g. CREATE TABLE IF NOT EXISTS). */
    public void initialize(String ddl) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
            LOG.fine("[SQLite] Table initialized at " + url);
        } catch (SQLException e) {
            LOG.severe("[SQLite] Initialization failed: " + e.getMessage());
        }
    }
}
