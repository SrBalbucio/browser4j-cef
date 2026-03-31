package balbucio.browser4j.browser.event;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefPaintEvent;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class CefOnPaintHandlerAdapter implements Consumer<CefPaintEvent> {
    @Override
    public void accept(CefPaintEvent cefPaintEvent) {
        onPaint(cefPaintEvent.getBrowser(), cefPaintEvent.getPopup(), cefPaintEvent.getDirtyRects(), cefPaintEvent.getRenderedFrame(), cefPaintEvent.getWidth(), cefPaintEvent.getHeight());
    }

    public abstract void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height);
}
