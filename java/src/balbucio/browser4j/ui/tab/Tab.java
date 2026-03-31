package balbucio.browser4j.ui.tab;

import balbucio.browser4j.browser.api.Browser;
import balbucio.browser4j.browser.api.CefBrowserImpl;
import balbucio.browser4j.browser.api.SiteMetadata;
import balbucio.browser4j.browser.events.BrowserEventListener;
import balbucio.browser4j.ui.abstraction.BrowserView;

import java.awt.Component;

public class Tab {
    private final String id;
    private final Browser browser;
    private final TabState state;

    public Tab(String id, Browser browser) {
        this.id = id;
        this.browser = browser;
        this.state = new TabState();

        this.browser.addEventListener(new BrowserEventListener() {
            @Override
            public void onLoadStart(String url) {
                state.setLoading(true);
                state.setUrl(url);
                state.setDrmProtected(false);
            }

            @Override
            public void onLoadEnd(String url, int httpStatusCode) {
                state.setLoading(false);
                state.setUrl(url);

                browser.getSiteMetadata().thenAccept(siteMetadata -> {
                    if (siteMetadata != null) {
                        if (siteMetadata.getTitle() != null && !siteMetadata.getTitle().isBlank()) {
                            state.setTitle(siteMetadata.getTitle());
                        }
                        state.setIcon(siteMetadata.getIcon());
                        state.setDescription(siteMetadata.getDescription());
                        state.setKeywords(siteMetadata.getKeywords());
                        state.setThemeColor(siteMetadata.getThemeColor());
                        state.setBackgroundColor(siteMetadata.getBackgroundColor());
                        state.setViewport(siteMetadata.getViewport());
                        state.setManifestUrl(siteMetadata.getManifestUrl());
                        state.setPwaCapable(siteMetadata.isPwaCapable());
                        state.setLanguage(siteMetadata.getLanguage());
                        state.setAutoTranslationEnabled(siteMetadata.isAutoTranslationEnabled());
                        state.setRobots(siteMetadata.getRobots());
                        state.setCanonical(siteMetadata.getCanonical());
                    }
                }).exceptionally(err -> {
                    // ignore metadata fetch failures; PAGE still loads
                    return null;
                });
            }

            @Override
            public void onLoadError(String url, int errorCode, String errorText) {
                state.setLoading(false);
            }

            @Override
            public void onNavigation(String url) {
                state.setUrl(url);
            }

            @Override
            public void onTitleChange(String title) {
                state.setTitle(title);
            }

            @Override
            public void onDRMDetected(String url) {
                state.setDrmProtected(true);
            }
        });
    }

    public String getId() {
        return id;
    }

    public Browser getBrowser() {
        return browser;
    }

    public TabState getState() {
        return state;
    }

    public Component getUIComponent() {
        if (browser instanceof CefBrowserImpl) {
            BrowserView view = ((CefBrowserImpl) browser).getView();
            if (view != null) {
                return view.getUIComponent();
            }
        }
        return null;
    }

    public void close() {
        browser.close();
    }
}
