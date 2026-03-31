# Instâncias do Navegador no Browser4j

O gerenciamento principal das abas navegadas e as injecções Swing / UI ocorrem pela interface `balbucio.browser4j.browser.api.Browser` e a implementação `CefBrowserImpl`.

Um único contexto de `BrowserRuntime` pode instânciar diversos `Browser` independentes.

---

### Inicialização e Integração

Lembre-se de primeiro iniciar o [Runtime](01-runtime-config.md) de fundo e depois você estará elegível a instanciar contextos usando a fábrica `CefBrowserImpl.create(CefApp p)`.

```java
import balbucio.browser4j.core.runtime.BrowserRuntime;
import balbucio.browser4j.browser.api.CefBrowserImpl;
import balbucio.browser4j.browser.api.Browser;
import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.Session;

// 1. (Recomendado) Utilize Sessions para manter consistência de Rede e Fingerprint:
// Leia mais na seção [04 Proxy Pool e Fingerprint](04-proxy-pool.md)
//
// Você também pode acionar o MODO ANÔNIMO chamando .createIncognito(), o que gera
// um sandbox isolado no contexto nativo do Chromium para todas as abas desta mesma sessão!
// Session session = Session.createIncognito(meuProfile);

// 2. Opcionalmente configurar isolamentos de perfil do browser instanciado:
BrowserOptions opStatus = BrowserOptions.builder()
        // Substitui o User Agent para apenas esta "aba" - caso necessário em futuras versões
        .userAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)")
        // ou defina a Session completa:
        // .session(session)
        .build();

// 3. Crie a Interface
Browser browser = CefBrowserImpl.create(BrowserRuntime.getCefApp(), opStatus);

// Se nenhuma opção for requerida, pode invocar CefBrowserImpl.create(BrowserRuntime.getCefApp());
```

---

### Obtendo Componente para JFrame / AWT
Após criada a instância, deve-se acoplar a sua interface visual caso não esteja ativado a opção `--osrEnabled=true`. Para acoplar ao Swing:

```java
import javax.swing.JFrame;
import java.awt.Component;

JFrame frame = new JFrame("Minha App Web");
Component meuComponenteNavegador = ((CefBrowserImpl) browser).getView().getUIComponent();

frame.getContentPane().add(meuComponenteNavegador, java.awt.BorderLayout.CENTER);
frame.setVisible(true);
```


### Modo Anônimo (Incognito) e Sessões Isoladas

Sessões Isoladas permitem que o `Browser4j` crie um ambiente de memória temporária que **nunca grava dados, cache ou histórico no disco**, e desativa completamente o acesso aos Cookies Globais. 

1. Se você quer instanciar um navegador privado puro (anônimo):
```java
// Crie a sessão isolada
Session privateSession = Session.createIncognito(meuProfile);

// Atrele à aba
BrowserOptions options = BrowserOptions.builder().session(privateSession).build();
Browser browserPrivado = CefBrowserImpl.create(BrowserRuntime.getCefApp(), options);
```
2. **Memória Compartilhada no Modo Anônimo**: Abas diferentes ou Instâncias de Browser que utilizarem a **mesma** variável `privateSession` compartilharão os Cookies Anônimos e o Cachê Memória viva entre si, da exata mesma forma que navegadores convencionais tratam "Múltiplas janelas anônimas". Ao fechar todos os navegadores daquela sessão, o loop de limpeza do JCEF destroí a memória limpa.

---

### Carregando HTML Diretamente

Além de navegar para URLs, o Browser4j suporta carregar conteúdo HTML de três formas:

#### Via String (gerado em tempo de execução)
```java
String meuHtml = "<html><body><h1>Olá, Browser4j!</h1></body></html>";
browser.loadHTML(meuHtml);
// Para HTMLs grandes (> 512KB), um arquivo temporário é criado automaticamente
// para não estourar o limite de tamanho de uma data: URL.
```

