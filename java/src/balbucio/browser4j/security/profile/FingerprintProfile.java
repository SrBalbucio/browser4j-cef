package balbucio.browser4j.security.profile;

public class FingerprintProfile {
    private final String userAgent;
    private final String acceptLanguage;
    private final String timezone;
    private final int screenWidth;
    private final int screenHeight;
    private final String platform;
    private final int hardwareConcurrency;
    private final int deviceMemory;

    private FingerprintProfile(Builder builder) {
        this.userAgent = builder.userAgent;
        this.acceptLanguage = builder.acceptLanguage;
        this.timezone = builder.timezone;
        this.screenWidth = builder.screenWidth;
        this.screenHeight = builder.screenHeight;
        this.platform = builder.platform;
        this.hardwareConcurrency = builder.hardwareConcurrency;
        this.deviceMemory = builder.deviceMemory;
    }

    public String getUserAgent() { return userAgent; }
    public String getAcceptLanguage() { return acceptLanguage; }
    public String getTimezone() { return timezone; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public String getPlatform() { return platform; }
    public int getHardwareConcurrency() { return hardwareConcurrency; }
    public int getDeviceMemory() { return deviceMemory; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userAgent = "Mozilla/5.0";
        private String acceptLanguage = "en-US,en;q=0.9";
        private String timezone = "America/New_York";
        private int screenWidth = 1920;
        private int screenHeight = 1080;
        private String platform = "Win32";
        private int hardwareConcurrency = 4;
        private int deviceMemory = 8;

        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder acceptLanguage(String acceptLanguage) { this.acceptLanguage = acceptLanguage; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }
        public Builder screen(int width, int height) { this.screenWidth = width; this.screenHeight = height; return this; }
        public Builder platform(String platform) { this.platform = platform; return this; }
        public Builder hardwareConcurrency(int cores) { this.hardwareConcurrency = cores; return this; }
        public Builder deviceMemory(int gb) { this.deviceMemory = gb; return this; }

        public FingerprintProfile build() { return new FingerprintProfile(this); }
    }
}
