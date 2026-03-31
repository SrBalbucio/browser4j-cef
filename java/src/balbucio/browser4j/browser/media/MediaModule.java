package balbucio.browser4j.browser.media;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MediaModule {
    CompletableFuture<List<MediaResource>> listMedia();
    CompletableFuture<List<MediaResource>> scanMedia();

    void addMediaListener(MediaListener listener);
    void removeMediaListener(MediaListener listener);

    CompletableFuture<Path> downloadMedia(String src, Path destination);
    CompletableFuture<Path> downloadMedia(MediaResource media, Path destination);
}
