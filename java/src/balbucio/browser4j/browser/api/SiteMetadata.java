package balbucio.browser4j.browser.api;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SiteMetadata {
    private final String title;
    private final String url;
    private final String icon;
    private final String description;
    private final String keywords;

    // UI / theme
    private final String themeColor;
    private final String backgroundColor;
    private final String viewport;

    // PWA / mobile behavior
    private final String manifestUrl;
    private final boolean pwaCapable;

    // tradução / idioma
    private final String language;
    private final boolean autoTranslationEnabled;

    // SEO
    private final String robots;
    private final String canonical;

    public SiteMetadata(String title,
                        String url,
                        String icon,
                        String description,
                        String keywords,
                        String themeColor,
                        String backgroundColor,
                        String viewport,
                        String manifestUrl,
                        boolean pwaCapable,
                        String language,
                        boolean autoTranslationEnabled,
                        String robots,
                        String canonical) {
        this.title = title;
        this.url = url;
        this.icon = icon;
        this.description = description;
        this.keywords = keywords;
        this.themeColor = themeColor;
        this.backgroundColor = backgroundColor;
        this.viewport = viewport;
        this.manifestUrl = manifestUrl;
        this.pwaCapable = pwaCapable;
        this.language = language;
        this.autoTranslationEnabled = autoTranslationEnabled;
        this.robots = robots;
        this.canonical = canonical;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public String getKeywords() { return keywords; }
    public String getThemeColor() { return themeColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public String getViewport() { return viewport; }
    public String getManifestUrl() { return manifestUrl; }
    public boolean isPwaCapable() { return pwaCapable; }
    public String getLanguage() { return language; }
    public boolean isAutoTranslationEnabled() { return autoTranslationEnabled; }
    public String getRobots() { return robots; }
    public String getCanonical() { return canonical; }

    public static SiteMetadata fromDocument(Document doc, String pageUrl) {
        if (doc == null) {
            return new SiteMetadata(
                    "",
                    pageUrl == null ? "" : pageUrl,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    "",
                    false,
                    "",
                    "");
        }

        String title = doc.title();
        String icon = null;

        Element iconLink = doc.selectFirst("link[rel~=^(?i)(shortcut icon|icon|apple-touch-icon)$]");
        if (iconLink == null) {
            iconLink = doc.selectFirst("link[rel~=^(?i)(alternate icon)$]");
        }
        if (iconLink == null) {
            iconLink = doc.selectFirst("link[rel~=^(?i)(apple-touch-icon-precomposed)$]");
        }
        if (iconLink != null) {
            icon = iconLink.attr("href");
        }

        String description = null;
        Element desc = doc.selectFirst("meta[name=description], meta[property=og:description]");
        if (desc != null) {
            description = desc.attr("content");
        }

        String keywords = null;
        Element kw = doc.selectFirst("meta[name=keywords]");
        if (kw != null) {
            keywords = kw.attr("content");
        }

        String themeColor = null;
        Element theme = doc.selectFirst("meta[name=theme-color]");
        if (theme != null) {
            themeColor = theme.attr("content");
        }

        String backgroundColor = null;
        Element msTile = doc.selectFirst("meta[name=msapplication-TileColor]");
        if (msTile != null) {
            backgroundColor = msTile.attr("content");
        }
        if (backgroundColor == null) {
            Element themeColorBg = doc.selectFirst("meta[name=theme-color]");
            if (themeColorBg != null) {
                backgroundColor = themeColorBg.attr("content");
            }
        }

        String viewport = null;
        Element vp = doc.selectFirst("meta[name=viewport]");
        if (vp != null) {
            viewport = vp.attr("content");
        }

        String manifestUrl = null;
        Element manifest = doc.selectFirst("link[rel=manifest]");
        if (manifest != null) {
            manifestUrl = manifest.attr("href");
        }

        boolean pwaCapable = false;
        Element pwa1 = doc.selectFirst("meta[name=mobile-web-app-capable]");
        if (pwa1 != null && "yes".equalsIgnoreCase(pwa1.attr("content"))) {
            pwaCapable = true;
        }
        Element pwa2 = doc.selectFirst("meta[name=apple-mobile-web-app-capable]");
        if (!pwaCapable && pwa2 != null && "yes".equalsIgnoreCase(pwa2.attr("content"))) {
            pwaCapable = true;
        }

        String language = null;
        Element html = doc.selectFirst("html");
        if (html != null) {
            language = html.attr("lang");
        }
        if (language == null || language.isBlank()) {
            Element langMeta = doc.selectFirst("meta[http-equiv=content-language]");
            if (langMeta != null) {
                language = langMeta.attr("content");
            }
        }

        boolean autoTranslationEnabled = false;
        Element translateMeta = doc.selectFirst("meta[name=google-translate-customization]");
        if (translateMeta != null) {
            autoTranslationEnabled = true;
        }
        if (!autoTranslationEnabled) {
            Element translateFlag = doc.selectFirst("meta[name=google]");
            if (translateFlag != null) {
                String value = translateFlag.attr("content");
                if (value != null && !value.isBlank()) {
                    autoTranslationEnabled = true;
                }
            }
        }

        String robots = null;
        Element robotsMeta = doc.selectFirst("meta[name=robots]");
        if (robotsMeta != null) {
            robots = robotsMeta.attr("content");
        }

        String canonical = null;
        Element canonicalLink = doc.selectFirst("link[rel=canonical]");
        if (canonicalLink != null) {
            canonical = canonicalLink.attr("href");
        }

        return new SiteMetadata(
                title == null ? "" : title,
                pageUrl,
                icon == null ? "" : icon,
                description == null ? "" : description,
                keywords == null ? "" : keywords,
                themeColor == null ? "" : themeColor,
                backgroundColor == null ? "" : backgroundColor,
                viewport == null ? "" : viewport,
                manifestUrl == null ? "" : manifestUrl,
                pwaCapable,
                language == null ? "" : language,
                autoTranslationEnabled,
                robots == null ? "" : robots,
                canonical == null ? "" : canonical);
    }
}
