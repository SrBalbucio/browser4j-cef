package balbucio.browser4j.ui.swing;

import balbucio.browser4j.ui.abstraction.BrowserView;
import org.cef.browser.CefBrowser;
import java.awt.Component;

public class SwingBrowserView implements BrowserView {
    private final CefBrowser cefBrowser;

    public SwingBrowserView(CefBrowser cefBrowser) {
        this.cefBrowser = cefBrowser;
    }

    @Override
    public Component getUIComponent() {
        return cefBrowser.getUIComponent();
    }
}
