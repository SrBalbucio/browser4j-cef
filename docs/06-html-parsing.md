# Extração de HTML e Parsing com Jsoup

Para fins de scraping e bots, é extremamente útil extrair o conteúdo do DOM nativo sem depender estritamente de execuções JavaScript ponteadas.

O Browser4j fornece uma abstração direta para resgatar o código-fonte da aba atual já parseado utilizando a popular biblioteca **Jsoup**.

## 1. Obtendo o Documento (Snapshot)

A função `getDOM()` retorna uma `CompletableFuture<Document>`. A extração do código nativo passa pelo pipeline do CEF de forma Assíncrona para não travar a UI (Thread principal).

```java
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import balbucio.browser4j.ui.tab.Tab;

// ... após criar sua Tab ...
tab1.getBrowser().getDOM().thenAccept((Document doc) -> {
    System.out.println("Título extraído: " + doc.title());
    
    // Usufruindo do poder do Jsoup
    Elements links = doc.select("a[href]");
    links.forEach(link -> {
         System.out.println("Link encontrado: " + link.attr("href"));
    });
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

## ⚠️ Limitações Importantes: Estado "Snapshot"

> [!WARNING]
> O método `getDOM()` captura o estado do HTML e DOM correspondente **ao exato milissegundo em que foi invocado**.
> 
> Ele age como uma **cópia estática (Snapshot)**. Se a página web continuar mudando após a captura (exemplo: novos elementos adicionados via WebSockets, animações React/Vue alterando as classes, ou requisições AJAX chegando tardiamente), **estas alterações não serão refletidas** no objeto `Document` retornado.
>
> Não é possível fazer Live-Tracking de instâncias baseadas no DOM Jsoup retornado. Você precisará chamar `getDOM()` frequentemente em momentos cruciais da navegação para obter um Snapshot atualizado.

## 2. Observação de mudanças no DOM em tempo real (MutationObserver)

Foi adicionada uma nova API para acompanhar mutações do DOM em tempo real, usando `MutationObserver` injetado no contexto da página:

- `Browser.addDomMutationListener(DomMutationListener listener)`
- `Browser.removeDomMutationListener(DomMutationListener listener)`

A cada mutação válida (`childList`, `attributes`, `characterData`), o Browser4j envia evento `dom_mutation` através da ponte JSBridge para Java.

### Exemplo de uso

```java
import balbucio.browser4j.browser.events.DomMutationEvent;
import balbucio.browser4j.browser.events.DomMutationListener;

browser.addDomMutationListener(new DomMutationListener() {
    @Override
    public void onDomMutation(DomMutationEvent event) {
        System.out.println("DOM mutation: " + event.getType());
        System.out.println("Target: " + event.getTargetTag() + " id=" + event.getTargetId());
        System.out.println("OuterHTML: " + event.getOuterHTML());
        System.out.println("Added elements: " + event.getAddedOuterHTML());
        System.out.println("Removed elements: " + event.getRemovedOuterHTML());
        System.out.println("Attribute: " + event.getAttributeName() + " old=" + event.getOldValue());
    }
});
```

### Quando usar

- Raspagem em páginas altamente dinâmicas (SPA, atualizações em tempo real, data grids)
- Validação de estrutura dinâmica (elementos adicionados/removidos/alterados)
- Triggers de automação reativos a mudanças de DOM

## 4. Metadata da aba (title, icon, descrição, keywords, UI/Tema, PWA, tradução, SEO)

A classe `TabState` agora guarda metadados de site:

- `title`
- `url`
- `icon` (favicon / apple-touch-icon)
- `description` (meta description / og:description)
- `keywords` (meta keywords)
- `themeColor` / `backgroundColor` (`meta[name=theme-color]`, `meta[name=msapplication-TileColor]`)
- `viewport` (`meta[name=viewport]`)
- `manifestUrl` (`link[rel=manifest]`)
- `pwaCapable` (`meta[name=mobile-web-app-capable|apple-mobile-web-app-capable]`)
- `language` (`<html lang>` / `meta[http-equiv=content-language]`)
- `autoTranslationEnabled` (meta de tradução automática detectada)
- `robots` (`meta[name=robots]`)
- `canonical` (`link[rel=canonical]`)

O `Tab` atualiza esses campos automaticamente no evento `onLoadEnd`, usando `browser.getSiteMetadata()`.

### Exemplo de leitura de metadados

```java
Tab active = tabManager.getActiveTab();
if (active != null) {
    TabState state = active.getState();
    System.out.println("Título: " + state.getTitle());
    System.out.println("URL: " + state.getUrl());
    System.out.println("Ícone: " + state.getIcon());
    System.out.println("Descrição: " + state.getDescription());
    System.out.println("Keywords: " + state.getKeywords());
}
```

### Uso via Browser API

O `Browser` agora também tem método `getSiteMetadata()`:

```java
browser.getSiteMetadata().thenAccept(meta -> {
    System.out.println("Title: " + meta.getTitle());
    System.out.println("Icon: " + meta.getIcon());
    System.out.println("Description: " + meta.getDescription());
    System.out.println("Keywords: " + meta.getKeywords());
});
```

### Observações

- O evento é enviado em lote após cada observação de mutation e pode conter várias mudanças por emissão.
- Pode gerar muito tráfego para alterações intensas; use lógica de filtro no listener para evitar sobrecarga.
- Esse mecanismo complementa `getDOM()` e permite use cases de monitoramento contínuo enquanto a página está ativa.

## 3. API de Mídia (imagens vídeo áudio) com detecção de novas mídias

O Browser4j agora expõe o módulo:

- `browser.media().listMedia()` – lista todas as mídias (`img`, `video`, `audio`) presentes na página em execução.
- `browser.media().scanMedia()` – mesma função, re-scaneia o DOM e retorna o conjunto detectado.
- `browser.media().addMediaListener(MediaListener)` – recebe apenas novos recursos de mídia descobertos após alterações do DOM.
- `browser.media().downloadMedia(src, destination)` / `downloadMedia(MediaResource, destination)` – baixa para arquivo local.

### Modelo `MediaResource`

- `id` (composição: tag+src+id+class)
- `tag` (`img` / `video` / `audio`)
- `src`
- `mediaType`
- `alt`, `poster`, `width`, `height`, `duration`, `outerHTML`

### Exemplo de lista e download

```java
import balbucio.browser4j.browser.media.MediaResource;

browser.media().listMedia().thenAccept(mediaList -> {
    for (MediaResource m : mediaList) {
        System.out.println("Encontrado: " + m.getTag() + " - " + m.getSrc());
        if (m.getSrc() != null && m.getSrc().startsWith("http")) {
            java.nio.file.Path destino = java.nio.file.Path.of("/tmp", java.nio.file.Paths.get(new java.net.URI(m.getSrc()).getPath()).getFileName().toString());
            browser.media().downloadMedia(m, destino).thenAccept(path -> {
                System.out.println("Baixado em: " + path);
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }
    }
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

### Exemplo de escuta incremental de novas mídias

```java
browser.media().addMediaListener(new MediaListener() {
    @Override
    public void onMediaDiscovered(List<MediaResource> newMedia) {
        newMedia.forEach(m -> System.out.println("Nova mídia: " + m.getTag() + " -> " + m.getSrc()));
    }
});
```

> Nota: o módulo de mídia usa o `DomMutationListener` já existente para disparar detecção de novos recursos sempre que o DOM muda.