#### Via InputStream (de recurso, classpath, socket...)
```java
// De um arquivo no classpath
try (InputStream is = getClass().getResourceAsStream("/templates/pagina.html")) {
    browser.loadHTML(is); // o stream é consumido e fechado internamente
}

// De qualquer outro InputStream (HTTP, banco, gerador...)
browser.loadHTML(minhaResposta.getBody());
```

#### Via arquivo local (File / Path)
```java
import java.io.File;
import java.nio.file.Path;

// Por File
browser.loadFile(new File("relatorio.html"));

// Por Path (sobrecarga padrão da interface)
browser.loadFile(Path.of("templates", "pagina.html"));
```

> [!TIP]
> O método `loadFile()` usa o protocolo `file://` nativo do Chromium, o que mantém o contexto correto para recursos relativos como imagens, CSS e JS locais referenciados no HTML.

---

### Comandos de Controle (Ações de Navegação e DOM)

A gerência e comunicação com a Engine se dá utilizando métodos declarados:

#### Comandos Básicos
```java
browser.loadURL("https://github.com");
browser.reload();

// Navegação entre histórico (após acionar páginas)
if (podeVoltar) {
    browser.goBack();
}
browser.goForward();
```

#### Comandos de Entrada (Interação Manual / Sem Mouse)
Se você estiver utilizando a modalidade robótica/scraper pode enviar entrada virtualmente pelas coordenadas do Frame renderizado via `InputController`:

```java
balbucio.browser4j.browser.input.InputController controller = browser.getInputController();

// Click em coordenadas absolutas
controller.click(150, 200); 

// Inserir texto cru ou enviar tecla tab via keyCode Java.awt.event.KeyEvent 
controller.type("Minha Query de busca...");
```

---

#### Console de Desenvolvedor Automático (Mensagens Web)
O Browser4j pode resgatar requisições feitas na linha de depuração do F12 (DevTools > `console.log(...)`) utilizando o callback global atrelado aquela instância:

```java
browser.onConsoleMessage((String logMensagem) -> {
    System.out.println("LOG JAVASCRIPT: " + logMensagem);
});
```

---

### Perfis Persistentes

O `ProfileManager` permite criar **contextos de usuário nomeados** salvos no disco — com preferências de tema, idioma, fuso horário e zoom que sobrevivem entre execuções da JVM.

> [!IMPORTANT]
> `activateProfile()` **deve ser chamado antes de `BrowserRuntime.init()`**, pois define o diretório de cache que o Chromium usará desde o início.

```java
import balbucio.browser4j.browser.profile.ProfileManager;
import balbucio.browser4j.browser.profile.ProfilePreferences;
import balbucio.browser4j.browser.profile.ProfileEntry;
import balbucio.browser4j.security.profile.BrowserProfile;

// 1. Ativar ANTES do init()
ProfileManager.get().activateProfile("perfil-joao");
BrowserRuntime.init(BrowserRuntimeConfiguration.builder().build());

// 2. Registrar preferências (só necessário na primeira vez)
ProfilePreferences prefs = ProfilePreferences.builder()
    .language("pt-BR")
    .timezone("America/Sao_Paulo")
    .theme(ProfilePreferences.Theme.DARK)
    .zoomLevel(1.1)
    .build();
ProfileEntry perfil = ProfileManager.get().register("perfil-joao", "João", prefs);

// 3. Criar sessão ligada ao perfil
BrowserProfile profile = BrowserProfile.builder().profileEntry(perfil).build();
Session sessao = Session.create(profile);
Browser browser = CefBrowserImpl.create(BrowserRuntime.getCefApp(),
        BrowserOptions.builder().session(sessao).build());
```

Consulte o guia completo: [Gerenciamento de Perfis Persistentes →](07-profile-manager.md)

---

[Próximo: Lidando com Eventos, API de Rede, Cookies e Segurança →](03-events-network.md)
