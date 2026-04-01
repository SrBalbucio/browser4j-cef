# Gerenciamento de Perfis Persistentes

O `ProfileManager` é o sistema de identidade persistente do Browser4j. Ele associa um **nome de perfil** a um diretório
no disco, fazendo o Chromium persistir naturalmente os dados daquele usuário (cookies, localStorage, cache, histórico de
download) entre sessões da JVM.

Em paralelo, o `ProfilePreferences` define as **preferências de usuário** (idioma, tema, zoom, etc.) que são reaplicadas
via API nativa do Chromium em cada carregamento de página.

> **Observações:** Nas versões ``1.0.*`` as preferências não funcionam corretamente, isto deve ser corrigido nas
> próximas versões.

---

## Como funciona internamente

Devido a uma limitação da API Java do JCEF (o `CefRequestContextSettings.cache_path` não é exposto por contexto, apenas
globalmente), a persistência funciona em duas camadas:

| Camada                  | Mecanismo                           | Resultado                                                   |
|-------------------------|-------------------------------------|-------------------------------------------------------------|
| Dados do Chromium       | `CefSettings.cache_path` (global)   | Cookies, localStorage, cache ficam no disco do perfil ativo |
| Preferências de Usuário | `CefRequestContext.setPreference()` | Idioma, zoom, tema aplicados em cada carregamento           |
| Configuração Salva      | SQLite (`profile.db`)               | Preferências sobrevivem reinicializações da JVM             |

---

## Guia de uso passo a passo

### 1. Ativar o perfil antes do Runtime

> [!CAUTION]
> Esta é a etapa mais crítica. `activateProfile()` define o diretório de dados do Chromium **antes** da inicialização.
> Se chamado depois de `BrowserRuntime.init()`, não terá efeito algum no cache.

```java
import balbucio.browser4j.browser.profile.ProfileManager;
import balbucio.browser4j.core.runtime.BrowserRuntime;
import balbucio.browser4j.core.config.BrowserRuntimeConfiguration;

import java.nio.file.Path;

// (Opcional) Definir a pasta base dos perfis (padrão: ~/.browser4j)
ProfileManager.initialize(Path.of(System.getProperty("user.home"), ".minha-app"));

// Ativar o perfil desejado → configura o cache_path antes do Chromium inicializar
ProfileManager.get().activateProfile("perfil-padrao");

// Inicializar normalmente — cachePath será configurado automaticamente
BrowserRuntime.init(BrowserRuntimeConfiguration.builder().build());
```

---

### 2. Registrar preferências

Registrar cria (ou atualiza) a tabela no banco `profile.db` dentro da pasta do perfil. Só é necessário na **primeira execução** ou quando as preferências mudarem.

```java
import balbucio.browser4j.browser.profile.ProfilePreferences;
import balbucio.browser4j.browser.profile.ProfileEntry;

ProfilePreferences prefs = ProfilePreferences.builder()
        .language("pt-BR")                       // BCP-47: en-US, pt-BR, ja-JP...
        .timezone("America/Sao_Paulo")           // IANA Timezone
        .theme(ProfilePreferences.Theme.DARK)    // LIGHT | DARK | SYSTEM
        .zoomLevel(1.1)                          // 0.0 = sem override (padrão 100%)
        // Flags Chromium brutas para uso avançado:
        // .flag("profile.managed_default_content_settings.images", 2)  // bloquear imagens
        .build();

ProfileEntry perfil = ProfileManager.get().register("perfil-padrao", "Perfil Principal", prefs);
```

---

### 3. Criar o Browser associado ao perfil

```java
import balbucio.browser4j.security.profile.BrowserProfile;
import balbucio.browser4j.browser.api.Session;
import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.CefBrowserImpl;

BrowserProfile profile = BrowserProfile.builder()
        .profileEntry(perfil)
        .build();

Session sessao = Session.create(profile);
Browser browser = CefBrowserImpl.create(BrowserRuntime.getCefApp(),
        BrowserOptions.builder().session(sessao).build());

browser.

loadURL("https://exemplo.com");
// As preferências (idioma, tema, zoom) são aplicadas automaticamente após o carregamento
```

---

## Gerenciamento de Múltiplos Perfis

O `ProfileManager` suporta múltiplos perfis nomeados no mesmo diretório base. Cada perfil tem sua pasta própria e `profile.db` individual.

```java
// Na primeira execução: registrar todos os perfis necessários
ProfileManager pm = ProfileManager.get();
pm.

register("perfil-joao","João",ProfilePreferences.builder().

language("pt-BR").

build());
        pm.

register("perfil-maria","Maria",ProfilePreferences.builder().

language("en-US").

build());

// Listar todos os perfis disponíveis
List<ProfileEntry> todos = pm.list();
todos.

forEach(p ->System.out.

println(p.getProfileId() +" → "+p.

getDisplayName()));

// Carregar um perfil específico do disco
Optional<ProfileEntry> joao = pm.load("perfil-joao");

// Atualizar preferências de um perfil existente
pm.

updatePreferences("perfil-joao",ProfilePreferences.builder()
    .

language("pt-BR")
    .

theme(ProfilePreferences.Theme.LIGHT)
    .

build());

// Deletar um perfil (remove a pasta inteira)
        pm.

delete("perfil-antigo");
```

> [!NOTE]
> Apenas **um perfil pode estar ativo por execução da JVM**, pois o `cache_path` do Chromium é global. Para trocar de
> perfil, é necessário chamar `BrowserRuntime.shutdown()`, alterar o perfil ativo, e reinicializar.

---

## `ProfilePreferences` — Referência de Opções

| Método                  | Tipo           | Padrão               | Descrição                               |
|-------------------------|----------------|----------------------|-----------------------------------------|
| `.language(String)`     | BCP-47         | `"en-US"`            | Idioma preferencial (`Accept-Language`) |
| `.timezone(String)`     | IANA           | `"America/New_York"` | Fuso horário (injetado via JS)          |
| `.theme(Theme)`         | Enum           | `SYSTEM`             | Esquema de cores do Chrome              |
| `.zoomLevel(double)`    | double         | `0.0`                | Zoom global. `0.0` = sem override       |
| `.flag(String, Object)` | String, Object | —                    | Preferência Chromium bruta direta       |

---

## Estrutura no disco

Após ativar e registrar um perfil, a estrutura de pastas será:

```
~/.browser4j/
└── profiles/
    └── perfil-padrao/
        ├── profile.db           ← preferências do Browser4j (SQLite)
        ├── Cookies              ← gerenciado pelo Chromium
        ├── Cache/               ← gerenciado pelo Chromium
        ├── Local Storage/       ← gerenciado pelo Chromium
        └── LocalPrefs.json      ← preferências nativas do Chromium
```

---

[← Voltar: Gerenciamento de Abas](05-tab-manager.md) | [Extração de HTML →](06-html-parsing.md)
