# Gerenciamento de Múltiplas Abas (Tab Manager)

O Browser4j fornece uma classe utilitária de alto nível chamada `TabManager` para facilitar a construção de aplicações que requerem múltiplas janelas (abas) dividindo componentes gráficos e ciclo de vida isolado.

## Princípios Básicos

O `TabManager` elimina a necessidade de você interagir manualmente com o estado base do JCEF (que responde na thread CefApp) injetando um State Seguro na memória do Java.

- Ele não compartilha interface de usuário por padrão: apenas a aba ativa é renderizada num Frame `java.awt.Container`.
- Garante o descarte correto da memória do Chromium ao invocar `closeTab`.

---

## 1. Criando um Gerenciador Ativo no UI

A forma mais rápida de usar é emprestando um "Container" Swing para o TabManager. Dessa forma ele fará a mágica de alternância e de Resize para você!

```java
import balbucio.browser4j.ui.tab.TabManager;
import balbucio.browser4j.ui.tab.Tab;
import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.Session;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

// Instancie seu JFrame
JFrame window = new JFrame("Browser4j - Tabs");
window.setSize(1024, 768);

// Painel onde o site efetivamente ficará
JPanel webPanel = new JPanel(new BorderLayout());
window.add(webPanel, BorderLayout.CENTER);

// 1. Crie o TabManager atrelando ao painel principal
TabManager tabManager = new TabManager(webPanel);

// 2. Crie abas usando suas sessões desejadas
BrowserOptions optAna = BrowserOptions.builder().session(Session.create()).build();
Tab tab1 = tabManager.createTab(optAna);

// Mandando o navegador navegar
tab1.getBrowser().loadURL("https://google.com");

// 3. Ao criar a primeira aba, o TabManager automaticamente a fará renderizar no webPanel!
window.setVisible(true);
```

---

## 2. Rastreando Estados (Loading, URL, Título) sem delays

Você não precisa pedir para o `Browser.getNative()` descobrir onde está navegando. A classe `Tab` faz isso passivamente.

```java
// Útil para construir sua barra de abas visual:
String titulo = tab1.getState().getTitle();
boolean estaCarregando = tab1.getState().isLoading();
String urlAtual = tab1.getState().getUrl();

// Novos Recursos Avançados
boolean modoAnonimo = tab1.getState().isIncognito();
boolean conteudoIsProtectedByDRM = tab1.getState().isDrmProtected();
```

---

## 3. Alternando e Fechando
Quando o usuário clicar na interface para trocar de aba:

```java
// Alterna visualmente e marca a aba como acessada por último em cache
tabManager.switchTo(tab2.getId());

// Fecha a aba e descarta instâncias JCEF corretamente
tabManager.closeTab(tab1.getId());
```

> [!TIP]
> **Fallback Automático**: Se a aba que está ativamente desenhada na tela for fechada, o `TabManager` tenta buscar a última aba que foi visitada (`tab.getState().getLastAccessedAt()`) e alterna para ela automaticamente para não deixar um fundo cinza no aplicativo! Se não encontrar mais arquivos, o componente fica vazio.

---

## 4. Modo Incognito (Limpeza em Massa)

Como visto na seção inicial, você pode invocar o Tab Manager passando sessões instanciadas por `Session.createIncognito(...)`. Abas com essa propriedade não gravam disco nem cookies e são isoladas nativamente pelo JCEF!

Para descartá-las todas de uma vez de forma segura:

```java
// Isso vai fechar todas as abas marcadas com o .isIncognito() e vai 
// instantaneamente limpar o CefRequestContext temporário da memória de fundo:
tabManager.closeAllIncognitoTabs();
```

---

## 5. Gestor "Headless" (Sem UI atrelado)
Se você não está usando Swing ou quer gerenciar por si próprio onde a aba entra (por exemplo compondo painéis de X/Y de quadros):

```java
TabManager headlessManager = new TabManager(); // Construtor Vazio

Tab minhaAba = headlessManager.createTab(null);
Component ui = minhaAba.getUIComponent();
// Adicione 'ui' manualmente onde desejar.
```

---
[Voltar para Lista de Configurações](01-runtime-config.md)
