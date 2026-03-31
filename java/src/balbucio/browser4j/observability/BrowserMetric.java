package balbucio.browser4j.observability;

public class BrowserMetric {
    private final String url;
    private final long durationMs;
    private final int statusCode;
    private final long bytesLoaded;

    public BrowserMetric(String url, long durationMs, int statusCode, long bytesLoaded) {
        this.url = url;
        this.durationMs = durationMs;
        this.statusCode = statusCode;
        this.bytesLoaded = bytesLoaded;
    }

    public String getUrl() { return url; }
    public long getDurationMs() { return durationMs; }
    public int getStatusCode() { return statusCode; }
    public long getBytesLoaded() { return bytesLoaded; }
}
