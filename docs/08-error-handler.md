# Tratamento de Erros do Browser (Error Handler)

O Browser4j intercepta automaticamente erros de navegação — falhas de rede (DNS, SSL, timeout), erros de protocolo CEF e respostas HTTP de erro (404, 500, etc.) — e exibe **páginas de erro integradas** elegantes no lugar de uma tela em branco.

Todo o sistema é customizável: você pode substituir qualquer página de erro por HTML próprio, reagir programaticamente via evento, ou ambos ao mesmo tempo.

---

## Como funciona

Quando um erro ocorre, o fluxo interno é sempre o mesmo:

```
erro de rede (CEF)  ──┐
                        ├──► classifica tipo ──► busca provider no registry
erro HTTP 4xx/5xx   ──┘                              │           │
                                            provider custom    sem mapping
                                                     │              │
                                               seu HTML       built-in HTML
                                                     └────────────►┘
                                                                   │
                                         onBrowserError(event) ◄──┤
                                                                   │
                                                           loadHTML(página)
```

> [!NOTE]
> **Erros HTTP interceptados por padrão:** 404, 410, 500, 502, 503, 504.
> Demais status (400, 401, 429...) passam normalmente para o `onLoadEnd`, pois são frequentemente respostas intencionais de APIs.

---

## Tipos de Erro (`BrowserErrorType`)

| Tipo | Quando ocorre |
|---|---|
| `DNS_FAILURE` | Host não encontrado (`ERR_NAME_NOT_RESOLVED`) |
| `NO_CONNECTION` | Sem internet (`ERR_INTERNET_DISCONNECTED`) |
| `SSL_ERROR` | Certificado inválido, expirado ou protocolo SSL com falha |
| `TIMEOUT` | Conexão ou resposta demorou demais |
| `CONNECTION_REFUSED` | Servidor recusou ativamente a conexão |
| `NOT_FOUND` | HTTP 404 ou 410 |
| `SERVER_ERROR` | HTTP 500, 502, 503, 504 |
| `UNKNOWN` | Qualquer outro código CEF não mapeado |

---

## Reagindo a erros via evento

Adicione um `BrowserEventListener` com `onBrowserError` para ser notificado de todos os erros interceptados, **sem substituir** a página de erro padrão:

```java
import balbucio.browser4j.browser.error.BrowserError;
import balbucio.browser4j.browser.events.BrowserEventListener;

browser.addEventListener(new BrowserEventListener() {

    @Override
    public void onBrowserError(BrowserError error) {
        System.err.printf("[BROWSER ERROR] %s → %s (tipo: %s)%n",
            error.getUrl(), error.getErrorText(), error.getType());
    }

    // métodos obrigatórios...
    @Override public void onLoadStart(String url) {}
    @Override public void onLoadEnd(String url, int code) {}
    @Override public void onLoadError(String url, int code, String text) {}
    @Override public void onNavigation(String url) {}
    @Override public void onTitleChange(String title) {}
});
```

O objeto `BrowserError` expõe:

```java
error.getUrl()            // URL que falhou
error.getType()           // BrowserErrorType semântico
error.getErrorText()      // Descrição legível do erro
error.getCefErrorCode()   // Código CEF nativo (negativo), ou 0 se for HTTP
error.getHttpStatusCode() // HTTP status (ex: 404), ou 0 se for erro de rede
error.isHttpError()       // true se o erro veio de uma resposta HTTP
```

---

## Páginas de erro customizadas

Use `browser.errors()` para registrar provedores de HTML por **tipo semântico** ou por **código específico**:

### Por tipo de erro

```java
import balbucio.browser4j.browser.error.BrowserErrorType;

// Substitui a página de erro para TODA a família SSL
browser.errors().onError(BrowserErrorType.SSL_ERROR, error ->
    """
    <html>
      <body style="font-family:sans-serif;padding:2rem">
        <h1>🔒 Certificado inválido</h1>
        <p>Não foi possível verificar o certificado de: <b>%s</b></p>
      </body>
    </html>
    """.formatted(error.getUrl())
);

// Substitui a página para erros sem conexão
browser.errors().onError(BrowserErrorType.NO_CONNECTION, error ->
    "<html><body><h1>Você está offline</h1></body></html>"
);
```

### Por código específico (prioridade máxima)

```java
// Código HTTP específico
browser.errors().onError(404, error ->
    myApp.renderTemplate("error-404.html", error.getUrl())
);

// Código CEF específico (ERR_NAME_NOT_RESOLVED = -105)
browser.errors().onError(-105, error ->
    "<html><body><h1>DNS falhou para: " + error.getUrl() + "</h1></body></html>"
);
```

> [!TIP]
> Mapeamentos **por código** têm sempre prioridade sobre mapeamentos **por tipo**. Útil quando você quer tratar um código específico de forma diferente de toda a família.

### Removendo mapeamentos

```java
// Remove apenas o mapeamento de 404 (volta para built-in)
browser.errors().clearMapping(404);

// Remove apenas o mapeamento de SSL_ERROR
browser.errors().clearMapping(BrowserErrorType.SSL_ERROR);

// Remove todos os mapeamentos de uma vez
browser.errors().clearMappings();
```

> [!NOTE]
> Os métodos de `.errors()` são encadeáveis (`fluent`):
> ```java
> browser.errors()
>     .onError(BrowserErrorType.TIMEOUT, e -> "<html>...</html>")
>     .onError(BrowserErrorType.DNS_FAILURE, e -> "<html>...</html>")
>     .onError(404, e -> "<html>...</html>");
> ```

---

## Interface `ErrorPageProvider`

Para providers mais complexos (templates externos, i18n, etc.), implemente diretamente a interface:

```java
import balbucio.browser4j.browser.error.ErrorPageProvider;
import balbucio.browser4j.browser.error.BrowserError;

public class MinhasPaginasDeErro implements ErrorPageProvider {
    @Override
    public String renderPage(BrowserError error) {
        return switch (error.getType()) {
            case NOT_FOUND   -> carregarTemplate("404.html", error.getUrl());
            case SSL_ERROR   -> carregarTemplate("ssl-error.html", error.getUrl());
            default          -> carregarTemplate("erro-generico.html", error.getErrorText());
        };
    }
}

// Registrar o provider para todos os tipos desejados:
ErrorPageProvider provider = new MinhasPaginasDeErro();
browser.errors()
    .onError(BrowserErrorType.NOT_FOUND, provider)
    .onError(BrowserErrorType.SSL_ERROR, provider);
```

---

## Páginas built-in

Quando nenhum provider customizado é encontrado, o Browser4j exibe páginas built-in com design escuro e autocontidas (sem dependências externas):

| Tipo de erro | Visual |
|---|---|
| DNS / Sem internet | 🌐 ícone de rede + mensagem orientativa |
| SSL | 🔒 cadeado + aviso de certificado |
| Timeout | ⏱️ relógio + sugestão de retry |
| Conexão recusada | 🚫 + mensagem de serviço offline |
| 404 / Não encontrado | 📄 + URL tentada |
| Erro de servidor (5xx) | 🖥️ + código HTTP |
| Erro genérico | ⚠️ + código CEF + texto bruto |

Todas as páginas built-in exibem o código de erro (HTTP ou CEF) no rodapé.

---

[← Voltar: Proxy Pool e Fingerprint](04-proxy-pool.md) | [Gerenciamento de Abas →](05-tab-manager.md)
