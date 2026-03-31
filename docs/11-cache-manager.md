# Gerenciamento de Cache Avançado

O Browser4j possui um sistema de cache persistente, inteligente e isolado por perfil, projetado para reduzir o consumo de banda e acelerar o carregamento de páginas repetitivas através da intercepção nativa do Chromium (JCEF).

---

## Como acessar

O gerenciador de cache é acessado diretamente pela instância do `Browser`:

```java
import balbucio.browser4j.cache.api.CacheManager;

CacheManager cm = browser.cache();
```

---

## Arquitetura Multi-Camada

O sistema utiliza três componentes coordenados para garantir eficiência e performance:

1.  **`CacheIndex` (Metadados)**: Utiliza **SQLite** para indexar metadados de cada recurso (URL, hash do conteúdo, tipo MIME, status HTTP, expiração e headers originais).
2.  **`CacheStorage` (Arquivos)**: Armazena o conteúdo físico no disco usando o hash **SHA-256** do conteúdo como nome do arquivo.
    *   **Deduplicação**: Se o mesmo arquivo (ex: uma biblioteca jQuery ou fonte comum) for baixado de URLs diferentes ou em perfis diferentes, ele será armazenado apenas uma vez no disco.
    *   **Compressão GZIP**: Recursos de texto (HTML, JS, CSS, JSON) acima de 5KB são compactados automaticamente, reduzindo o uso de disco em até 70%.
3.  **`CachePolicyEngine` (Inteligência)**: Decide o que deve ser cacheado e por quanto tempo, respeitando headers HTTP (`Cache-Control`, `Expires`) e utilizando heurísticas para recursos sem política definida.

---

## Funcionalidades e Benefícios

### 🚀 Performance e Economia
*   **Cache HIT Instantâneo**: Recursos cacheados são servidos diretamente do disco via `CachedResourceHandler`, ignorando totalmente a latência de rede.
*   **Captura via Proxy**: Quando ocorre um "MISS", o sistema utiliza um proxy de rede interno para baixar e simultaneamente armazenar o recurso, garantindo que a próxima visita seja um "HIT".

### 🔐 Segurança e Privacidade
*   **Isolamento por Perfil**: O diretório de cache é localizado dentro da pasta do perfil (`<profileDir>/cache/`), garantindo isolamento total de dados entre usuários.
*   **Filtros de Sensibilidade**: O motor nunca armazena recursos que contenham headers sensíveis, como `Authorization` ou `Set-Cookie`, para evitar vazamento de sessões.
*   **Exclusão de Binários Gigantes**: Por padrão, o cache ignora fluxos de vídeo/áudio e arquivos multipart para não esgotar o limite de disco rapidamente.

### 📊 Observabilidade
Você pode monitorar a eficiência do sistema em tempo real:

```java
import balbucio.browser4j.cache.model.CacheStats;

CacheStats stats = browser.cache().getStats();
System.out.println("Espaço em disco: " + (stats.getTotalSize() / 1024 / 1024) + " MB");
System.out.println("Taxa de sucesso (Hits): " + stats.getHits());
```

---

## Uso Manual e Gerenciamento

Embora o sistema seja 100% automatizado, a API permite controle manual:

```java
// Limpar todo o cache do perfil atual (limpa SQLite e arquivos)
browser.cache().clearAll();

// Invalida uma entrada específica
browser.cache().invalidate("https://cdn.example.com/logo.png");
```

---

## Configuração

A configuração é definida via `CacheConfig` no momento da criação da instância:

*   `maxCacheSizeBytes`: Limite de armazenamento (ex: 1GB). Ao atingir o limite, o sistema remove itens via algoritmo LRU (Least Recently Used).
*   `enabled`: Feature flag para ligar/desligar todo o módulo de cache.
*   `compressionThresholdBytes`: Tamanho mínimo para disparar a compressão GZIP (Padrão: 5KB).

---

[← Voltar: Gerenciamento de Histórico](10-history-manager.md) | [JS Bridge Modular →](12-js-bridge.md)
