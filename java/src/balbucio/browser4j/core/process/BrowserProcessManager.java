package balbucio.browser4j.core.process;

import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;
import org.cef.CefSettings;
import org.cef.CefApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserProcessManager {
    private static final Logger log = LoggerFactory.getLogger(BrowserProcessManager.class);

    public static void configureArgs(CefSettings settings, BrowserRuntimeConfiguration config) {
        log.info("Configuring browser process arguments...");
        
        CefApp.addAppHandler(new org.cef.handler.CefAppHandlerAdapter(null) {
            @Override
            public void onBeforeCommandLineProcessing(String process_type, org.cef.callback.CefCommandLine command_line) {
                if (!config.isEnableGPU()) {
                    command_line.appendSwitch("disable-gpu");
                    command_line.appendSwitch("disable-gpu-compositing");
                }
                
                if (!config.isEnableSandbox()) {
                    command_line.appendSwitch("no-sandbox");
                    command_line.appendSwitch("disable-setuid-sandbox");
                }
                
                if (config.isOsrEnabled()) {
                    command_line.appendSwitch("disable-gpu-vsync");
                    if (config.getWindowlessFrameRate() <= 0) {
                        command_line.appendSwitch("disable-frame-rate-limit");
                    }
                }
                
                // Add default flags for reliability in headless or critical desktop apps
                command_line.appendSwitch("disable-web-security"); // Depending on use case
                command_line.appendSwitch("ignore-certificate-errors");
                command_line.appendSwitchWithValue("disable-blink-features", "AutomationControlled");

                if (config.getRemoteDebuggingPort() > 0) {
                    command_line.appendSwitchWithValue("remote-debugging-port", String.valueOf(config.getRemoteDebuggingPort()));
                }

                if (config.getUserAgent() != null && !config.getUserAgent().isEmpty()) {
                    command_line.appendSwitchWithValue("user-agent", config.getUserAgent());
                }

                if (config.getProxy() != null) {
                    String proxyServer = config.getProxy().getServerString();
                    if (proxyServer != null) {
                        command_line.appendSwitchWithValue("proxy-server", proxyServer);
                    }
                    if (config.getProxy().getBypassList() != null) {
                        command_line.appendSwitchWithValue("proxy-bypass-list", config.getProxy().getBypassList());
                    }
                }

                if (config.isEnableMediaStream()) {
                    command_line.appendSwitch("enable-media-stream");
                }

                if (config.isEnableUserMediaScreenCapturing()) {
                    command_line.appendSwitch("enable-usermedia-screen-capturing");
                }

                if (config.isUseFakeUiForMediaStream()) {
                    log.warn("USING FAKE UI FOR MEDIA STREAM - ONLY FOR TESTING PURPOSES");
                    command_line.appendSwitch("use-fake-ui-for-media-stream");
                }

                if (config.isUseFakeDeviceForMediaStream()) {
                    log.warn("USING FAKE DEVICE FOR MEDIA STREAM - ONLY FOR TESTING PURPOSES");
                    command_line.appendSwitch("use-fake-device-for-media-stream");
                }

                if (config.isAllowHttpScreenCapture()) {
                    command_line.appendSwitch("allow-http-screen-capture");
                }

                if (!config.getUnsafelyTreatInsecureOriginAsSecure().isEmpty()) {
                    command_line.appendSwitchWithValue("unsafely-treat-insecure-origin-as-secure",
                            String.join(",", config.getUnsafelyTreatInsecureOriginAsSecure()));
                }

                if (config.getAutoSelectDesktopCaptureSource() != null) {
                    command_line.appendSwitchWithValue("auto-select-desktop-capture-source",
                            config.getAutoSelectDesktopCaptureSource());
                }

                if (config.isEnableWebRTC()) {
                    command_line.appendSwitch("enable-webrtc-hide-local-ips-with-mdns");
                }

                if (config.getWebrtcIPHandlingPolicy() != null) {
                    command_line.appendSwitchWithValue("force-webrtc-ip-handling-policy",
                            config.getWebrtcIPHandlingPolicy());
                }

                if (config.getAutoPlayPolicy() != null) {
                    command_line.appendSwitchWithValue("autoplay-policy", config.getAutoPlayPolicy());
                }

                log.debug("Chromium flags set to: {}", command_line.getArguments().toString());
            }
        });
    }
}
