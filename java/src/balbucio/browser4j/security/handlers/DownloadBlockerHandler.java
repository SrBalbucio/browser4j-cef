package balbucio.browser4j.security.handlers;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;

public class DownloadBlockerHandler extends CefDownloadHandlerAdapter {
    @Override
    public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
        super.onDownloadUpdated(browser, downloadItem, callback);
    }

    @Override
    public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
        // Block all downloads by default for MVP Phase 3
        callback.Continue(suggestedName, false);
        return super.onBeforeDownload(browser, downloadItem, suggestedName, callback);
    }
}
