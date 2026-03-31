package balbucio.browser4j.download.handler;

import balbucio.browser4j.download.api.DownloadManager;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param manager   the shared DownloadManager for this browser instance
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
                item.getId(),
                callback
        );
        return true; // always handle asynchronously
    }

    @Override
    public void onDownloadUpdated(CefBrowser browser, CefDownloadItem item,
                                  CefDownloadItemCallback callback) {
        manager.handleDownloadUpdated(
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
