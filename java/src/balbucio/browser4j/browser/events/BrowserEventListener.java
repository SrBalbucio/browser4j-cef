package balbucio.browser4j.browser.events;

import balbucio.browser4j.browser.error.BrowserError;

public interface BrowserEventListener {
    void onLoadStart(String url);
    void onLoadEnd(String url, int httpStatusCode);
    void onLoadError(String url, int errorCode, String errorText);
    void onNavigation(String url);
    void onTitleChange(String title);
    
    default void onDRMDetected(String url) {}

    /**
     * Fired after Browser4j classifies and renders an error page.
     * Override to react to errors programmatically without replacing the error page.
     */
    default void onBrowserError(BrowserError error) {}
}
