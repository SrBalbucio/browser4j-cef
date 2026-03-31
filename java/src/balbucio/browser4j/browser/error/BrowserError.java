package balbucio.browser4j.browser.error;

/**
 * Immutable representation of a browser error event, carrying all available
 * context about what went wrong and where.
 */
public class BrowserError {

    private final String url;
    private final int cefErrorCode;
    private final String errorText;
    private final BrowserErrorType type;
    private final int httpStatusCode;

    private BrowserError(Builder builder) {
        this.url            = builder.url;
        this.cefErrorCode   = builder.cefErrorCode;
        this.errorText      = builder.errorText;
        this.type           = builder.type;
        this.httpStatusCode = builder.httpStatusCode;
    }

    /** The URL that failed to load. */
    public String getUrl() { return url; }

    /**
     * The raw CEF error code (negative integer per CEF spec).
     * 0 when the error originated from an HTTP status rather than a CEF load failure.
     */
    public int getCefErrorCode() { return cefErrorCode; }

    /** Human-readable error text supplied by CEF, or a generated description for HTTP errors. */
    public String getErrorText() { return errorText; }

    /** Semantic error category for use in registry lookups and display logic. */
    public BrowserErrorType getType() { return type; }

    /**
     * HTTP status code if the error came from {@code onLoadEnd} (e.g. 404, 503).
     * 0 when the error came from {@code onLoadError} (network-level error).
     */
    public int getHttpStatusCode() { return httpStatusCode; }

    /** Returns true if this error originated from an HTTP response status code. */
    public boolean isHttpError() { return httpStatusCode > 0; }

    @Override
    public String toString() {
        return "BrowserError{type=" + type + ", url=" + url
                + (isHttpError() ? ", http=" + httpStatusCode : ", cef=" + cefErrorCode) + "}";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String url = "";
        private int cefErrorCode = 0;
        private String errorText = "";
        private BrowserErrorType type = BrowserErrorType.UNKNOWN;
        private int httpStatusCode = 0;

        public Builder url(String url)                   { this.url = url;                     return this; }
        public Builder cefErrorCode(int code)            { this.cefErrorCode = code;            return this; }
        public Builder errorText(String text)            { this.errorText = text;               return this; }
        public Builder type(BrowserErrorType type)       { this.type = type;                    return this; }
        public Builder httpStatusCode(int httpStatus)    { this.httpStatusCode = httpStatus;    return this; }

        public BrowserError build() { return new BrowserError(this); }
    }
}
