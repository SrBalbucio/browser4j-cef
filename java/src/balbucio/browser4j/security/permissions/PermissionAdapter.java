package balbucio.browser4j.security.permissions;

import balbucio.browser4j.browser.api.Browser;

/**
 * Interface that allows the host application to decide on permission requests.
 */
public interface PermissionAdapter {
    /**
     * Called when a website requests a permission and there is no saved decision.
     * @param browser the browser requesting the permission
     * @param origin the origin URL (e.g. https://google.com)
     * @param type the permission type
     * @return the status to apply
     */
    PermissionStatus onRequest(Browser browser, String origin, PermissionType type);
}
