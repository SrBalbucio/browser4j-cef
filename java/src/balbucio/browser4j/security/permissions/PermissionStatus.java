package balbucio.browser4j.security.permissions;

/**
 * Result status for a permission request.
 */
public enum PermissionStatus {
    /** Permission granted. */
    GRANTED,
    
    /** Permission denied. */
    DENIED,
    
    /** Prompt the user for an answer. */
    ASK;

    public static PermissionStatus fromString(String status) {
        if (status == null) return ASK;
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ASK;
        }
    }
}
