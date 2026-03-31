package balbucio.browser4j.download.model;

import java.util.Set;

/** Semantic category of a downloaded file, derived from MIME type or extension. */
public enum DownloadCategory {
    IMAGE, VIDEO, DOCUMENT, OTHER;

    private static final Set<String> IMAGE_MIMES  = Set.of("image/jpeg","image/png","image/gif","image/webp","image/svg+xml","image/bmp","image/tiff");
    private static final Set<String> VIDEO_MIMES  = Set.of("video/mp4","video/webm","video/ogg","video/mpeg","video/quicktime","video/x-msvideo","video/x-matroska");
    private static final Set<String> DOC_MIMES    = Set.of("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                            "application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                            "application/vnd.ms-powerpoint","text/plain","text/csv","application/rtf","application/zip",
                                                            "application/x-tar","application/x-zip-compressed");

    private static final Set<String> IMAGE_EXTS   = Set.of("jpg","jpeg","png","gif","webp","svg","bmp","tiff","ico");
    private static final Set<String> VIDEO_EXTS   = Set.of("mp4","webm","mkv","avi","mov","mpeg","mpg","flv","wmv");
    private static final Set<String> DOC_EXTS     = Set.of("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","csv","rtf","zip","tar","gz","7z","rar");

    public static DownloadCategory fromMimeType(String mimeType) {
        if (mimeType == null) return OTHER;
        String m = mimeType.toLowerCase().split(";")[0].trim();
        if (IMAGE_MIMES.contains(m)) return IMAGE;
        if (VIDEO_MIMES.contains(m)) return VIDEO;
        if (DOC_MIMES.contains(m))   return DOCUMENT;
        return OTHER;
    }

    public static DownloadCategory fromExtension(String fileName) {
        if (fileName == null) return OTHER;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return OTHER;
        String ext = fileName.substring(dot + 1).toLowerCase();
        if (IMAGE_EXTS.contains(ext)) return IMAGE;
        if (VIDEO_EXTS.contains(ext)) return VIDEO;
        if (DOC_EXTS.contains(ext))   return DOCUMENT;
        return OTHER;
    }
}
