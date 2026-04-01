import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.CefBrowserImpl;
import balbucio.browser4j.cache.config.CacheConfig;
import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;
import balbucio.browser4j.core.runtime.BrowserRuntime;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Browser4jTest extends JFrame {

    public static void main(String[] args) {
        try {
            File cachePath = new File("build/browser/cache");
            cachePath.mkdirs();
            File userDataPath = new File("build/browser/user");
            userDataPath.mkdirs();

            BrowserRuntime.init(BrowserRuntimeConfiguration.builder()
                    .enableGPU(true)
                    .enableMediaStream(true)
                    .enableWebRTC(true)
                    .enableUserMediaScreenCapturing(true)
                    .cookiesPersistent(true)
                    .enableNetworkInterception(false)
                    .enableSecurity(false)
                    .osrEnabled(false)
                    .cachePath(cachePath.getAbsolutePath())
                    .userDataPath(userDataPath.getAbsolutePath())
                    .nativeCachePath(new File("binary_distrib/win64/bin/lib/win64").getAbsolutePath())
                    .disableNativeLoader()
                    .build());

            new Browser4jTest();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Browser4jTest() {
        super("Browser4j - Example");
        this.setSize(1280, 720);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CefBrowserImpl browser = (CefBrowserImpl) CefBrowserImpl.create(BrowserRuntime.getCefApp(), BrowserOptions.builder()
                        .cacheConfig(CacheConfig.builder().enabled(true).sharedCacheEnabled(true).maxEntries(10000).maxCacheSizeBytes(99999999).build())
                        .initialUrl("https://google.com")
                .build());

        this.add(browser.getView().getUIComponent(), BorderLayout.CENTER);
        this.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            browser.loadURL("https://youtube.com");
        });
    }
}