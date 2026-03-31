package balbucio.browser4j.automation.internal;

/**
 * JS snippets for browser automation.
 */
public class AutomationScripts {
    /**
     * Finds an element, scrolls it into view, and returns its center coordinates in the viewport.
     * Returns: { x: number, y: number, width: number, height: number } or null.
     */
    public static final String GET_ELEMENT_COORDINATES = """
        (function(selector) {
            const el = document.querySelector(selector);
            if (!el) return null;
            el.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'center' });
            const rect = el.getBoundingClientRect();
            return {
                x: rect.left + rect.width / 2,
                y: rect.top + rect.height / 2,
                left: rect.left,
                top: rect.top,
                width: rect.width,
                height: rect.height
            };
        })('%s')
        """;

    /**
     * Waits for an element to match the selector and be visible.
     * Resolves when found.
     */
    public static final String WAIT_FOR_SELECTOR = """
        (async function(selector, timeoutMs, callbackId) {
            const startTime = Date.now();
            
            function check() {
                const el = document.querySelector(selector);
                if (el && el.offsetParent !== null) {
                    window.bridge({ 
                        request: JSON.stringify({ 
                            event: '__automation_wait_success', 
                            data: { id: callbackId, selector: selector } 
                        }), 
                        onSuccess: () => {}, 
                        onFailure: () => {} 
                    });
                    return true;
                }
                if (Date.now() - startTime > timeoutMs) {
                    window.bridge({ 
                        request: JSON.stringify({ 
                            event: '__automation_wait_error', 
                            data: { id: callbackId, error: 'Timeout waiting for ' + selector } 
                        }), 
                        onSuccess: () => {}, 
                        onFailure: () => {} 
                    });
                    return true;
                }
                return false;
            }

            if (check()) return;
            const observer = new MutationObserver(() => {
                if (check()) observer.disconnect();
            });
            observer.observe(document.body, { childList: true, subtree: true, attributes: true });
            
            // Fallback interval for dynamic styles
            const interval = setInterval(() => {
                if (check()) {
                    observer.disconnect();
                    clearInterval(interval);
                }
            }, 500);
        })('%s', %d, '%s')
        """;

    /** Simple visibility check */
    public static final String IS_VISIBLE = """
        (function(selector) {
            const el = document.querySelector(selector);
            return !!(el && el.offsetParent !== null);
        })('%s')
        """;
}
