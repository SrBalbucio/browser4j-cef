package balbucio.browser4j.browser.media;

import java.util.List;

public interface MediaListener {
    void onMediaDiscovered(List<MediaResource> newMedia);
}
