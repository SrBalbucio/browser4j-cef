package balbucio.browser4j.core.runtime;

import balbucio.browser4j.browser.profile.ProfileManager;
import balbucio.browser4j.cache.api.CacheManager;
import balbucio.browser4j.cache.api.CacheManagerImpl;
import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;
import balbucio.browser4j.download.api.DownloadManager;
import balbucio.browser4j.download.api.DownloadManagerImpl;
import balbucio.browser4j.download.config.DownloadConfig;
import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.api.HistoryManagerImpl;
import balbucio.browser4j.history.service.AutocompleteService;
import balbucio.browser4j.security.permissions.PermissionModule;
import balbucio.browser4j.security.permissions.PermissionModuleImpl;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;
import balbucio.browser4j.core.process.BrowserProcessManager;

import java.nio.file.Path;

@Getter
public class BrowserRuntime {

    private static final Logger log = LoggerFactory.getLogger(BrowserRuntime.class);

    private static BrowserRuntime instance;
    private final BrowserRuntimeConfiguration config;
    private boolean initialized = false;
    private CefApp cefApp;
    private DownloadManager downloadManager;
    private HistoryManager historyManager;
    private AutocompleteService autocompleteService;
    private CacheManager cacheManager;
    private PermissionModule permissionModule;

    private BrowserRuntime(BrowserRuntimeConfiguration config) {
        this.config = config;
    }

    public static synchronized void init(BrowserRuntimeConfiguration config) {
        if (instance != null) {
            log.warn("BrowserRuntime is already initialized. Ignoring init call.");
            return;
        }

        validateOS();

        instance = new BrowserRuntime(config);
        instance.doInit();
    }

    public static boolean is64Bit;
    public static boolean isWin;
    public static boolean isLinux;
    public static boolean isMac;

    private static void validateOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        is64Bit = osArch.contains("64");
        isWin = osName.contains("win");
        isLinux = osName.contains("linux") || osName.contains("nix");
        isMac = osName.contains("mac");

        if (!is64Bit) {
            throw new UnsupportedOperationException("BrowserRuntime requires a 64-bit architecture.");
        }

        if (!isWin && !isLinux && !isMac) {
            throw new UnsupportedOperationException("BrowserRuntime does not support OS: " + osName);
        }
    }

    private synchronized void doInit() {
        if (initialized) return;

        log.info("Initializing BrowserRuntime...");

        NativeBundle.PreparedNatives natives = null;

        if (!config.isDisableNativeLoader()) {
            // Prepare native bundle (extract + set custom loader) BEFORE any JCEF startup call.
            natives = NativeBundle.prepare(Path.of(config.getNativeCachePath()));
            SystemBootstrap.setLoader(natives.loader);
        } else {
            log.info("Browser4j is not managing natives. If the initialization fails, this is probably the reason.");
        }

        if (!CefApp.startup(null)) {
            throw new RuntimeException("Failed to startup native JCEF components.");
        }

        CefSettings settings = new CefSettings();
        settings.cache_path = config.getCachePath();
//        settings. = config.getUserDataPath();
//        settings.no_sandbox = !config.isEnableSandbox(); // Utilizar via args
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_DEFAULT;
        settings.windowless_rendering_enabled = config.isOsrEnabled();
        settings.persist_session_cookies = config.isCookiesPersistent();

        // Point JCEF to the extracted native/runtime assets.
        Path root = natives != null ? natives.extractedRoot : Path.of(config.getNativeCachePath());
        if (natives != null ? natives.platform.os == NativeBundle.Os.WIN : isWin) {
            settings.browser_subprocess_path = root.resolve("jcef_helper.exe").toAbsolutePath().toString();
            settings.resources_dir_path = root.toAbsolutePath().toString();
            settings.locales_dir_path = root.resolve("locales").toAbsolutePath().toString();
        } else {
            settings.browser_subprocess_path = root.resolve("jcef_helper").toAbsolutePath().toString();
            settings.resources_dir_path = root.toAbsolutePath().toString();
            settings.locales_dir_path = root.resolve("locales").toAbsolutePath().toString();
        }

        // Custom config via Process Manager
        BrowserProcessManager.configureArgs(settings, config);

        Path browserUserDataPath = Path.of(config.getUserDataPath());
        Path profilePath = browserUserDataPath;

        if (ProfileManager.get() != null && ProfileManager.get().hasActiveProfile()) {
            profilePath = ProfileManager.get().getActiveProfileDir();
        }

        cefApp = CefApp.getInstance(settings);
        downloadManager = new DownloadManagerImpl(config.getDownloadConfig());
        historyManager = new HistoryManagerImpl(profilePath);
        autocompleteService = new AutocompleteService(historyManager);
        cacheManager = new CacheManagerImpl(config.getCacheConfig(), browserUserDataPath);
        permissionModule = new PermissionModuleImpl(profilePath);
        initialized = true;
        log.info("BrowserRuntime initialized successfully.");
    }

    public static synchronized void shutdown() {
        if (instance != null && instance.initialized) {
            log.info("Shutting down BrowserRuntime...");
            CefApp.getInstance().dispose();
            instance.downloadManager = null;
            instance.historyManager = null;
            instance.autocompleteService = null;
            instance.initialized = false;
            instance = null;
        }
    }

    public static CefApp getCefApp() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }
        return instance.cefApp;
    }

    public static BrowserRuntimeConfiguration getConfig() {
        if (instance == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized.");
        }
        return instance.config;
    }

    public static DownloadManager getDownloadManager() {
        if (instance == null || !instance.initialized || instance.downloadManager == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }
        return instance.downloadManager;
    }

    public static DownloadManagerImpl getDownloadManagerImpl() {
        return (DownloadManagerImpl) getDownloadManager();
    }

    public static HistoryManager getHistoryManager() {
        if (instance == null || !instance.initialized || instance.historyManager == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }
        return instance.historyManager;
    }

    public static AutocompleteService getAutocompleteService() {
        if (instance == null || !instance.initialized || instance.autocompleteService == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }
        return instance.autocompleteService;
    }

    public static CacheManager getCacheManager() {
        if (instance == null || !instance.initialized || instance.cacheManager == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }

        return instance.cacheManager;
    }

    public static PermissionModule getPermissionModule(){
        if (instance == null || !instance.initialized || instance.permissionModule == null) {
            throw new IllegalStateException("BrowserRuntime is not initialized. Call init() first.");
        }

        return instance.permissionModule;
    }
}
