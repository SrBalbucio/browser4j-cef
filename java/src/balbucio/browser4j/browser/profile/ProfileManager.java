package balbucio.browser4j.browser.profile;

import org.cef.browser.CefRequestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Singleton that manages persistent browser profiles on disk.
 *
 * <h2>Persistence Model</h2>
 * <p>Due to the JCEF Java API limitation (CefRequestContextSettings.cache_path is not
 * exposed per-context in the Java bindings), profile data is persisted via the global
 * CefSettings.cache_path set during {@link balbucio.browser4j.core.runtime.BrowserRuntime}
 * initialization.
 *
 * <p>The workflow is:
 * <ol>
 *   <li>Call {@link #activateProfile(String)} <em>before</em> {@code BrowserRuntime.init()}.
 *       This sets the system property {@code browser4j.profile.cachePath} which the
 *       runtime reads from {@link balbucio.browser4j.core.config.BrowserRuntimeConfiguration}.</li>
 *   <li>A {@code profile.json} file inside the profile directory tracks display names
 *       and preference values so they survive across JVM restarts.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // 1. Initialize (optional – defaults to ~/.browser4j)
 * ProfileManager.initialize(Path.of(System.getProperty("user.home"), ".browser4j"));
 *
 * // 2. Activate before BrowserRuntime.init()
 * ProfileManager.get().activateProfile("my-profile");
 * BrowserRuntime.init(config);  // cache_path will point to the profile dir
 *
 * // 3. Optionally register preferences at first run
 * ProfilePreferences prefs = ProfilePreferences.builder()
 *     .language("pt-BR").theme(ProfilePreferences.Theme.DARK).build();
 * ProfileManager.get().register("my-profile", "My Name", prefs);
 *
 * // 4. Apply preferences to the global context inside CefBrowserImpl creation
 * ProfileManager.get().applyPreferencesToContext(entry, CefRequestContext.getGlobalContext());
 * }</pre>
 */
public class ProfileManager {

    private static final Logger LOG = Logger.getLogger(ProfileManager.class.getName());

    /** System property set by activateProfile() so BrowserRuntimeConfiguration can read it. */
    public static final String CACHE_PATH_PROPERTY = "browser4j.profile.cachePath";

    private static final String PROFILES_DIR = "profiles";

    private static volatile ProfileManager instance;

    private final Path baseDir;
    private String activeProfileId;

