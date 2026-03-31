package balbucio.browser4j.browser.error;

/**
 * Functional interface for custom error page providers.
 *
 * <p>Implementations receive a {@link BrowserError} and must return a complete
 * HTML document string to be displayed in the browser in place of the default
 * error page.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * browser.errors().onError(BrowserErrorType.NOT_FOUND, error ->
 *     "<html><body><h1>404 – Não encontrado</h1><p>" + error.getUrl() + "</p></body></html>"
 * );
 * }</pre>
 */
@FunctionalInterface
public interface ErrorPageProvider {
    /**
     * Renders an HTML page for the given browser error.
     *
     * @param error full context about what went wrong
     * @return a complete HTML document to display; must not be null or empty
     */
    String renderPage(BrowserError error);
}
