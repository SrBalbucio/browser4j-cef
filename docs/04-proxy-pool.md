# Proxy Pool e Controle de Fingerprint

Para evitar bloqueios de rastreadores e manter a consistência de navegações simuladas, o Browser4j implementa dois módulos vitais que interagem entre si: **Fingerprint Profile** e o **Proxy Pool**.

> A regra de ouro na arquitetura avançada de navegação é: "Um Profile sempre deve ser associado ao mesmo Proxy (Coerência de Sessão)".

---

## 🔒 1. Fingerprint Profile

A classe `FingerprintProfile` define os traços digitais que serão interceptados ou anexados via injeção JavaScript aos frames recém-criados, permitindo anonimização natural.

### Exemplo Básico:

```java
import balbucio.browser4j.security.profile.FingerprintProfile;
import balbucio.browser4j.security.profile.BrowserProfile;

FingerprintProfile fingerprint = FingerprintProfile.builder()
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .acceptLanguage("en-US,en;q=0.9")
        .timezone("America/New_York")
        .screen(1920, 1080)
        .platform("Win32") // Mocka `navigator.platform`
        .hardwareConcurrency(8) // Mocka threads locais retornadas pelo JS
        .deviceMemory(16) // Quantidade GB mocado
        .build();

// Encapsulando no BrowserProfile
BrowserProfile meuProfile = BrowserProfile.builder()
        .fingerprint(fingerprint)
        // .profileEntry(perfil)  ← opcional: vincule um Perfil Persistente (ver doc 07)
        .build();
```

> [!NOTE]
> Essa inserção será injetada usando a API do JS Engine Native de forma rápida no evento de `onLoadStart`, muito antes do script da página poder analisar.

---

## 🌐 2. Proxy Pool

Um Proxy Pool armazena múltiplos end-points de procuração web. Ele se encarrega de rotear automaticamente cada sessão para um endereço evitando sobrecarga ou quebra de autenticação (Sticky Session).

```java
import balbucio.browser4j.network.proxy.ProxyConfig;
import balbucio.browser4j.network.proxy.pool.ProxyPool;

ProxyPool pool = ProxyPool.create();

// Alimentando o pool
pool.add(ProxyConfig.http("104.28.10.15", 8080));
pool.add(ProxyConfig.http("104.28.18.22", 8080));
pool.add(ProxyConfig.socks5("185.15.22.1", 1080));

// Você não precisa escolher o proxy manualmente, usaremos a classe Session.
```

---

## 🧩 3. Unindo com o `Session`

A classe `Session` é a cola principal do sistema a partir de agora se você quer aplicar Fingerprint + IPs rotativos ou persistentes ao instanciar o browser:

```java
import balbucio.browser4j.browser.api.Session;
import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.CefBrowserImpl;
import balbucio.browser4j.core.runtime.BrowserRuntime;

// 1. O Session adquire automaticamente um Proxy livre do Pool usando Sticky Session
Session sessionDaAna = Session.create(meuProfile, pool);

// 2. Aplica na Options! O Proxy e o User-Agent do Fingerprint são herdados pelo builder local automaticamente.
BrowserOptions options = BrowserOptions.builder()
        .session(sessionDaAna)
        .build();

// 3. Cria o navegador coeso e isolado.
// (O Proxy, o IP e as variáveis JS já nascem idênticas).
Browser anaBrowserContext = CefBrowserImpl.create(BrowserRuntime.getCefApp(), options);
anaBrowserContext.loadURL("https://browserleaks.com/");
```

---

### Tolerância a Falhas

Os proxies do `ProxyPool` tem saúde (*Health*). Toda vez que ocorrer mais de 3 requisições interrompidas seguidas pelo status `FAILED`, o Pool marcará esse Proxy temporariamente e na próxima designação de `assignToSession`, fará o fail-over para um Proxy livre com o estado `ACTIVE`.
