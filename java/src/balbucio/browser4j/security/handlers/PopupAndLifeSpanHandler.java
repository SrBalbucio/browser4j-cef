package balbucio.browser4j.security.handlers;

import balbucio.browser4j.security.api.SecurityModuleImpl;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

public class PopupAndLifeSpanHandler extends CefLifeSpanHandlerAdapter {
    private final SecurityModuleImpl securityModule;

    public PopupAndLifeSpanHandler(SecurityModuleImpl securityModule) {
        this.securityModule = securityModule;
    }

    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
        if (securityModule.isPopupBlocked(target_url)) {
            return true; // Blocked by policy
        }
        return false;
    }
}
