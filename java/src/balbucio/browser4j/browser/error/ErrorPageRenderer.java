package balbucio.browser4j.browser.error;

/**
 * Generates HTML error pages for Browser4j.
 *
 * <p>First checks if the {@link ErrorPageRegistry} has a custom provider registered
 * for the error's specific CEF code or semantic type, and falls back to the
 * built-in self-contained HTML page if none is found.
 *
 * <p>All built-in pages are fully self-contained — no external CSS, fonts or
 * images — so they render correctly regardless of network state.
 */
public class ErrorPageRenderer {

    private final ErrorPageRegistry registry;

    public ErrorPageRenderer(ErrorPageRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the HTML to display for the given error.
     * Delegates to a custom provider if one is registered, otherwise uses the built-in page.
     */
    public String render(BrowserError error) {
        // 1. Priority: exact CEF code or HTTP status
        ErrorPageProvider byCode = error.isHttpError()
                ? registry.getProviderForCode(error.getHttpStatusCode())
                : registry.getProviderForCode(error.getCefErrorCode());

        if (byCode != null) return byCode.renderPage(error);

        // 2. Semantic type
        ErrorPageProvider byType = registry.getProviderForType(error.getType());
        if (byType != null) return byType.renderPage(error);

        // 3. Built-in default
        return renderBuiltIn(error);
    }

    // ---- Built-in HTML pages ----

    private String renderBuiltIn(BrowserError error) {
        String icon    = iconFor(error.getType());
        String title   = titleFor(error);
        String message = messageFor(error);

        return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<title>" + escHtml(title) + "</title>" +
               "<style>" + BUILT_IN_CSS + "</style></head><body>" +
               "<div class='container'>" +
               "<div class='icon'>" + icon + "</div>" +
               "<h1>" + escHtml(title) + "</h1>" +
               "<p class='message'>" + escHtml(message) + "</p>" +
               "<p class='url'>" + escHtml(error.getUrl()) + "</p>" +
               (error.getCefErrorCode() != 0 || error.getHttpStatusCode() > 0
                    ? "<p class='code'>Código: " + errorCodeLabel(error) + "</p>" : "") +
               "</div></body></html>";
    }

    private String titleFor(BrowserError error) {
        return switch (error.getType()) {
            case DNS_FAILURE        -> "Endereço não encontrado";
            case NO_CONNECTION      -> "Sem conexão com a internet";
            case SSL_ERROR          -> "Conexão não segura";
            case TIMEOUT            -> "Página demorando demais";
            case CONNECTION_REFUSED -> "Conexão recusada";
            case NOT_FOUND          -> "Página não encontrada";
            case SERVER_ERROR       -> "Erro no servidor";
            default                 -> "Erro ao carregar a página";
        };
    }

    private String messageFor(BrowserError error) {
        return switch (error.getType()) {
            case DNS_FAILURE        -> "Não foi possível localizar o servidor. Verifique o endereço ou sua conexão com a internet.";
            case NO_CONNECTION      -> "Você está offline. Verifique seu Wi-Fi ou cabo de rede e tente novamente.";
            case SSL_ERROR          -> "O certificado de segurança do servidor apresentou um problema. A conexão foi bloqueada para proteger seus dados.";
            case TIMEOUT            -> "O servidor está demorando muito para responder. Tente novamente ou acesse o site mais tarde.";
            case CONNECTION_REFUSED -> "O servidor recusou ativamente a conexão. O serviço pode estar fora do ar.";
            case NOT_FOUND          -> "A página que você procura não existe ou foi removida.";
            case SERVER_ERROR       -> "O servidor encontrou um problema interno e não pôde completar a requisição.";
            default                 -> (error.getErrorText() != null && !error.getErrorText().isBlank())
                                        ? error.getErrorText()
                                        : "Ocorreu um erro inesperado ao tentar carregar esta página.";
        };
    }

    private String iconFor(BrowserErrorType type) {
        return switch (type) {
            case DNS_FAILURE, NO_CONNECTION -> "🌐";
            case SSL_ERROR                  -> "🔒";
            case TIMEOUT                    -> "⏱️";
            case CONNECTION_REFUSED         -> "🚫";
            case NOT_FOUND                  -> "📄";
            case SERVER_ERROR               -> "🖥️";
            default                         -> "⚠️";
        };
    }

    private String errorCodeLabel(BrowserError error) {
        if (error.isHttpError()) return "HTTP " + error.getHttpStatusCode();
        return "CEF " + error.getCefErrorCode();
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ---- Built-in CSS (self-contained, no external deps) ----

    private static final String BUILT_IN_CSS =
        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
        "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;" +
        "       background: #0f1117; color: #c9d1d9; display: flex;" +
        "       align-items: center; justify-content: center; min-height: 100vh; padding: 2rem; }" +
        ".container { max-width: 560px; text-align: center; }" +
        ".icon { font-size: 4rem; margin-bottom: 1.5rem; }" +
        "h1 { font-size: 1.6rem; font-weight: 600; color: #f0f6fc; margin-bottom: 0.75rem; }" +
        ".message { font-size: 0.95rem; line-height: 1.6; color: #8b949e; margin-bottom: 1.25rem; }" +
        ".url { font-size: 0.78rem; color: #484f58; word-break: break-all;" +
        "       background: #161b22; padding: 0.4rem 0.75rem; border-radius: 6px;" +
        "       border: 1px solid #30363d; margin-bottom: 0.75rem; }" +
        ".code { font-size: 0.75rem; color: #484f58; }";
}
