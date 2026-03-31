package balbucio.browser4j.browser.input;

import org.cef.browser.CefBrowser;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class InputController {
    private final CefBrowser browser;

    public InputController(CefBrowser browser) {
        this.browser = browser;
    }

    public void sendSyntheticMouseClick(int x, int y, int buttonId, boolean isClickUp) {
        int id = isClickUp ? MouseEvent.MOUSE_RELEASED : MouseEvent.MOUSE_PRESSED;
        // Depending on JCEF capabilities or component wrapping, we simulate raw java.awt events.
        MouseEvent event = new MouseEvent(browser.getUIComponent(), id, System.currentTimeMillis(), 0, x, y, 1, false, buttonId);
        browser.getUIComponent().dispatchEvent(event);
    }

    public void sendSyntheticMouseMove(int x, int y) {
        MouseEvent event = new MouseEvent(browser.getUIComponent(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false, MouseEvent.NOBUTTON);
        browser.getUIComponent().dispatchEvent(event);
    }

    public void sendSyntheticKeyPress(int keyCode, char keyChar) {
        KeyEvent event = new KeyEvent(browser.getUIComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, keyChar);
        browser.getUIComponent().dispatchEvent(event);
    }
}
