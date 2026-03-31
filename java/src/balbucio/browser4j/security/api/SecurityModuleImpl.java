package balbucio.browser4j.security.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SecurityModuleImpl implements SecurityModule {
    private final List<String> allowedGlobPatterns = new ArrayList<>();
    private final List<String> blockedGlobPatterns = new ArrayList<>();
    private Predicate<String> popupHandler;

    @Override
    public void allow(String globPattern) {
        allowedGlobPatterns.add(globPattern);
    }

    @Override
    public void block(String globPattern) {
        blockedGlobPatterns.add(globPattern);
    }

    @Override
    public void onPopup(Predicate<String> popupHandler) {
        this.popupHandler = popupHandler;
    }

    public boolean isUrlBlocked(String url) {
        if (url == null) return false;
        
        // Exact block check logic using basic string contains/match for demo purposes
        // Real implementation would convert globs to regex.
        for (String block : blockedGlobPatterns) {
            if (matches(url, block)) {
                
                // Allow exceptions
                for (String allow : allowedGlobPatterns) {
                    if (matches(url, allow)) return false;
                }
                
                return true;
            }
        }
        return false;
    }
    
    public boolean isPopupBlocked(String targetUrl) {
        if (popupHandler != null) {
            return popupHandler.test(targetUrl);
        }
        return false; // Default permit
    }

    private boolean matches(String url, String pattern) {
        if (pattern.equals("*")) return true;
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return url.matches(".*" + regex + ".*");
    }
}
