# Eventos, Rede e Segurança Avançada no Browser4j

Esta parte da documentação compreende os métodos arquiteturais de monitoramento (escuta de mudança e estados) e os submódulos da biblioteca: `NetworkModule`, `SecurityModule`, `Cookies`, e `DevTools`. 

---

### Monitorando Eventos de Carregamento 

O Browser4j possui uma classe base customizada para relatar transições do progresso do carregamento. Você deve acoplar classes aderentes a `balbucio.browser4j.browser.events.BrowserEventListener`.

```java
import balbucio.browser4j.browser.events.BrowserEventListener;
import balbucio.browser4j.browser.api.Browser;

browser.addEventListener(new BrowserEventListener() {
    
    @Override
    public void onLoadStart(String str_url) {
        System.out.println("Iniciando a requisição para " + str_url);
    }

    @Override
    public void onLoadEnd(String str_url, int code) {
        System.out.println("Carregamento Finalizado. Status: " + code);
        // Exemplo: 200 (OK), 404 (Not Found)
    }

    @Override
    public void onLoadError(String str_url, int errorCode, String reasonText) {
        System.err.println("Erro Interno de Protocolo ao conectar em " + str_url);
        System.err.println("Razão Explicativa: " + reasonText);
    }

    @Override
    public void onNavigation(String newBarUrl) {
         System.out.println("O Usuário navegou para: " + newBarUrl);
    }

    // Disparado quando o Browser4j classifica e renderiza uma página de erro
    // (DNS, SSL, timeout, 404, 5xx, etc.). Ver: docs/08-error-handler.md
    @Override
    public void onBrowserError(balbucio.browser4j.browser.error.BrowserError error) {
        System.err.println("Erro de navegação: " + error.getType() + " → " + error.getUrl());
    }
});
```

> [!TIP]
> Para **personalizar as páginas de erro** exibidas pelo browser (DNS, SSL, 404, etc.) ou reagir a eles programaticamente, veja o guia dedicado: [Tratamento de Erros →](08-error-handler.md)

### Captura de Quadros (Renderização OSR - Headless / Invisível)
Para aplicações sem tela física, ou "gravadores de ações do browser" você receberá pacotes limpos contendo matrizes ByteBuffer em tempo real na GPU do Frame de exibição. Lembre-se, o construtor Runtime deve ter explicitado `.osrEnabled(true)`.

```java
import balbucio.browser4j.browser.events.FrameCaptureListener;
import balbucio.browser4j.streaming.Frame;

browser.addFrameCaptureListener(new FrameCaptureListener() {
    @Override
    public void onFrame(Frame capturedFrame) {
        int w = capturedFrame.getWidth();
        int h = capturedFrame.getHeight();
        java.nio.ByteBuffer rgba_buffer = capturedFrame.getBuffer();
        
        // Aqui o frame pode ser exportado para .png, desenhado num Canvas HTML5 paralelo
        // ou interpretado com Machine Learning (OpenCV).
    }
});
```

---

### Módulos Integrados e Gerência Persistente

Em tempo de execução sob abas ativas, as abas oferecem os métodos estendidos do projeto do Browser4j. Retorne seus controladores por `browser.network()`, `browser.security()`, `browser.cookies()`, `browser.storage()`, `browser.devtools()`, `browser.errors()` e `browser.downloads()`.


#### `NetworkModule` (Controle de Rede)
Módulo projetado para bloqueio forçado, redirecionamento ou alteração de requisições HTTPS disparadas livremente pelo Javascript ou Tags de Imagem na web. O método base que é utilizado quando **Intercepção Global** foi ligada (veja docs [Runtime](01-runtime-config.md)).

```java
import balbucio.browser4j.network.api.NetworkModule;
import balbucio.browser4j.network.interception.RequestInterceptor;

NetworkModule subrede = browser.network();

// Bloqueia qualquer tentativa do javascript carregar a API do Google Analytics na página:
subrede.addInterceptor(new RequestInterceptor() {
    @Override
    public balbucio.browser4j.network.interception.InterceptResult intercept(balbucio.browser4j.network.interception.Request req) {
        if (req.getUrl().contains("google-analytics.com")) {
            // Cancelar requisição bloqueando sua passagem e retornando ERRO (ABORTED) nativamente 
            return balbucio.browser4j.network.interception.InterceptResult.cancel();
        }
        
        return balbucio.browser4j.network.interception.InterceptResult.continue_();
    }
});
```

