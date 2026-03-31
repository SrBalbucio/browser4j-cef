# Gerenciamento de Histórico e Autocomplete

O Browser4j possui um sistema de histórico avançado que utiliza o motor **SQLite FTS5** para buscas ultra-rápidas por texto completo, suporte a Single Page Applications (SPA) e um serviço de autocomplete inteligente.

---

## Como acessar

O gerenciador de histórico e o serviço de autocomplete são acessados pela instância do `Browser`:

```java
import balbucio.browser4j.history.api.HistoryManager;
import balbucio.browser4j.history.service.AutocompleteService;

HistoryManager hm = browser.history();
AutocompleteService as = browser.autocomplete();
```

---

## Registro Automático

O histórico captura visitas automaticamente nos seguintes momentos:
1.  **Mudança de endereço**: Quando o navegador navega para uma nova URL.
2.  **Mudança de título**: Captura o `<title>` da página assim que ele é resolvido pelo Chromium.
3.  **Navegação SPA**: Detecta mudanças de URL em sites como YouTube, Facebook ou dashboards React/Vue que usam `pushState` ou `popstate`, sem recarregar a página.

> [!IMPORTANT]
> **Privacidade**: O histórico **NÃO** registra visitas realizadas em **Modo Incognito**.

---

## Pesquisa e Autocomplete

A pesquisa utiliza o algoritmo **BM25** combinado com a popularidade (número de visitas) e a recência do acesso.

### Pesquisa no Histórico
```java
List<HistoryEntry> resultados = browser.history().search("google", "meu-perfil", 10);
for (HistoryEntry e : resultados) {
    System.out.println(e.getTitle() + " - " + e.getUrl());
}
```

### Serviço de Autocomplete (Omnibox)
O `AutocompleteService` fornece sugestões prontas para serem exibidas em uma barra de endereços:

```java
List<Suggestion> sugestoes = browser.autocomplete().suggest("git", "meu-perfil", 5);
// Retornará matches do histórico + sugestões diretas de URL (ex: https://github.com)
```

---

## Configurações

### Limite de Entradas
Você pode definir um limite máximo de itens no histórico para evitar que o banco de dados cresça indefinidamente. O padrão é ilimitado.

```java
browser.history().setMaxEntries(10000); // Mantém apenas os 10k mais recentes
```

### Limpeza
```java
browser.history().clear("meu-perfil"); // Apaga todo o histórico do perfil
```

---

## Integração Técnica

### SQLite FTS5
O sistema utiliza tabelas virtuais FTS5 para indexar URLs e títulos. Isso permite queries como:
- Prefixos: `navega*` encontra "navegação", "navegador".
- Ranking: Resultados que combinam termos no título **e** URL sobem no ranking.

### Suporte a SPA
O suporte a SPA é feito via injeção automática de um script JavaScript que "grampeia" os métodos `history.pushState` e `history.replaceState` do navegador, notificando o Java instantaneamente via `JSBridge`.

---

[← Voltar: Gerenciamento de Downloads](09-download-manager.md) | [Gerenciamento de Cache Avançado →](11-cache-manager.md)
