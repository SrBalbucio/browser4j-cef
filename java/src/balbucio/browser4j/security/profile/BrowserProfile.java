package balbucio.browser4j.security.profile;

import balbucio.browser4j.browser.profile.ProfileEntry;

public class BrowserProfile {
    private final String profileId;
    private final FingerprintProfile fingerprint;
    private final ProfileEntry profileEntry;

    private BrowserProfile(Builder builder) {
        this.profileId    = builder.profileId;
        this.fingerprint  = builder.fingerprint;
        this.profileEntry = builder.profileEntry;
    }

    public String getProfileId()         { return profileId;    }
    public FingerprintProfile getFingerprint() { return fingerprint; }
    public ProfileEntry getProfileEntry() { return profileEntry; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String profileId = java.util.UUID.randomUUID().toString();
        private FingerprintProfile fingerprint = FingerprintProfile.builder().build();
        private ProfileEntry profileEntry;

        public Builder profileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder fingerprint(FingerprintProfile fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder profileEntry(ProfileEntry profileEntry) {
            this.profileEntry = profileEntry;
            return this;
        }

        public BrowserProfile build() {
            return new BrowserProfile(this);
        }
    }
}