#### `FakeDns` (Substituição de DNS para hosts HTTP/HTTPS)
Em cenários de teste ou injeção, o Browser4j agora permite forçar um hostname para outro destino via DNS interno. Use com cuidado: não se aplica a protocolos customizados (`file://`, `mailto://`, `custom://`, etc.).

```java
NetworkModule network = browser.network();
network.addFakeDnsEntry("example.com", "127.0.0.1");
network.addFakeDnsEntry("api.example.com", "internal-service.local:8080");

// Opcional: listar entradas configuradas
network.getFakeDnsEntries().forEach((host,dst) -> System.out.println(host + " -> " + dst));

// Remover ou limpar
network.removeFakeDnsEntry("example.com");
network.clearFakeDnsEntries();
```


---

#### `SecurityModule` (Controle do Comportamento Aberto)
Projetado para restringir as janelas popup (Anúncios do botão de Download das páginas) não autorizadas, Downloads de links diretos de executáveis, e controle estrito.

```java
import balbucio.browser4j.security.api.SecurityModule;

SecurityModule seguranca = browser.security();

// Proibe redirecionamentos sem controle e popups `target="_blank"` em botões maliciosos
seguranca.setBlockPopups(true);

```

> [!TIP]
> Para um controle completo sobre downloads (progresso, pausa, segurança de extensões e histórico), use o **Download Manager**: [Gerenciamento de Downloads →](09-download-manager.md)

---

#### `CookieManager` (Isolamento e Controle de Sessão)
Permite salvar e apagar tokens temporários de sessões, sem utilizar o menu tradicional do Chrome e programaticamente definir se um usuário "está logado" num site pelo JWT ou JessionID.

```java
import balbucio.browser4j.network.cookies.CookieManager;

CookieManager gerenciadorDeCookies = browser.cookies();

// Ler de forma síncrona
gerenciadorDeCookies.getCookies("https://app.sistema.com", (cookiesFound) -> {
    for(var c : cookiesFound) {
        System.out.println("- Chave: " + c.getName() + " | - Valor: " + c.getValue()); 
    }
});

// Limpar cache temporário e tokens forçosamente antes de logar
gerenciadorDeCookies.clearCookies(null); 
```

---

#### `DevTools`
Gatilho visual para a janela autônoma de inspecionar elementos do código HTML:

```java
browser.devtools().open();

// Fecha o painel
// browser.devtools().close();
```

---

#### `StorageModule` (Armazenamento Web Local e Sessão)
Gerencie os dados e chaves salvas em `localStorage` e `sessionStorage` diretamente por Java sob uma integração robusta de Promises e bridge JS assíncrono:

```java
import balbucio.browser4j.storage.api.StorageModule;

StorageModule storage = browser.storage();

// Atribui valores ao cache de sessão em aba síncrona/assíncrona:
storage.getSessionStorage().setItem("chave-secreta", "token-de-acesso");

// Lê valores de forma assíncrona recebendo na mesma execução pelo JS:
storage.getLocalStorage().getItem("tema").thenAccept(temaDefinido -> {
    System.out.println("O tema local do site é: " + temaDefinido);
});

// Limpar conteúdo
// storage.getLocalStorage().clear();
```

---

### Verificação de Proteção DRM (Widevine)

Se o aplicativo requer validação contra gravações de tela pirata ou detecta se o vídeo que está rodando está criptografado (Streaming), o `Browser4J` ativa Hooks e listeners *nativamente no processo* da página pra ter altíssima precisão:

```java
// Pode ser invocado a qualquer momento e retorna uma promessa validando o contexto EME e o DOM.
browser.isDRMProtected().thenAccept(temDRM -> {
    if (temDRM) {
        System.out.println("Atenção! Esta página ativou a extensão DRM (MediaKeys/Encrypted)!");
    }
});
```

Além disso, a sua biblioteca de eventos (`BrowserEventListener`) agora entrega o gatilho assíncrono padrão **`onDRMDetected(String url)`** para te notificar passivamente toda vez que o gatilho explodir!

---

[Próximo: Tratamento de Erros →](08-error-handler.md) | [Gerenciamento de Downloads →](09-download-manager.md) | [Gerenciamento de Histórico →](10-history-manager.md)
