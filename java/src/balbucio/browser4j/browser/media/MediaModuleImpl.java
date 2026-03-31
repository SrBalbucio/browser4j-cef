package balbucio.browser4j.browser.media;

import balbucio.browser4j.browser.api.Browser;
import balbucio.browser4j.bridge.messaging.JSBridge;
import balbucio.browser4j.download.api.DownloadManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MediaModuleImpl implements MediaModule {
    private final Browser browser;
    private final JSBridge bridge;
    private final DownloadManager downloadManager;
    private final Set<String> knownIds = ConcurrentHashMap.newKeySet();
    private final List<MediaListener> listeners = new ArrayList<>();

    public MediaModuleImpl(Browser browser, JSBridge bridge, DownloadManager downloadManager) {
        this.browser = browser;
        this.bridge = bridge;
        this.downloadManager = downloadManager;

        this.browser.addDomMutationListener(event -> scanMedia().thenAccept(this::fireNewMedia));
    }

    @Override
    public CompletableFuture<List<MediaResource>> listMedia() {
        return scanMedia();
    }

    @Override
    public CompletableFuture<List<MediaResource>> scanMedia() {
        String js = "(function() {" +
                "  const nodes = Array.from(document.querySelectorAll('img,video,audio'));" +
                "  return nodes.map(function(el){" +
                "    let src = el.src || '';" +
                "    if (!src && (el.tagName.toLowerCase()==='video'||el.tagName.toLowerCase()==='audio')) {" +
                "      const fallback = el.querySelector('source[src]');" +
                "      if (fallback) src = fallback.src || fallback.getAttribute('src') || '';" +
                "    }" +
                "    return {" +
                "      id: (el.tagName.toLowerCase()+'|'+(src||'')+'|'+(el.id||'')+'|'+(el.className||''))," +
                "      tag: el.tagName.toLowerCase()," +
                "      src: src || null," +
                "      mediaType: el.tagName.toLowerCase()," +
                "      alt: el.alt || null," +
                "      poster: el.poster || null," +
                "      width: el.width || null," +
                "      height: el.height || null," +
                "      duration: el.duration || null," +
                "      outerHTML: el.outerHTML || null" +
                "    };" +
                "  }).filter(i => i.src != null && i.src !== '');" +
                "})()";

        return bridge.evaluateJavaScript(js).thenApply(result -> {
            List<MediaResource> mediaResources = new ArrayList<>();
            if (result instanceof List) {
                for (Object item : (List<?>) result) {
                    if (item instanceof Map) {
                        MediaResource mr = mapToMediaResource((Map<?, ?>) item);
                        if (mr != null) mediaResources.add(mr);
                    }
                }
            }
            return mediaResources;
        });
    }

    @Override
    public synchronized void addMediaListener(MediaListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void removeMediaListener(MediaListener listener) {
        listeners.remove(listener);
    }

    @Override
    public CompletableFuture<Path> downloadMedia(String src, Path destination) {
        if (src == null || src.isBlank()) {
            CompletableFuture<Path> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("src cannot be null or empty"));
            return f;
        }

        if (src.startsWith("data:")) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String[] parts = src.split(",", 2);
                    if (parts.length != 2) throw new IllegalArgumentException("Invalid data URI");
                    String metadata = parts[0];
                    String payload = parts[1];
                    byte[] content;
                    if (metadata.contains("base64")) {
                        content = java.util.Base64.getDecoder().decode(payload);
                    } else {
                        content = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }
                    Files.createDirectories(destination.getParent());
                    Files.write(destination, content);
                    return destination;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write data URI media resource", e);
                }
            });
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(30))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(src))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("Failed to download media resource, status=" + response.statusCode());
                }

                Files.createDirectories(destination.getParent());
                Files.write(destination, response.body());
                return destination;
            } catch (Exception e) {
                throw new RuntimeException("Failed to download media resource: " + src, e);
            }
        });
    }

    @Override
    public CompletableFuture<Path> downloadMedia(MediaResource media, Path destination) {
        if (media == null) {
            CompletableFuture<Path> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("media cannot be null"));
            return f;
        }
        return downloadMedia(media.getSrc(), destination);
    }

    private MediaResource mapToMediaResource(Map<?, ?> map) {
        Object id = map.get("id");
        Object tag = map.get("tag");
        Object src = map.get("src");
        if (id == null || tag == null || src == null) {
            return null;
        }

        String idStr = String.valueOf(id);
        String tagStr = String.valueOf(tag);
        String srcStr = String.valueOf(src);

        String mediaType = map.containsKey("mediaType") && map.get("mediaType") != null
                ? map.get("mediaType").toString() : tagStr;
        String alt = map.containsKey("alt") && map.get("alt") != null
                ? map.get("alt").toString() : null;
        String poster = map.containsKey("poster") && map.get("poster") != null
                ? map.get("poster").toString() : null;

        Integer width = null;
        if (map.get("width") instanceof Number) {
            width = ((Number) map.get("width")).intValue();
        }

        Integer height = null;
        if (map.get("height") instanceof Number) {
            height = ((Number) map.get("height")).intValue();
        }

        Double duration = null;
        if (map.get("duration") instanceof Number) {
            duration = ((Number) map.get("duration")).doubleValue();
        }

        String outerHTML = map.getOrDefault("outerHTML", null) == null ? null : map.get("outerHTML").toString();

        return new MediaResource(idStr, tagStr, srcStr, mediaType, alt, poster, width, height, duration, outerHTML);
    }

    private void fireNewMedia(List<MediaResource> currentMedia) {
        List<MediaResource> newItems = new ArrayList<>();
        for (MediaResource m : currentMedia) {
            if (!knownIds.contains(m.getId())) {
                knownIds.add(m.getId());
                newItems.add(m);
            }
        }

        if (newItems.isEmpty()) {
            return;
        }

        List<MediaListener> listenersCopy;
        synchronized (this) {
            listenersCopy = new ArrayList<>(listeners);
        }
        for (MediaListener listener : listenersCopy) {
            try {
                listener.onMediaDiscovered(newItems);
            } catch (Exception ignored) {
            }
        }
    }
}
