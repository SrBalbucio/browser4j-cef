package balbucio.browser4j.browser.media;

import java.util.Objects;

public class MediaResource {
    private final String id;
    private final String tag;
    private final String src;
    private final String mediaType;
    private final String alt;
    private final String poster;
    private final Integer width;
    private final Integer height;
    private final Double duration;
    private final String outerHTML;

    public MediaResource(String id, String tag, String src, String mediaType, String alt,
                         String poster, Integer width, Integer height, Double duration, String outerHTML) {
        this.id = id;
        this.tag = tag;
        this.src = src;
        this.mediaType = mediaType;
        this.alt = alt;
        this.poster = poster;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.outerHTML = outerHTML;
    }

    public String getId() { return id; }
    public String getTag() { return tag; }
    public String getSrc() { return src; }
    public String getMediaType() { return mediaType; }
    public String getAlt() { return alt; }
    public String getPoster() { return poster; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Double getDuration() { return duration; }
    public String getOuterHTML() { return outerHTML; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaResource that = (MediaResource) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MediaResource{" +
                "id='" + id + '\'' +
                ", tag='" + tag + '\'' +
                ", src='" + src + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", alt='" + alt + '\'' +
                ", poster='" + poster + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", duration=" + duration +
                '}';
    }
}
