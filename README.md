# 🌐 Browser4j
[![](https://img.shields.io/badge/HyperPowered-Use%20the%20official%20repository-yellow?color=%23279BF8&cacheSeconds=3600)](https://maven.dev.hyperpowered.net/#/releases/balbucio/browser4j/)

O **Browser4j** é uma biblioteca Java robusta projetada para embutir um navegador avançado baseado no **Chromium (via JCEF)** em aplicações Swing ou AWT, com foco em controle de rede, performance de cache e gerenciamento de perfis.

---

## ✨ Principais Funcionalidades

-   🚀 **Advanced Cache**: Sistema multi-camada com SQLite (metadados), SHA-256 (deduplicação) e GZIP (compressão).
-   📂 **Download Manager**: Controle total do ciclo de vida de downloads com camadas de segurança integradas.
-   📜 **History & Autocomplete**: Histórico persistente com busca ultra-rápida via **SQLite FTS5**.
-   ✨ **Browser Automation**: High-level API for clicking, typing, and waiting on elements (Puppeteer-style).
-   🧩 **Modular JS Bridge**: Expose Java modules to JavaScript with automatic proxy generation.
-   🚀 **Built for Performance**: Lightweight and optimized for embedding.
-   👤 **Profile Management**: Perfis totalmente isolados (cookies, localStorage, cache e histórico).
-   📑 **Multi-Tab Support**: Gerenciador de abas flexível para aplicações multi-documento.
-   🛠️ **DevTools & Hooks**: Acesso nativo ao Inspector do Chromium e intercepção de rede completa.

---

## 🚀 Início Rápido

### Depedência (Maven)
```xml
<dependency>
    <groupId>balbucio.browser4j</groupId>
    <artifactId>browser4j</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Exemplo Minimalista
```java
// 1. Inicializa o Runtime global
BrowserRuntime.init(BrowserRuntimeConfiguration.builder().build());

// 2. Cria a instância do navegador
Browser browser = CefBrowserImpl.create(BrowserRuntime.getCefApp());

// 3. Adiciona ao JFrame e navega
frame.add(browser.getView().getUIComponent());
browser.loadURL("https://github.com/balbucio/browser4j");
```

---

## 📚 Documentação Detalhada

Mergulhe nas capacidades do Browser4j consultando nossos guias:

1.  **[Getting Started](get-started.md)** - Guia de configuração inicial.
2.  **[Configuração do Runtime](docs/01-runtime-config.md)** - Feature flags e inicialização.
3.  **[API de Navegação](docs/02-browser-instance.md)** - Comandos básicos e avançados.
4.  **[Intercepção de Rede](docs/03-events-network.md)** - Hooks de requests e cookies.
5. **[Proxy Pool e Controle de Fingerprint](docs/04-proxy-pool.md)** - Proxy Pool e Controle de Fingerprint.
6. **[Gerenciador de Abas](docs/05-tab-manager.md)** - Lógica multi-janela.
7. **[HTML Parsing](docs/06-html-parsing.md)** - Extração de HTML, Parsing com Jsoup e Live-Tracking.
8. **[Perfis e Isolamento](docs/07-profile-manager.md)** - Gerenciamento de usuários.
9. **[Custom Error Handler](docs/08-error-handler.md)** - Tratamento de Erros do Browser
10. **[Gerenciador de Downloads](docs/09-download-manager.md)** - Fluxo de arquivos.
11. **[Histórico e Autocomplete](docs/10-history-manager.md)** - Persistência de navegação.
12. **[Cache Avançado](docs/11-cache-manager.md)** - Otimização de performance e disco.
13. **[Modular JS Bridge](docs/12-js-bridge.md)** - Comunicação estruturada Java-JS.
14. **[Automation API](docs/13-automation-api.md)** - Automação de navegador.

---

## ⚖️ Licença
Distribuído sob a licença MIT e BSD 3-Clause License. Navegue até a pasta `licenses` para mais informações.
