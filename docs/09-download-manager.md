# Gerenciamento de Downloads

O Browser4j possui um sistema robusto e seguro para gerenciar downloads de arquivos disparados pelo navegador. Ele oferece controle total sobre o ciclo de vida do download, persistência de histórico por perfil, limitação de concorrência e camadas de segurança integradas.

---

## Como acessar

O gerenciador de downloads é acessado diretamente de qualquer instância do `Browser`:

```java
import balbucio.browser4j.download.api.DownloadManager;

DownloadManager dm = browser.downloads();
```

---

## Monitorando Downloads

Para acompanhar o progresso ou reagir a finalizações, adicione um `DownloadEventListener`. Todos os métodos são opcionais (default).

```java
import balbucio.browser4j.download.events.DownloadEventListener;
import balbucio.browser4j.download.model.DownloadTask;

browser.downloads().addEventListener(new DownloadEventListener() {
    @Override
    public void onDownloadStart(DownloadTask task) {
        System.out.println("Iniciando: " + task.getFileName());
    }

    @Override
    public void onDownloadProgress(DownloadTask task) {
        System.out.printf("Progresso [%s]: %d%%%n", 
            task.getFileName(), task.getProgressPercent());
    }

    @Override
    public void onDownloadComplete(DownloadTask task) {
        System.out.println("Download concluído! Salvo em: " + task.getFullPath());
    }

    @Override
    public void onDownloadError(DownloadTask task, String reason) {
        System.err.println("Falha no download: " + reason);
    }
});
```

### Eventos disponíveis:
- `onDownloadQueued`: Quando o download entra na fila (aguardando slot de concorrência).
- `onDownloadStart`: Quando o download começa a receber bytes.
- `onDownloadProgress`: Atualizações periódicas de progresso.
- `onDownloadComplete`: Sucesso total.
- `onDownloadError`: Falha (rede, disco, etc).
- `onDownloadCanceled`: Cancelado manualmente.
- `onDownloadPaused` / `onDownloadResumed`: Controle de pausa.
- `onDownloadBlocked`: Quando a segurança bloqueia o arquivo (extensão, domínio, etc).

---

## Controle do Ciclo de Vida

Você pode pausar, retomar ou cancelar downloads ativos usando o `downloadId` (UUID) presente na `DownloadTask`.

```java
String id = task.getDownloadId();

browser.downloads().pause(id);   // Pausa
browser.downloads().resume(id);  // Retoma
browser.downloads().cancel(id);  // Cancela
```

### Resiliência e Retry
Se um download falhar ou for cancelado, você pode tentar baixá-lo novamente:

```java
browser.downloads().retry(id);
```
*Nota: O retry reinicia a tentativa de navegação para a URL original.*

---

## Modelo de Dados (`DownloadTask`)

Cada download é representado por um snapshot imutável contendo:

- `getDownloadId()`: ID único (UUID).
- `getUrl()`: URL de origem.
- `getFileName()`: Nome do arquivo (sanitizado).
- `getFullPath()`: Caminho absoluto no disco.
- `getMimeType()`: Tipo de mídia.
- `getTotalBytes()` / `getReceivedBytes()`: Tamanho.
- `getProgressPercent()`: Porcentagem de 0 a 100.
- `getStatus()`: Estado atual (`QUEUED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELED`).
- `getCategory()`: Categoria (`IMAGE`, `VIDEO`, `DOCUMENT`, `OTHER`).
- `getProfileId()`: ID do perfil dono do download.
- `getCreatedAt()` / `getUpdatedAt()`: Timestamps.

---

## Histórico e Persistência

Os downloads são persistidos automaticamente em um banco de dados `history.db` dentro do diretório do perfil correspondente.

- **Isolamento**: Downloads de um perfil nunca são visíveis para outro.
- **Recuperação**: O histórico é carregado ao iniciar o browser.

```java
// Listar histórico de um perfil (ativos e passados)
List<DownloadTask> historico = browser.downloads().list("meu-perfil");

// Limpar histórico do disco (não afeta downloads em curso)
browser.downloads().clearHistory("meu-perfil");
```

---

## Segurança Integrada

O Browser4j aplica várias camadas de proteção automaticamente:

1.  **Sanitização de Nome**: Remove caracteres perigosos e evita *Path Traversal* (`../`).
2.  **Renomeação Automática**: Se um arquivo já existir, ele será salvo como `arquivo (1).ext`.
3.  **Bloqueio de Extensões**: Por padrão, bloqueia arquivos executáveis e scripts (`.exe`, `.bat`, `.ps1`, `.sh`, etc).
4.  **Blacklist de Domínios**: Permite impedir downloads de domínios específicos.
5.  **Limite de Tamanho**: Permite definir um tamanho máximo global para arquivos.

---

## Configuração Avançada

A configuração é feita via `DownloadConfig`. Atualmente, a instância padrão do `CefBrowserImpl` usa configurações recomendadas:

- **Concorrência**: Limite de 3 downloads simultâneos (os demais ficam em `QUEUED`).
- **Organização**: Pode organizar automaticamente por pastas (`images/`, `videos/`, etc).

> [!TIP]
> Em instâncias vinculadas a **Perfis Persistentes**, o diretório padrão de download é automaticamente alterado para `<profileDir>/downloads/`.

---

[← Voltar: Tratamento de Erros](08-error-handler.md) | [Gerenciamento de Abas →](05-tab-manager.md)
