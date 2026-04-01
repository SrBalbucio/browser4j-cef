package balbucio.browser4j.core.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import balbucio.browser4j.download.config.DownloadConfig;
import balbucio.browser4j.observability.BrowserMetric;
import lombok.Getter;

@Getter
public class BrowserRuntimeConfiguration {

    private final String cachePath;
    private final String userDataPath;
    private final String nativeCachePath;
    private final boolean disableNativeLoader;
    private final boolean enableGPU;
    private final boolean enableSandbox;
    private final boolean osrEnabled;
    private final boolean cookiesPersistent;
    private final int windowlessFrameRate;
    private final boolean enableNetworkInterception;
    private final boolean enableSecurity;
    private final Consumer<BrowserMetric> metricHandler;
    private final int remoteDebuggingPort;
    private final String userAgent;
    private final balbucio.browser4j.network.proxy.ProxyConfig proxy;
    private final boolean enableMediaStream;
    private final boolean enableUserMediaScreenCapturing;
    private final boolean useFakeUiForMediaStream;
    private final boolean useFakeDeviceForMediaStream;
    private final boolean allowHttpScreenCapture;
    private final List<String> unsafelyTreatInsecureOriginAsSecure;
    private final String autoSelectDesktopCaptureSource;
    private final boolean enableWebRTC;
    private final String webrtcIPHandlingPolicy;
    private final String autoPlayPolicy;
    private final DownloadConfig downloadConfig;

