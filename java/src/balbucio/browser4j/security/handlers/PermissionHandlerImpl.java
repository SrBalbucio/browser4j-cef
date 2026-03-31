package balbucio.browser4j.security.handlers;

import balbucio.browser4j.browser.api.Browser;
import balbucio.browser4j.security.permissions.*;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefMediaAccessCallback;
import org.cef.callback.CefPermissionPromptCallback;
import org.cef.handler.CefPermissionHandler;

/**
 * JCEF handler for browser permissions.
 */
public class PermissionHandlerImpl implements CefPermissionHandler {
    private final Browser browser;
    private final PermissionModule module;

    public PermissionHandlerImpl(Browser browser, PermissionModule module) {
        this.browser = browser;
        this.module = module;
    }

    @Override
    public boolean onRequestMediaAccessPermission(CefBrowser browser, CefFrame frame, String requesting_origin, int requested_permissions, CefMediaAccessCallback callback) {
        boolean audio = (requested_permissions & 0x1) != 0;
        boolean video = (requested_permissions & 0x2) != 0;

        PermissionStatus status = PermissionStatus.GRANTED;

        if (audio) {
            status = handle(requesting_origin, PermissionType.MEDIASTREAM_MIC, status);
        }
        if (video) {
            status = handle(requesting_origin, PermissionType.MEDIASTREAM_CAMERA, status);
        }

        if (status == PermissionStatus.GRANTED) {
            callback.Continue(requested_permissions);
            return true;
        } else if (status == PermissionStatus.DENIED) {
            callback.Cancel();
            return true;
        }
        return false;
    }

    @Override
    public boolean onShowPermissionPrompt(CefBrowser browser, int prompt_id, String requesting_origin, int requested_permissions, CefPermissionPromptCallback callback) {
        PermissionType type = mapPermissions(requested_permissions);
        if (type == null) return false;

        PermissionStatus status = handle(requesting_origin, type, PermissionStatus.ASK);

        if (status == PermissionStatus.GRANTED) {
            callback.Continue(requested_permissions);
            return true;
        } else if (status == PermissionStatus.DENIED) {
            callback.Cancel();
            return true;
        }
        return false;
    }

    @Override
    public void onDismissPermissionPrompt(CefBrowser browser, int prompt_id, int result) {
        // Not implemented
    }

    private PermissionStatus handle(String origin, PermissionType type, PermissionStatus defaultStatus) {
        PermissionStatus saved = module.getPermission(origin, type);
        if (saved != PermissionStatus.ASK) {
            return saved;
        }

        PermissionAdapter adapter = module.getAdapter();
        if (adapter != null) {
            PermissionStatus response = adapter.onRequest(browser, origin, type);
            if (response != PermissionStatus.ASK) {
                module.setPermission(origin, type, response);
                return response;
            }
        }
        return defaultStatus;
    }

    private PermissionType mapPermissions(int p) {
        if ((p & (1 << 4)) != 0) return PermissionType.GEOLOCATION;
        if ((p & (1 << 3)) != 0) return PermissionType.NOTIFICATIONS;
        if ((p & (1 << 2)) != 0) return PermissionType.MIDI_SYSEX;
        if ((p & (1 << 6)) != 0) return PermissionType.CLIPBOARD_READ_WRITE;
        return null;
    }
}
