package balbucio.browser4j.observability;

import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;
import balbucio.browser4j.core.runtime.BrowserRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsTracker {
    private final ConcurrentHashMap<Long, Long> requestStartTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);

    private final AtomicLong framesRendered = new AtomicLong(0);
    private long lastFpsTime = System.currentTimeMillis();
    private volatile int currentFps = 0;

    private static final ObjectMapper mapper = new ObjectMapper();

    public void markRequestStart(long identifier) {
        requestStartTimes.put(identifier, System.currentTimeMillis());
        totalRequests.incrementAndGet();
    }

    public void markRequestEnd(long identifier, String url, int statusCode, long bytesLoaded) {
        Long startTime = requestStartTimes.remove(identifier);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            try {
                BrowserRuntimeConfiguration config = BrowserRuntime.getConfig();
                if (config != null && config.getMetricHandler() != null) {
                    config.getMetricHandler().accept(new BrowserMetric(url, duration, statusCode, bytesLoaded));
                }
            } catch (IllegalStateException e) {
            }
        }
    }

    public void markFrame() {
        framesRendered.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 1000) {
            currentFps = (int) framesRendered.getAndSet(0);
            lastFpsTime = now;
        }
    }

    public int getCurrentFps() {
        return currentFps;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public String export() {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("fps", currentFps);
            root.put("totalRequests", totalRequests.get());
            root.put("timestamp", System.currentTimeMillis());
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }
}
