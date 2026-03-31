# Configuração do Runtime no Browser4j

O **BrowserRuntime** é o pilar que gerencia o processo global nativo do Chromium. Ele deve ser inicializado **apenas uma vez** pela sua aplicação durante a execução, e ser desligado (*shutdown*) antes da aplicação finalizar.

## Funcionalidades do Runtime
A inicialização do Runtime ocorre configurando um builder de `BrowserRuntimeConfiguration`. É nesse momento que você define opções globais de pastas, perfis, e flags experimentais OSR *(On-Screen Rendering)*, rede, entre outros.

---

### Exemplo Básico de Configuração

```java
import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;
import balbucio.browser4j.core.runtime.BrowserRuntime;

BrowserRuntimeConfiguration config = BrowserRuntimeConfiguration.builder()
        // Pastas persistentes de estado
        .cachePath("diretorio_de_cache")
        .userDataPath("diretorio_de_dados_usuario") 
        
        // Habilita ou Desabilita cookies estendidos
        .cookiesPersistent(true)
        
        // Opções de aceleração / Segurança
        .enableGPU(true)
        .enableSandbox(false) // Necessário atenção com os limites do JCEF
        
        // OSR - Renderização Headless / Sem Janelas Nativas
        .osrEnabled(false) 
        .windowlessFrameRate(60)

         // Monitoramento e intercepção
        .enableNetworkInterception(true)
        .enableSecurity(true)
        
        // Alteração Global do User-Agent da aplicação
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) MeuNavegador4j")

        // Porta para ferramentas de desenvolvedor remota
        .remoteDebuggingPort(9222)
        .build();

// Inicia o processo do CEFAplication (Chromium Embedded)
BrowserRuntime.init(config);
```

### Principais Opções do Builder:

#### `cachePath` e `userDataPath`
Esses dois atributos controlam onde os dados persistentes (cookies, extensões pre-configuradas, localStorage) do navegador nativo do Chromium ficarão armazenados no disco rígido do sistema. Você pode configurá-los apontando para diretórios ocultos `new File(".minha-app/cache").getAbsolutePath()`.

> [!IMPORTANT]
> Se você estiver usando o **sistema de Perfis Persistentes** (`ProfileManager`), **não defina `cachePath` manualmente** no builder. Chame `ProfileManager.get().activateProfile("meu-perfil")` **antes** de `BrowserRuntime.init()` e o caminho será configurado automaticamente.
> Veja o guia completo em [Gerenciamento de Perfis Persistentes →](07-profile-manager.md)

#### `enableGPU`
Ao ligar a GPU, o Chrome realiza a renderização de vídeo e animações ricas por aceleração de hardware pela placa de vídeo do usuário. Desative apenas para contornar instabilidades.

#### `osrEnabled`
OSR *(Off-Screen Rendering)* significa que o Chromium invés de criar e gerenciar a própria janela de SO subjacente (Win32 API/X11, etc) repassará a renderização de frames brutos (`balbucio.browser4j.streaming.Frame`) como `ByteBuffer` diretamente na mão da biblioteca (isso consome mais CPU na thread do Java, mas permite manipular pixels diretamente). Por padrão, OSR é usado para fazer bots automáticos que não precisam exibir o navegador.

#### `enableNetworkInterception` e `enableSecurity`
Habilitam os submódulos que deixam ser possíveis bloquear downloads, bloquear popups do sistema (`balbucio.browser4j.security.handlers`), ou modificar cabeçalhos do protocolo HTTPS `balbucio.browser4j.network.interception.NetworkHandlerImpl`.

#### `proxy`
(Disponível também globalmente no runtime ou a nível específico por instância)

```java
import balbucio.browser4j.network.proxy.ProxyConfig;
import balbucio.browser4j.network.proxy.ProxyType;

ProxyConfig myProxy = new ProxyConfig("192.168.0.100", 8080, ProxyType.HTTP, "admin", "senha123");

BrowserRuntimeConfiguration.builder()
    .proxy(myProxy)
    ...
```

### Shutdown do Runtime
Certifique-se sempre de invocar `BrowserRuntime.shutdown()` ao final do seu programa para desvincular o Chromium Embedded da memória antes de terminar a aplicação:

```java
BrowserRuntime.shutdown();
```

---
**Próximos Passos:**
* [Criando e gerenciando o Browser →](02-browser-instance.md)
* [Lidando com Eventos, API de Rede, Cookies e Segurança →](03-events-network.md)
* [Proxy Pool e Configuração de Fingerprint/Sessões Consistentes →](04-proxy-pool.md)
* [Gerenciamento de Multiplas Abas →](05-tab-manager.md)
* [Extração de HTML e Parsing Jsoup →](06-html-parsing.md)
* [Gerenciamento de Perfis Persistentes →](07-profile-manager.md)
* [Tratamento de Erros do Browser →](08-error-handler.md)
* [Gerenciamento de Downloads →](09-download-manager.md)
* [Gerenciamento de Histórico e Autocomplete →](10-history-manager.md)
