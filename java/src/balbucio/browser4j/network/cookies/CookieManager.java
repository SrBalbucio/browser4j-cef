package balbucio.browser4j.network.cookies;

import org.cef.network.CefCookieManager;
import org.cef.network.CefCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CookieManager {
    private final CefCookieManager cefManager;

    public CookieManager(CefCookieManager cefManager) {
        this.cefManager = cefManager;
    }

    public boolean set(String url, CefCookie cookie) {
        return cefManager.setCookie(url, cookie);
    }

    public CompletableFuture<List<CefCookie>> getAll() {
        CompletableFuture<List<CefCookie>> future = new CompletableFuture<>();
        List<CefCookie> cookies = new ArrayList<>();
        
        boolean ok = cefManager.visitAllCookies((cookie, count, total, deleteObject) -> {
            cookies.add(cookie);
            if (count + 1 == total) {
                future.complete(cookies);
            }
            return true;
        });
        
        if (!ok) {
            future.complete(cookies);
        }
        return future;
    }

    public CompletableFuture<List<CefCookie>> get(String url) {
        CompletableFuture<List<CefCookie>> future = new CompletableFuture<>();
        List<CefCookie> cookies = new ArrayList<>();
        
        boolean ok = cefManager.visitUrlCookies(url, true, (cookie, count, total, deleteObject) -> {
            cookies.add(cookie);
            if (count + 1 == total) {
                future.complete(cookies);
            }
            return true;
        });

        if (!ok) {
            future.complete(cookies);
        }
        return future;
    }

    public boolean delete(String url, String cookieName) {
        return cefManager.deleteCookies(url, cookieName);
    }

    public boolean clear() {
        return cefManager.deleteCookies("", "");
    }
}
