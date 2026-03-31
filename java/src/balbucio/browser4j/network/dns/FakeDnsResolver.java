package balbucio.browser4j.network.dns;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeDnsResolver {
    private final Map<String, String> overrides = new ConcurrentHashMap<>();

    public void addOverride(String hostname, String destination) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new IllegalArgumentException("hostname não pode ser nulo ou vazio");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("destination não pode ser nulo ou vazio");
        }
        overrides.put(normalize(hostname), destination.trim());
    }

    public String removeOverride(String hostname) {
        if (hostname == null) return null;
        return overrides.remove(normalize(hostname));
    }

    public void clearOverrides() {
        overrides.clear();
    }

    public String resolve(String hostname) {
        if (hostname == null) return null;
        return overrides.get(normalize(hostname));
    }

    public Map<String, String> getEntries() {
        return Collections.unmodifiableMap(overrides);
    }

    public boolean hasOverride(String hostname) {
        if (hostname == null) return false;
        return overrides.containsKey(normalize(hostname));
    }

    private String normalize(String hostname) {
        return hostname.trim().toLowerCase();
    }
}
