package balbucio.browser4j.browser.api;

import balbucio.browser4j.browser.event.CefOnPaintHandlerAdapter;
import balbucio.browser4j.browser.events.BrowserEventListener;
import balbucio.browser4j.browser.events.DomMutationEvent;
import balbucio.browser4j.browser.events.DomMutationListener;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefCookieManager;
import org.cef.network.CefRequest;
import balbucio.browser4j.bridge.messaging.JSBridge;
import balbucio.browser4j.network.interception.NetworkHandlerImpl;
import balbucio.browser4j.ui.abstraction.BrowserView;
import balbucio.browser4j.ui.swing.SwingBrowserView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.Rectangle;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

import org.cef.CefSettings;

import balbucio.browser4j.browser.events.FrameCaptureListener;
import balbucio.browser4j.browser.input.InputController;
import balbucio.browser4j.core.runtime.BrowserRuntime;
import balbucio.browser4j.security.api.SecurityModuleImpl;
import balbucio.browser4j.security.handlers.PopupAndLifeSpanHandler;
import balbucio.browser4j.cache.api.CacheManager;
import balbucio.browser4j.cache.api.CacheManagerImpl;
import balbucio.browser4j.cache.config.CacheConfig;
import balbucio.browser4j.cache.interception.CacheInterceptor;
import balbucio.browser4j.download.api.DownloadManager;
import balbucio.browser4j.download.api.DownloadManagerImpl;
import balbucio.browser4j.download.config.DownloadConfig;
import balbucio.browser4j.download.handler.DownloadHandlerImpl;
import balbucio.browser4j.observability.MetricsTracker;
import balbucio.browser4j.network.api.NetworkModule;
import balbucio.browser4j.security.api.SecurityModule;
import balbucio.browser4j.devtools.DevToolsModule;
import balbucio.browser4j.streaming.Frame;
import org.cef.browser.CefRequestContext;
import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.api.HistoryManagerImpl;
import balbucio.browser4j.history.service.AutocompleteService;
import balbucio.browser4j.network.cookies.CookieManager;
import balbucio.browser4j.security.profile.FingerprintInjector;
import balbucio.browser4j.security.drm.DRMInjector;
import balbucio.browser4j.browser.profile.ProfileManager;
import balbucio.browser4j.browser.profile.ProfileEntry;
import balbucio.browser4j.automation.api.AutomationModule;
import balbucio.browser4j.automation.api.AutomationModuleImpl;
import balbucio.browser4j.security.permissions.PermissionModule;
import balbucio.browser4j.security.permissions.PermissionModuleImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import balbucio.browser4j.storage.api.StorageModuleImpl;
import balbucio.browser4j.browser.error.BrowserError;
import balbucio.browser4j.browser.error.BrowserErrorType;
import balbucio.browser4j.browser.error.ErrorPageRegistry;
import balbucio.browser4j.browser.error.ErrorPageRenderer;
import balbucio.browser4j.browser.media.MediaModule;
import balbucio.browser4j.browser.media.MediaModuleImpl;

public class CefBrowserImpl implements Browser {
    private final CefBrowser cefBrowser;
    private final CefClient cefClient;
    private final List<BrowserEventListener> listeners;
    private final List<FrameCaptureListener> frameListeners;
    private final List<DomMutationListener> domMutationListeners;
    private final JSBridge jsBridge;
    private final NetworkHandlerImpl networkHandler;
    private final InputController inputController;
    private final MetricsTracker metricsTracker;
    private final SecurityModuleImpl securityModule;
    private final CookieManager cookieManager;
    private final BrowserOptions options;
    private final StorageModuleImpl storageModule;
    private final ErrorPageRegistry errorPageRegistry;
    private final ErrorPageRenderer errorPageRenderer;
    private final DownloadManager downloadManager;
    private final MediaModule mediaModule;
    private final CacheManager cacheManager;
    private final HistoryManager historyManager;
    private final AutocompleteService autocompleteService;
    private final AutomationModuleImpl automationModule;
    private final PermissionModule permissionModule;
    private Consumer<String> consoleMessageHandler;

