package balbucio.browser4j.download.handler;

import balbucio.browser4j.download.api.DownloadManager;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;

/**
 * JCEF download handler that bridges Chromium download events to {@link DownloadManager}.
 *
 * <p>Replaces the old {@code DownloadBlockerHandler} in {@code CefBrowserImpl}.
 * The profile ID is bound at construction time so downloads are always attributed
 * to the correct profile.
 */
public class DownloadHandlerImpl extends CefDownloadHandlerAdapter {

    private final balbucio.browser4j.download.api.DownloadManagerImpl manager;
    private final String profileId;

    /**
     * @param manager   the shared DownloadManager for the current BrowserRuntime
     * @param profileId the profile this browser belongs to ("global" if none)
     */
    public DownloadHandlerImpl(balbucio.browser4j.download.api.DownloadManagerImpl manager, String profileId) {
        this.manager   = manager;
        this.profileId = profileId;
    }

    @Override
    public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem item,
                                    String suggestedName, CefBeforeDownloadCallback callback) {
        manager.handleBeforeDownload(
                item.getURL(),
                suggestedName,
                item.getMimeType(),
                item.getTotalBytes(),
                profileId,
                browser.getIdentifier(),
                item.getId(),
                callback
        );
        return true; // always handle asynchronously
    }

    @Override
    public void onDownloadUpdated(CefBrowser browser, CefDownloadItem item,
                                  CefDownloadItemCallback callback) {
        manager.handleDownloadUpdated(
                browser.getIdentifier(),
                item.getId(),
                item.getReceivedBytes(),
                item.getTotalBytes(),
                item.isComplete(),
                item.isCanceled(),
                item.isInProgress(),
                !item.isValid(), // isInterrupted approximation
                callback
        );
    }
}
