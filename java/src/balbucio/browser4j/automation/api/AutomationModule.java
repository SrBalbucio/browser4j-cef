package balbucio.browser4j.automation.api;

import java.util.concurrent.CompletableFuture;

/**
 * High-level API for browser automation tasks using CSS selectors.
 */
public interface AutomationModule {
    /**
     * Clicks the first element matching the selector.
     * Performs a scroll-to-view followed by a native mouse click event.
     */
    CompletableFuture<Void> click(String selector);

    /**
     * Types text into the element matching the selector.
     * Focuses the element and simulates native key events.
     */
    CompletableFuture<Void> type(String selector, String text);

    /**
     * Returns the visible text of the element.
     */
    CompletableFuture<String> getText(String selector);

    /**
     * Returns an attribute value of the element.
     */
    CompletableFuture<String> getAttribute(String selector, String attribute);

    /**
     * Checks if at least one element matching the selector exists and is visible.
     */
    CompletableFuture<Boolean> isVisible(String selector);

    /**
     * Waits until the selector appears in the DOM and is visible.
     * @param timeoutMs Maximum wait time in milliseconds.
     */
    CompletableFuture<Void> waitForSelector(String selector, long timeoutMs);

    /**
     * Scrolls the element into the center of the viewport.
     */
    CompletableFuture<Void> scrollTo(String selector);
}