    private final DevToolsModule devToolsModule = new DevToolsModule() {
        @Override
        public void open() {
            SwingUtilities.invokeLater(() -> {
                if (cefBrowser != null) {
                    cefBrowser.openDevTools();
                }
            });
        }

        @Override
        public void close() {
            cefBrowser.closeDevTools();
        }
    };

    public CefBrowserImpl(CefApp cefApp) {
        this(cefApp, null);
    }

    public CefBrowserImpl(CefApp cefApp, BrowserOptions options) {
        this.options = options;
        this.listeners = new ArrayList<>();
        this.frameListeners = new ArrayList<>();
        this.domMutationListeners = new ArrayList<>();

        this.cefClient = cefApp.createClient();

        setupHandlers(this.cefClient);

        this.jsBridge = new JSBridge(this.cefClient, this);
        this.metricsTracker = new MetricsTracker();
        this.securityModule = new SecurityModuleImpl();
        this.networkHandler = new NetworkHandlerImpl(this.cefClient, this.metricsTracker, this.securityModule);
        this.storageModule = new StorageModuleImpl(this.jsBridge);
        this.errorPageRegistry = new ErrorPageRegistry();
        this.errorPageRenderer = new ErrorPageRenderer(this.errorPageRegistry);

        // Download manager — uses active profile dir if available
        String profileId = "global";
        Path downloadRoot = DownloadConfig.builder().build().getDefaultDownloadDir();
        if (options != null && options.getSession() != null
                && options.getSession().getProfile() != null) {
            ProfileEntry pe = options.getSession().getProfile().getProfileEntry();
            if (pe != null && pe.getProfilePath() != null) {
                profileId = pe.getProfileId();
                downloadRoot = Path.of(pe.getProfilePath()).resolve("downloads");
            }
        }
        this.downloadManager = new DownloadManagerImpl(DownloadConfig.builder().build(), downloadRoot);
        this.mediaModule = new MediaModuleImpl(this, this.jsBridge, this.downloadManager);

        // History Setup - Shared with profile dir if possible
        Path historyPath = Path.of(System.getProperty("user.home"), ".browser4j");
        if (options != null && options.getSession() != null && options.getSession().getProfile() != null) {
            ProfileEntry pe = options.getSession().getProfile().getProfileEntry();
            if (pe != null && pe.getProfilePath() != null) {
                historyPath = Path.of(pe.getProfilePath());
            }
        }
        this.historyManager = new HistoryManagerImpl(historyPath);
        this.autocompleteService = new AutocompleteService(this.historyManager);

        // Initialize Cache
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(true)
                .maxCacheSizeBytes(1024L * 1024 * 1024)
                .build();
        Path cachePath = historyPath.resolve("cache");
        this.cacheManager = new CacheManagerImpl(cacheConfig, cachePath);
        this.networkHandler.setCacheInterceptor(new CacheInterceptor(cacheManager));

        this.permissionModule = new PermissionModuleImpl(historyPath);

        this.cefClient.addLifeSpanHandler(new PopupAndLifeSpanHandler(this.securityModule));
        this.cefClient.addDownloadHandler(new DownloadHandlerImpl((DownloadManagerImpl) this.downloadManager, profileId));

        CefRequestContext context = CefRequestContext.getGlobalContext();
        CefCookieManager globalCookieManager = CefCookieManager.getGlobalManager();

        if (options != null && options.getSession() != null) {
            Session session = options.getSession();
            if (session.isIncognito()) {
                if (session.getNativeContext() == null) {
                    // Create isolated in-memory context (empty handler prevents caching)
                    session.setNativeContext(CefRequestContext.createContext(new org.cef.handler.CefRequestContextHandlerAdapter() {
                    }));
                }
                context = (CefRequestContext) session.getNativeContext();
            }
        }

        this.cookieManager = new CookieManager(globalCookieManager);

        boolean osrEnabled = BrowserRuntime.getConfig().isOsrEnabled();
        this.cefBrowser = cefClient.createBrowser("about:blank", osrEnabled, false, context);
        this.inputController = new InputController(this.cefBrowser);
        this.automationModule = new AutomationModuleImpl(this.jsBridge, this.inputController);

        this.jsBridge.addHandler((event, data) -> {
            if ("__drm_detected".equals(event)) {
                if (listeners != null) {
                    for (BrowserEventListener listener : listeners) {
                        listener.onDRMDetected(this.cefBrowser.getURL());
                    }
                }
            } else if ("spa_navigation".equals(event)) {
                recordHistory(this.cefBrowser.getURL());
            } else if ("dom_mutation".equals(event)) {
                if (data instanceof Map) {
                    Object mutations = ((Map<?, ?>) data).get("mutations");
                    if (mutations instanceof List) {
                        for (Object item : (List<?>) mutations) {
                            if (item instanceof Map) {
                                DomMutationEvent mutationEvent = DomMutationEvent.fromMap((Map<?, ?>) item);
                                for (DomMutationListener listener : domMutationListeners) {
                                    listener.onDomMutation(mutationEvent);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public static Browser create(CefApp cefApp) {
        return new CefBrowserImpl(cefApp);
    }

    public static Browser create(CefApp cefApp, BrowserOptions options) {
        return new CefBrowserImpl(cefApp, options);
    }

    private void setupHandlers(CefClient client) {
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, org.cef.browser.CefFrame frame, CefRequest.TransitionType transitionType) {
                if (frame.isMain()) {
                    if (options != null && options.getSession() != null && options.getSession().getProfile() != null && options.getSession().getProfile().getFingerprint() != null) {
                        FingerprintInjector.inject(browser, options.getSession().getProfile().getFingerprint());
                    }

                    // Inject DRM hooks
                    browser.executeJavaScript(DRMInjector.INJECTION_SCRIPT, browser.getURL(), 0);

                    // Inject DOM mutation observer hooks
                    browser.executeJavaScript(DOMMutationObserverInjector.INJECTION_SCRIPT, browser.getURL(), 0);

                    // Inject SPA tracking hooks
                    browser.executeJavaScript("""
                            (function() {
                                const pushState = history.pushState;
                                history.pushState = function() {
                                    pushState.apply(history, arguments);
                                    window.cefQuery({ 
                                        request: JSON.stringify({ event: 'spa_navigation', data: window.location.href }),
                                        onSuccess: function(response) {},
                                        onFailure: function(error_code, error_message) {}
                                    });
                                };
                                window.addEventListener('popstate', function() {
                                    window.cefQuery({ 
                                        request: JSON.stringify({ event: 'spa_navigation', data: window.location.href }),
                                        onSuccess: function(response) {},
                                        onFailure: function(error_code, error_message) {}
                                    });
                                });
                            })();
                            """, browser.getURL(), 0);

                    // Inject JS Bridge modular proxies
                    String bridgeScript = jsBridge.getInjectionScript();
                    if (!bridgeScript.isEmpty()) {
                        browser.executeJavaScript(bridgeScript, browser.getURL(), 0);
                    }

                    for (BrowserEventListener listener : listeners) {
                        listener.onLoadStart(frame.getURL());
                    }
                }
            }

            @Override
            public void onLoadEnd(CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    // Apply persistent profile preferences on each page load
                    if (options != null && options.getSession() != null
                            && options.getSession().getProfile() != null) {
                        ProfileEntry profileEntry = options.getSession().getProfile().getProfileEntry();
                        if (profileEntry != null) {
                            ProfileManager.get().applyPreferencesToContext(profileEntry,
                                    browser.getRequestContext());
                        }
                    }

                    // Intercept selected HTTP error status codes
                    BrowserErrorType httpType = BrowserErrorType.fromHttpStatus(httpStatusCode);
                    String currentUrl = frame.getURL();
                    if (httpType != null && currentUrl != null
                            && !currentUrl.startsWith("data:") && !currentUrl.startsWith("file:")) {
                        BrowserError err = BrowserError.builder()
                                .url(currentUrl)
                                .httpStatusCode(httpStatusCode)
                                .errorText("HTTP " + httpStatusCode)
                                .type(httpType)
                                .build();
                        CefBrowserImpl.this.notifyAndShowError(err);
                        return; // don't propagate onLoadEnd to listeners for intercepted errors
                    }

                    for (BrowserEventListener listener : listeners) {
                        listener.onLoadEnd(frame.getURL(), httpStatusCode);
                    }
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, org.cef.browser.CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                if (frame.isMain()) {
                    // Skip internal navigation errors (e.g. cancellation during redirect)
                    if (errorCode.getCode() == -3) return; // ERR_ABORTED

                    BrowserErrorType type = BrowserErrorType.fromCefCode(errorCode.getCode());
                    BrowserError err = BrowserError.builder()
                            .url(failedUrl)
                            .cefErrorCode(errorCode.getCode())
                            .errorText(errorText)
                            .type(type)
                            .build();

                    CefBrowserImpl.this.notifyAndShowError(err);

                    // Also fire raw onLoadError for backwards compatibility
                    for (BrowserEventListener listener : listeners) {
                        listener.onLoadError(failedUrl, errorCode.getCode(), errorText);
                    }
                }
            }
        });

        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, org.cef.browser.CefFrame frame, String url) {
                if (frame.isMain()) {
                    recordHistory(url);
                    for (BrowserEventListener listener : listeners) {
                        listener.onNavigation(url);
                    }
                }
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                recordTitle(title);
                for (BrowserEventListener listener : listeners) {
                    listener.onTitleChange(title);
                }
            }

            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (consoleMessageHandler != null) {
                    consoleMessageHandler.accept(String.format("[%s] %s:%d %s", level.name(), source, line, message));
                }
                return false;
            }
        });

        client.addOnPaintListener(new CefOnPaintHandlerAdapter() {
            @Override
            public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
                metricsTracker.markFrame();
                Frame frame = new Frame(buffer, width, height, System.currentTimeMillis());
                for (FrameCaptureListener listener : frameListeners) {
                    listener.onFrame(frame);
                }
            }
        });
    }

    /**
     * Dispatches onBrowserError to listeners, then loads the error page HTML.
     */
    private void notifyAndShowError(BrowserError error) {
        for (BrowserEventListener listener : listeners) {
            listener.onBrowserError(error);
        }
        loadHTML(errorPageRenderer.render(error));
    }

    private void recordHistory(String url) {
        if (options != null && options.getSession() != null && options.getSession().isIncognito()) {
            return;
        }
        String profileId = "global";
        if (options != null && options.getSession() != null && options.getSession().getProfile() != null) {
            profileId = options.getSession().getProfile().getProfileEntry().getProfileId();
        }
        historyManager.recordVisit(url, profileId);
    }

    private void recordTitle(String title) {
        if (options != null && options.getSession() != null && options.getSession().isIncognito()) {
            return;
        }
        String profileId = "global";
        if (options != null && options.getSession() != null && options.getSession().getProfile() != null) {
            profileId = options.getSession().getProfile().getProfileEntry().getProfileId();
        }
        historyManager.updateTitle(cefBrowser.getURL(), title, profileId);
    }

    @Override
    public ErrorPageRegistry errors() {
        return errorPageRegistry;
    }

    @Override
    public DownloadManager downloads() {
        return downloadManager;
    }

    @Override
    public CacheManager cache() {
        return cacheManager;
    }

    @Override
    public AutomationModule automation() {
        return automationModule;
    }

    @Override
    public HistoryManager history() {
        return historyManager;
    }

    @Override
    public AutocompleteService autocomplete() {
        return autocompleteService;
    }

    @Override
    public void openDevTools() {
        devToolsModule.open();
    }

    @Override
    public void loadURL(String url) {
        cefBrowser.loadURL(url);
    }

    @Override
    public void loadHTML(String html) {
        // For small payloads use data: URI (fast, no I/O).
        // For larger content write to a temp file and use file:// to avoid URL length limits.
        final int DATA_URL_THRESHOLD = 512 * 1024; // 512 KB
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= DATA_URL_THRESHOLD) {
            String encoded = java.net.URLEncoder.encode(html, StandardCharsets.UTF_8);
            cefBrowser.loadURL("data:text/html;charset=utf-8," + encoded);
        } else {
            try {
                Path tmp = Files.createTempFile("browser4j-html-", ".html");
                tmp.toFile().deleteOnExit();
                Files.write(tmp, bytes);
                cefBrowser.loadURL(tmp.toUri().toString());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write HTML to temp file", e);
            }
        }
    }

