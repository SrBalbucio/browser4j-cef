package balbucio.browser4j.browser.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable POJO holding all user-facing browser preferences for a persistent profile.
 * Theme, language, timezone and zoom are mapped to native Chromium preference keys
 * via ProfileManager.createContextFor().
 *
 * Custom low-level Chromium preferences can be added via customFlags for advanced use.
 */
public class ProfilePreferences {

    // Chromium preference key constants
    public static final String PREF_ACCEPT_LANGUAGES  = "intl.accept_languages";
    public static final String PREF_THEME             = "browser.theme.color_scheme";
    public static final String PREF_ZOOM              = "partition.per_host_zoom_levels.x-default";

    /** Supported theme values matching Chromium color_scheme preference. */
    public enum Theme {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system");

        private final String value;
        Theme(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private final Theme theme;
    private final String language;
    private final String timezone;
    private final double zoomLevel;
    private final Map<String, Object> customFlags;

    private ProfilePreferences(Builder builder) {
        this.theme       = builder.theme;
        this.language    = builder.language;
        this.timezone    = builder.timezone;
        this.zoomLevel   = builder.zoomLevel;
        this.customFlags = Collections.unmodifiableMap(new HashMap<>(builder.customFlags));
    }

    public Theme   getTheme()       { return theme;       }
    public String  getLanguage()    { return language;    }
    public String  getTimezone()    { return timezone;    }
    public double  getZoomLevel()   { return zoomLevel;   }
    public Map<String, Object> getCustomFlags() { return customFlags; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Theme               theme       = Theme.SYSTEM;
        private String              language    = "en-US";
        private String              timezone    = "America/New_York";
        private double              zoomLevel   = 0.0;
        private Map<String, Object> customFlags = new HashMap<>();

        public Builder theme(Theme theme)          { this.theme = theme;       return this; }
        public Builder language(String language)   { this.language = language; return this; }
        public Builder timezone(String timezone)   { this.timezone = timezone; return this; }
        public Builder zoomLevel(double zoom)      { this.zoomLevel = zoom;    return this; }
        public Builder flag(String key, Object value) {
            this.customFlags.put(key, value);
            return this;
        }

        public ProfilePreferences build() { return new ProfilePreferences(this); }
    }
}
