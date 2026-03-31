package balbucio.browser4j.ui.tab;

public class TabState {
    private String title = "";
    private String url = "";
    private boolean loading = false;
    private long lastAccessedAt = System.currentTimeMillis();
    private boolean incognito = false;
    private boolean drmProtected = false;

    private String icon = "";
    private String description = "";
    private String keywords = "";
    private String themeColor = "";
    private String backgroundColor = "";
    private String viewport = "";
    private String manifestUrl = "";
    private boolean pwaCapable = false;
    private String language = "";
    private boolean autoTranslationEnabled = false;
    private String robots = "";
    private String canonical = "";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public String getViewport() { return viewport; }
    public void setViewport(String viewport) { this.viewport = viewport; }

    public String getManifestUrl() { return manifestUrl; }
    public void setManifestUrl(String manifestUrl) { this.manifestUrl = manifestUrl; }

    public boolean isPwaCapable() { return pwaCapable; }
    public void setPwaCapable(boolean pwaCapable) { this.pwaCapable = pwaCapable; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isAutoTranslationEnabled() { return autoTranslationEnabled; }
    public void setAutoTranslationEnabled(boolean autoTranslationEnabled) { this.autoTranslationEnabled = autoTranslationEnabled; }

    public String getRobots() { return robots; }
    public void setRobots(String robots) { this.robots = robots; }

    public String getCanonical() { return canonical; }
    public void setCanonical(String canonical) { this.canonical = canonical; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isLoading() { return loading; }
    public void setLoading(boolean loading) { this.loading = loading; }

    public long getLastAccessedAt() { return lastAccessedAt; }
    public void markAccessed() { this.lastAccessedAt = System.currentTimeMillis(); }
    
    public boolean isIncognito() { return incognito; }
    public void setIncognito(boolean incognito) { this.incognito = incognito; }
    
    public boolean isDrmProtected() { return drmProtected; }
    public void setDrmProtected(boolean drmProtected) { this.drmProtected = drmProtected; }
}