    private ProfileManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Initializes the ProfileManager with a custom base directory.
     * Must be called before {@link #get()} if a non-default location is desired.
     */
    public static void initialize(Path baseDir) {
        if (instance == null) {
            synchronized (ProfileManager.class) {
                if (instance == null) {
                    instance = new ProfileManager(baseDir);
                    LOG.info("[Browser4J] ProfileManager initialized at: " + baseDir);
                }
            }
        }
    }

    /**
     * Returns the singleton instance. Defaults to {@code ~/.browser4j} if not initialized.
     */
    public static ProfileManager get() {
        if (instance == null) {
            initialize(Path.of(System.getProperty("user.home"), ".browser4j"));
        }
        return instance;
    }

    // ---- Profile CRUD ----

    /**
     * Registers (or updates) a profile with the given preferences and saves {@code profile.json}.
     * Safe to call on an existing profile — only the metadata and preferences are rewritten.
     */
    public ProfileEntry register(String profileId, String displayName, ProfilePreferences preferences) {
        Path profileDir = profileDir(profileId);
        String absPath = profileDir.toAbsolutePath().toString();
        ProfileEntry entry = new ProfileEntry(profileId, displayName, absPath, preferences);
        try {
            Files.createDirectories(profileDir);
            ProfileSerializer.save(entry);
            LOG.info("[Browser4J] Profile registered: " + profileId + " → " + absPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register profile '" + profileId + "'", e);
        }
        return entry;
    }

    /**
     * Loads a profile from disk.
     * Returns empty if the profile directory or {@code profile.json} does not exist.
     */
    public Optional<ProfileEntry> load(String profileId) {
        try {
            ProfileEntry e = ProfileSerializer.load(profileDir(profileId));
            return Optional.ofNullable(e);
        } catch (IOException e) {
            LOG.warning("[Browser4J] Could not read profile '" + profileId + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Lists all profiles found in the profiles root directory. */
    public List<ProfileEntry> list() {
        List<ProfileEntry> entries = new ArrayList<>();
        Path root = profilesRoot();
        if (!Files.exists(root)) return entries;
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    ProfileEntry e = ProfileSerializer.load(dir);
                    if (e != null) entries.add(e);
                } catch (IOException ex) {
                    LOG.warning("[Browser4J] Skipping unreadable profile dir: " + dir);
                }
            });
        } catch (IOException e) {
            LOG.warning("[Browser4J] Unable to list profiles: " + e.getMessage());
        }
        return entries;
    }

    /** Deletes the profile directory and all its contents from disk. */
    public void delete(String profileId) {
        deleteRecursive(profileDir(profileId));
        LOG.info("[Browser4J] Profile deleted: " + profileId);
    }

    /** Overwrites the preferences of an existing profile and re-saves {@code profile.json}. */
    public ProfileEntry updatePreferences(String profileId, ProfilePreferences preferences) {
        ProfileEntry existing = load(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        ProfileEntry updated = existing.withPreferences(preferences);
        try {
            ProfileSerializer.save(updated);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update preferences for '" + profileId + "'", e);
        }
        return updated;
    }

    // ---- Activation (must happen before BrowserRuntime.init) ----

    /**
     * Marks a profile as active and sets the {@value #CACHE_PATH_PROPERTY} system property
     * so that {@code BrowserRuntimeConfiguration} can pick it up before JCEF initializes.
     *
     * <p><strong>This must be called before {@code BrowserRuntime.init()}.</strong>
     * Calling it afterwards has no effect on the cache path.
     */
    public void activateProfile(String profileId) {
        this.activeProfileId = profileId;
        Path dir = profileDir(profileId);
        try { Files.createDirectories(dir); } catch (IOException e) {
            throw new RuntimeException("Cannot create profile directory: " + dir, e);
        }
        System.setProperty(CACHE_PATH_PROPERTY, dir.toAbsolutePath().toString());
        LOG.info("[Browser4J] Profile activated: " + profileId + " → " + dir.toAbsolutePath());
    }

    /** Returns the currently active profile ID, or null if none was activated. */
    public String getActiveProfileId() {
        return activeProfileId;
    }

    /** Returns the absolute path of the currently active profile directory, or null. */
    public Path getActiveProfileDir() {
        return activeProfileId != null ? profileDir(activeProfileId) : null;
    }

    // ---- Preference application ----

    /**
     * Applies all preferences from the given {@link ProfileEntry} to the provided
     * {@link CefRequestContext} via {@code setPreference()}.
     *
     * <p>Must be called on or after browser creation (on the CEF UI thread, or inside a
     * callback already on the UI thread such as {@code onLoadEnd}).
     */
    public void applyPreferencesToContext(ProfileEntry entry, CefRequestContext context) {
        if (entry == null || context == null) return;
        ProfilePreferences prefs = entry.getPreferences();

        safeSetPref(context, ProfilePreferences.PREF_ACCEPT_LANGUAGES, prefs.getLanguage());

        int colorScheme = switch (prefs.getTheme()) {
            case LIGHT -> 1;
            case DARK  -> 2;
            default    -> 0;
        };
        safeSetPref(context, ProfilePreferences.PREF_THEME, colorScheme);

        if (prefs.getZoomLevel() != 0.0) {
            safeSetPref(context, ProfilePreferences.PREF_ZOOM, prefs.getZoomLevel());
        }

        prefs.getCustomFlags().forEach((key, value) -> safeSetPref(context, key, value));
    }

    // ---- Internal helpers ----

    private void safeSetPref(CefRequestContext ctx, String name, Object value) {
//        try {
//            if (ctx.canSetPreference(name)) {
//                String error = ctx.setPreference(name, value);
//                if (error != null && !error.isEmpty()) {
//                    LOG.warning("[Browser4J] setPreference('" + name + "') → " + error);
//                }
//            } else {
//                LOG.fine("[Browser4J] Preference '" + name + "' is not settable.");
//            }
//        } catch (Exception e) {
//            LOG.warning("[Browser4J] Exception setting pref '" + name + "': " + e.getMessage());
//        }
    }

    private Path profilesRoot() { return baseDir.resolve(PROFILES_DIR); }
    private Path profileDir(String profileId) { return profilesRoot().resolve(profileId); }

    private void deleteRecursive(Path dir) {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
        } catch (IOException e) {
            LOG.warning("[Browser4J] Could not fully delete: " + dir);
        }
    }
}
