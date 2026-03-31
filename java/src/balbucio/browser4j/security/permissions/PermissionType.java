package balbucio.browser4j.security.permissions;

/**
 * Supported browser permission types.
 */
public enum PermissionType {
    /** Geolocation access (e.g. navigator.geolocation) */
    GEOLOCATION,
    
    /** Push notifications and related APIs */
    NOTIFICATIONS,
    
    /** Microphones access (MediaStream) */
    MEDIASTREAM_MIC,
    
    /** Cameras access (MediaStream) */
    MEDIASTREAM_CAMERA,
    
    /** MIDI system exclusive messages access */
    MIDI_SYSEX,
    
    /** Clipboard read/write access */
    CLIPBOARD_READ_WRITE,
    
    /** Local devices discovery and connection */
    DEVICES,
    
    /** Screen capture (e.g. getDisplayMedia) */
    SCREEN_CAPTURE;

    /**
     * Converts a JCEF/Chromium string to PermissionType.
     * @param cefType string from CEF
     * @return PermissionType or null if not supported
     */
    public static PermissionType fromCef(String cefType) {
        if (cefType == null) return null;
        return switch (cefType.toLowerCase()) {
            case "geolocation" -> GEOLOCATION;
            case "notifications" -> NOTIFICATIONS;
            case "mediastream_mic", "microphone" -> MEDIASTREAM_MIC;
            case "mediastream_camera", "camera" -> MEDIASTREAM_CAMERA;
            case "midi_sysex" -> MIDI_SYSEX;
            case "clipboard" -> CLIPBOARD_READ_WRITE;
            default -> null;
        };
    }
}
