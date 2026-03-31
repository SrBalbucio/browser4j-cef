package balbucio.browser4j.browser.profile;

import java.nio.file.Path;

/**
 * Represents a single registered browser profile entry.
 * The profilePath points to the on-disk directory used as the
 * CefRequestContext cache_path, giving the profile full Chromium persistence
 * (cookies, localStorage, preferences via LocalPrefs.json, etc.).
 */
public class ProfileEntry {
    private final String            profileId;
    private final String            displayName;
    private final String            profilePath;   // stored as String for Gson compatibility
    private final ProfilePreferences preferences;

    public ProfileEntry(String profileId, String displayName, String profilePath, ProfilePreferences preferences) {
        this.profileId   = profileId;
        this.displayName = displayName;
        this.profilePath = profilePath;
        this.preferences = preferences;
    }

    public String             getProfileId()   { return profileId;   }
    public String             getDisplayName() { return displayName; }
    public String             getProfilePath() { return profilePath; }
    public Path               getProfileDir()  { return Path.of(profilePath); }
    public ProfilePreferences getPreferences() { return preferences; }

    /** Returns a new ProfileEntry with updated preferences (the original is immutable). */
    public ProfileEntry withPreferences(ProfilePreferences preferences) {
        return new ProfileEntry(profileId, displayName, profilePath, preferences);
    }
}