    private BrowserRuntimeConfiguration(Builder builder) {
        this.cachePath = builder.cachePath;
        this.userDataPath = builder.userDataPath;
        this.nativeCachePath = builder.nativeCachePath;
        this.disableNativeLoader = builder.disableNativeLoader;
        this.enableGPU = builder.enableGPU;
        this.enableSandbox = builder.enableSandbox;
        this.osrEnabled = builder.osrEnabled;
        this.cookiesPersistent = builder.cookiesPersistent;
        this.windowlessFrameRate = builder.windowlessFrameRate;
        this.enableNetworkInterception = builder.enableNetworkInterception;
        this.enableSecurity = builder.enableSecurity;
        this.metricHandler = builder.metricHandler;
        this.remoteDebuggingPort = builder.remoteDebuggingPort;
        this.userAgent = builder.userAgent;
        this.proxy = builder.proxy;
        this.enableMediaStream = builder.enableMediaStream;
        this.enableUserMediaScreenCapturing = builder.enableUserMediaScreenCapturing;
        this.useFakeUiForMediaStream = builder.useFakeUiForMediaStream;
        this.useFakeDeviceForMediaStream = builder.useFakeDeviceForMediaStream;
        this.allowHttpScreenCapture = builder.allowHttpScreenCapture;
        this.unsafelyTreatInsecureOriginAsSecure = builder.unsafelyTreatInsecureOriginAsSecure;
        this.autoSelectDesktopCaptureSource = builder.autoSelectDesktopCaptureSource;
        this.enableWebRTC = builder.enableWebRTC;
        this.webrtcIPHandlingPolicy = builder.webrtcIPHandlingPolicy;
        this.autoPlayPolicy = builder.autoPlayPolicy;
        this.downloadConfig = builder.downloadConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cachePath = System.getProperty(
                balbucio.browser4j.browser.profile.ProfileManager.CACHE_PATH_PROPERTY,
                new File(".cache").getAbsolutePath());
        private String userDataPath = new File(".userdata").getAbsolutePath();
        private String nativeCachePath = System.getProperty(
                "browser4j.native.cache.path",
                new File(".native-cache").getAbsolutePath());
        private boolean disableNativeLoader;
        private boolean enableGPU = true;
        private boolean enableSandbox = false;
        private boolean osrEnabled = false;
        private boolean cookiesPersistent = true;
        private int windowlessFrameRate = 60;
        private boolean enableNetworkInterception = true;
        private boolean enableSecurity = true;
        private Consumer<BrowserMetric> metricHandler = null;
        private int remoteDebuggingPort = 9222;
        private String userAgent = null;
        private balbucio.browser4j.network.proxy.ProxyConfig proxy = null;
        private boolean enableMediaStream = false;
        private boolean enableUserMediaScreenCapturing = false;
        private boolean useFakeUiForMediaStream = false;
        private boolean useFakeDeviceForMediaStream = false;
        private boolean allowHttpScreenCapture = false;
        private List<String> unsafelyTreatInsecureOriginAsSecure = new ArrayList<>();
        private String autoSelectDesktopCaptureSource = null;
        private boolean enableWebRTC = true;
        private String webrtcIPHandlingPolicy = "default";
        private String autoPlayPolicy = "no-user-gesture-required";
        private DownloadConfig downloadConfig;

        public Builder downloadConfig(DownloadConfig downloadConfig) {
            this.downloadConfig = downloadConfig;
            return this;
        }

        public Builder disableNativeLoader() {
            this.disableNativeLoader = true;
            return this;
        }

        public Builder cachePath(String cachePath) {
            this.cachePath = cachePath;
            return this;
        }

        public Builder userDataPath(String userDataPath) {
            this.userDataPath = userDataPath;
            return this;
        }

        public Builder nativeCachePath(String nativeCachePath) {
            this.nativeCachePath = nativeCachePath;
            return this;
        }

        public Builder enableGPU(boolean enableGPU) {
            this.enableGPU = enableGPU;
            return this;
        }

        public Builder enableSandbox(boolean enableSandbox) {
            this.enableSandbox = enableSandbox;
            return this;
        }
        
        public Builder osrEnabled(boolean osrEnabled) {
            this.osrEnabled = osrEnabled;
            return this;
        }

        public Builder cookiesPersistent(boolean cookiesPersistent) {
            this.cookiesPersistent = cookiesPersistent;
            return this;
        }

        public Builder windowlessFrameRate(int windowlessFrameRate) {
            this.windowlessFrameRate = windowlessFrameRate;
            return this;
        }

        public Builder enableNetworkInterception(boolean enableNetworkInterception) {
            this.enableNetworkInterception = enableNetworkInterception;
            return this;
        }

        public Builder enableSecurity(boolean enableSecurity) {
            this.enableSecurity = enableSecurity;
            return this;
        }

        public Builder metricHandler(Consumer<BrowserMetric> metricHandler) {
            this.metricHandler = metricHandler;
            return this;
        }

        public Builder remoteDebuggingPort(int remoteDebuggingPort) {
            this.remoteDebuggingPort = remoteDebuggingPort;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder proxy(balbucio.browser4j.network.proxy.ProxyConfig proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder enableMediaStream(boolean enableMediaStream) {
            this.enableMediaStream = enableMediaStream;
            return this;
        }

        public Builder enableUserMediaScreenCapturing(boolean enableUserMediaScreenCapturing) {
            this.enableUserMediaScreenCapturing = enableUserMediaScreenCapturing;
            return this;
        }

        public Builder useFakeUiForMediaStream(boolean useFakeUiForMediaStream) {
            this.useFakeUiForMediaStream = useFakeUiForMediaStream;
            return this;
        }

        public Builder useFakeDeviceForMediaStream(boolean useFakeDeviceForMediaStream) {
            this.useFakeDeviceForMediaStream = useFakeDeviceForMediaStream;
            return this;
        }

        public Builder allowHttpScreenCapture(boolean allowHttpScreenCapture) {
            this.allowHttpScreenCapture = allowHttpScreenCapture;
            return this;
        }

        public Builder unsafelyTreatInsecureOriginAsSecure(List<String> origins) {
            this.unsafelyTreatInsecureOriginAsSecure = origins;
            return this;
        }

        public Builder addUnsafelyTreatInsecureOriginAsSecure(String origin) {
            this.unsafelyTreatInsecureOriginAsSecure.add(origin);
            return this;
        }

        public Builder autoSelectDesktopCaptureSource(String source) {
            this.autoSelectDesktopCaptureSource = source;
            return this;
        }

        public Builder enableWebRTC(boolean enableWebRTC) {
            this.enableWebRTC = enableWebRTC;
            return this;
        }

        public Builder webrtcIPHandlingPolicy(String policy) {
            this.webrtcIPHandlingPolicy = policy;
            return this;
        }

        public Builder autoPlayPolicy(String policy) {
            this.autoPlayPolicy = policy;
            return this;
        }

        public BrowserRuntimeConfiguration build() {
            return new BrowserRuntimeConfiguration(this);
        }
    }
}