    @Override
    public void loadHTML(InputStream htmlStream) throws IOException {
        byte[] bytes = htmlStream.readAllBytes();
        htmlStream.close();
        Path tmp = Files.createTempFile("browser4j-html-", ".html");
        tmp.toFile().deleteOnExit();
        Files.write(tmp, bytes);
        cefBrowser.loadURL(tmp.toUri().toString());
    }

    @Override
    public void loadFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        cefBrowser.loadURL(file.toURI().toString());
    }

    @Override
    public void reload() {
        cefBrowser.reload();
    }

    @Override
    public void goBack() {
        if (cefBrowser.canGoBack()) {
            cefBrowser.goBack();
        }
    }

    @Override
    public void goForward() {
        if (cefBrowser.canGoForward()) {
            cefBrowser.goForward();
        }
    }

    @Override
    public void close() {
        cefBrowser.close(true);
        jsBridge.dispose();
        cefClient.dispose();
    }

    @Override
    public void addEventListener(BrowserEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(BrowserEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addDomMutationListener(DomMutationListener listener) {
        domMutationListeners.add(listener);
    }

    @Override
    public void removeDomMutationListener(DomMutationListener listener) {
        domMutationListeners.remove(listener);
    }

    @Override
    public void addFrameCaptureListener(FrameCaptureListener listener) {
        frameListeners.add(listener);
    }

    @Override
    public void removeFrameCaptureListener(FrameCaptureListener listener) {
        frameListeners.remove(listener);
    }

    @Override
    public void postMessage(String event, Object data) {
        jsBridge.postMessage(event, data);
    }

    @Override
    public NetworkModule network() {
        return networkHandler;
    }

    @Override
    public SecurityModule security() {
        return securityModule;
    }

    @Override
    public DevToolsModule devtools() {
        return devToolsModule;
    }

    @Override
    public void onConsoleMessage(Consumer<String> handler) {
        this.consoleMessageHandler = handler;
    }

    @Override
    public CookieManager cookies() {
        return cookieManager;
    }

    @Override
    public balbucio.browser4j.storage.api.StorageModule storage() {
        return storageModule;
    }

    @Override
    public MediaModule media() {
        return mediaModule;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> isDRMProtected() {
        return jsBridge.evaluateJavaScript(DRMInjector.EVALUATION_SCRIPT)
                .thenApply(res -> Boolean.TRUE.equals(res));
    }

    @Override
    public PermissionModule permissions() {
        return permissionModule;
    }

    @Override
    public java.util.concurrent.CompletableFuture<org.jsoup.nodes.Document> getDOM() {
        java.util.concurrent.CompletableFuture<org.jsoup.nodes.Document> future = new java.util.concurrent.CompletableFuture<>();
        cefBrowser.getSource(string -> {
            try {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(string);
                future.complete(doc);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public java.util.concurrent.CompletableFuture<SiteMetadata> getSiteMetadata() {
        String currentUrl = cefBrowser.getURL();
        return getDOM().thenApply(doc -> SiteMetadata.fromDocument(doc, currentUrl));
    }

    @Override
    public Object getNativeBrowser() {
        return cefBrowser;
    }

    @Override
    public InputController getInputController() {
        return inputController;
    }

    public BrowserView getView() {
        return new SwingBrowserView(cefBrowser);
    }
}
