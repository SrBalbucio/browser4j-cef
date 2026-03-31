package balbucio.browser4j.browser.error;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of custom error page mappings for a single {@code Browser} instance.
 *
 * <p>Supports two levels of mapping (highest priority first):
 * <ol>
 *   <li><b>By code</b> — exact CEF error code (negative int) or HTTP status code.</li>
 *   <li><b>By type</b> — semantic {@link BrowserErrorType} family.</li>
 * </ol>
 *
 * <p>If no mapping is found, {@link ErrorPageRenderer} falls back to the built-in page.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Custom page for all SSL errors
 * browser.errors().onError(BrowserErrorType.SSL_ERROR, error ->
 *         "<html><body><h1>Certificado inválido</h1><p>" + error.getUrl() + "</p></body></html>");
 *
 * // Custom page for HTTP 404 specifically
 * browser.errors().onError(404, error ->
 *         "<html><body><h1>Não encontrado</h1></body></html>");
 *
 * // Remove all custom mappings
 * browser.errors().clearMappings();
 * }</pre>
 */
public class ErrorPageRegistry {

    // Exact code → provider (CEF codes are negative, HTTP codes are positive)
    private final Map<Integer, ErrorPageProvider> byCode = new ConcurrentHashMap<>();

    // Semantic type → provider
    private final Map<BrowserErrorType, ErrorPageProvider> byType = new ConcurrentHashMap<>();

    /**
     * Registers a custom error page for a specific HTTP status code or CEF error code.
     * Takes priority over type-based mappings.
     *
     * @param code     HTTP status (e.g. 404) or CEF error code (e.g. -105)
     * @param provider the page provider lambda
     */
    public ErrorPageRegistry onError(int code, ErrorPageProvider provider) {
        byCode.put(code, provider);
        return this;
    }

    /**
     * Registers a custom error page for a semantic error type.
     *
     * @param type     the error family to handle
     * @param provider the page provider lambda
     */
    public ErrorPageRegistry onError(BrowserErrorType type, ErrorPageProvider provider) {
        byType.put(type, provider);
        return this;
    }

    /**
     * Removes the custom mapping for a specific code.
     * The built-in page (or a type-based mapping) will be used instead.
     */
    public ErrorPageRegistry clearMapping(int code) {
        byCode.remove(code);
        return this;
    }

    /**
     * Removes the custom mapping for a semantic error type.
     */
    public ErrorPageRegistry clearMapping(BrowserErrorType type) {
        byType.remove(type);
        return this;
    }

    /** Removes all custom mappings, restoring built-in pages for all errors. */
    public ErrorPageRegistry clearMappings() {
        byCode.clear();
        byType.clear();
        return this;
    }

    // ---- Internal API used by ErrorPageRenderer ----

    /** Returns the provider mapped to this exact code, or null. */
    public ErrorPageProvider getProviderForCode(int code) {
        return byCode.get(code);
    }

    /** Returns the provider mapped to this semantic type, or null. */
    public ErrorPageProvider getProviderForType(BrowserErrorType type) {
        return byType.get(type);
    }
}
