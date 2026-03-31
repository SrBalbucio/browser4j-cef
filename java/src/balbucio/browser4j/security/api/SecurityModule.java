package balbucio.browser4j.security.api;

import java.util.function.Predicate;

public interface SecurityModule {
    void allow(String globPattern);
    void block(String globPattern);
    void onPopup(Predicate<String> popupHandler);
}
