package balbucio.browser4j.browser.error;

/**
 * Semantic categories of browser errors, grouping related CEF error codes
 * and HTTP status codes into user-meaningful families.
 */
public enum BrowserErrorType {

    /** DNS resolution failed — host not found or no internet. */
    DNS_FAILURE,

    /** No active network connection. */
    NO_CONNECTION,

    /** SSL/TLS certificate or handshake error. */
    SSL_ERROR,

    /** Connection timed out. */
    TIMEOUT,

    /** Remote host actively refused the connection. */
    CONNECTION_REFUSED,

    /** HTTP 404 — resource not found. */
    NOT_FOUND,

    /** HTTP 5xx — server-side error (500, 502, 503, 504). */
    SERVER_ERROR,

    /** Any other load error not covered by the categories above. */
    UNKNOWN;

    /**
     * Classifies a CEF error code (from {@code ErrorCode.getCode()}) into a {@link BrowserErrorType}.
     *
     * @param cefCode the integer CEF error code (negative values as per CEF spec)
     * @return the most appropriate BrowserErrorType
     */
    public static BrowserErrorType fromCefCode(int cefCode) {
        // CEF error codes reference: https://magpcss.org/ceforum/apidocs3/projects/(default)/cef_errorcode_t.html
        return switch (cefCode) {
            // DNS
            case -105, // ERR_NAME_NOT_RESOLVED
                 -137  // ERR_NAME_RESOLUTION_FAILED
                    -> DNS_FAILURE;

            // No internet / network change
            case -21,  // ERR_NETWORK_CHANGED
                 -106  // ERR_INTERNET_DISCONNECTED
                    -> NO_CONNECTION;

            // SSL / certificate
            case -200, // ERR_CERT_COMMON_NAME_INVALID
                 -201, // ERR_CERT_DATE_INVALID
                 -202, // ERR_CERT_AUTHORITY_INVALID
                 -203, // ERR_CERT_CONTAINS_ERRORS
                 -204, // ERR_CERT_NO_REVOCATION_MECHANISM
                 -205, // ERR_CERT_UNABLE_TO_CHECK_REVOCATION
                 -206, // ERR_CERT_REVOKED
                 -207, // ERR_CERT_INVALID
                 -208, // ERR_CERT_WEAK_SIGNATURE_ALGORITHM
                 -210, // ERR_CERT_NON_UNIQUE_NAME
                 -211, // ERR_CERT_WEAK_KEY
                 -212, // ERR_CERT_NAME_CONSTRAINT_VIOLATION
                 -213, // ERR_CERT_VALIDITY_TOO_LONG
                 -300, // ERR_SSL_PROTOCOL_ERROR
                 -301, // ERR_SSL_CLIENT_AUTH_CERT_NEEDED
                 -302, // ERR_TUNNEL_CONNECTION_FAILED
                 -303, // ERR_NO_SSL_VERSIONS_ENABLED
                 -307  // ERR_SSL_CLIENT_AUTH_NO_COMMON_ALGORITHMS
                    -> SSL_ERROR;

            // Timeouts
            case -7,  // ERR_TIMED_OUT
                 -118 // ERR_CONNECTION_TIMED_OUT
                    -> TIMEOUT;

            // Connection refused
            case -102 // ERR_CONNECTION_REFUSED
                    -> CONNECTION_REFUSED;

            default -> UNKNOWN;
        };
    }

    /**
     * Classifies an HTTP status code into a {@link BrowserErrorType}.
     * Returns {@code null} if the status is not an intercepted error.
     *
     * @param httpStatus the HTTP response status code
     * @return the BrowserErrorType, or null if not intercepted by default
     */
    public static BrowserErrorType fromHttpStatus(int httpStatus) {
        return switch (httpStatus) {
            case 404, 410 -> NOT_FOUND;
            case 500, 502, 503, 504 -> SERVER_ERROR;
            default -> null; // not intercepted by default
        };
    }
}
