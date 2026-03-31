package balbucio.browser4j.download.security;

import balbucio.browser4j.download.config.DownloadConfig;

import java.net.URI;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Security layer for download validation.
 *
 * <p>Checks performed (in order):
 * <ol>
 *   <li>Blacklisted domain</li>
 *   <li>File size limit</li>
 *   <li>Blocked extension</li>
 *   <li>Path traversal prevention</li>
 *   <li>File name sanitization</li>
 * </ol>
 */
public class DownloadSanitizer {

    private static final Logger LOG = Logger.getLogger(DownloadSanitizer.class.getName());

    private final DownloadConfig config;

    public DownloadSanitizer(DownloadConfig config) {
        this.config = config;
    }

    /**
     * Validates a download request.
     *
     * @param url           the origin URL of the download
     * @param suggestedName the file name suggested by the server
     * @param mimeType      the MIME type reported by the server
     * @param totalBytes    total file size (-1 if unknown)
     * @return a {@link ValidationResult} with allow/block decision and sanitized file name
     */
    public ValidationResult validate(String url, String suggestedName, String mimeType, long totalBytes) {

        // 1. Domain blacklist
        try {
            String host = URI.create(url).getHost();
            if (host != null) {
                for (String blocked : config.getBlacklistDomains()) {
                    if (host.equalsIgnoreCase(blocked) || host.endsWith("." + blocked)) {
                        LOG.warning("[Download] Blocked domain: " + host + " from " + url);
                        return ValidationResult.block("Domain is blacklisted: " + host);
                    }
                }
            }
        } catch (Exception ignored) { /* malformed URL — let it pass to other checks */ }

        // 2. File size limit
        if (config.getMaxFileSizeBytes() > 0 && totalBytes > config.getMaxFileSizeBytes()) {
            LOG.warning("[Download] File too large: " + totalBytes + " bytes from " + url);
            return ValidationResult.block("File exceeds maximum size limit ("
                    + config.getMaxFileSizeBytes() + " bytes)");
        }

        // 3. Extension check
        String sanitizedName = sanitizeFileName(suggestedName);
        String ext = getExtension(sanitizedName).toLowerCase();
        if (config.getBlockedExtensions().contains(ext)) {
            LOG.warning("[Download] Blocked extension: " + ext + " from " + url);
            return ValidationResult.block("Extension is blocked for security reasons: ." + ext);
        }

        // 4. Path traversal guard — already handled by sanitizeFileName, but double-check
        if (sanitizedName.contains("..") || sanitizedName.contains("/") || sanitizedName.contains("\\")) {
            sanitizedName = sanitizedName.replaceAll("[/\\\\.]\\.", "").replaceAll("[/\\\\]", "_");
        }

        LOG.fine("[Download] Passed validation: " + sanitizedName + " from " + url);
        return ValidationResult.allow(sanitizedName);
    }

    /**
     * Resolves the final destination path, ensuring no path traversal and handling
     * duplicate files via auto-renaming ({@code file (1).ext}, {@code file (2).ext}, …).
     */
    public Path resolveDestination(Path downloadDir, String fileName) {
        // Normalize and strip any directory component from the file name
        String name = Path.of(fileName).getFileName().toString();
        name = sanitizeFileName(name);

        Path candidate = downloadDir.resolve(name).normalize();

        // Ensure resolved path is still inside downloadDir (path traversal guard)
        if (!candidate.startsWith(downloadDir.normalize())) {
            candidate = downloadDir.resolve("download_" + System.currentTimeMillis());
        }

        // Auto-rename if file already exists
        if (!candidate.toFile().exists()) return candidate;

        String base = getBaseName(name);
        String ext  = getExtension(name);
        String extPart = ext.isEmpty() ? "" : "." + ext;
        int counter = 1;
        while (true) {
            String newName = base + " (" + counter + ")" + extPart;
            Path next = downloadDir.resolve(newName).normalize();
            if (!next.toFile().exists()) return next;
            counter++;
        }
    }

    // ---- Helpers ----

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "download_" + System.currentTimeMillis();
        // Remove path separators and null bytes
        name = name.replaceAll("[\\x00/\\\\]", "_");
        // Remove control characters
        name = name.replaceAll("[\\p{Cntrl}]", "");
        // Collapse consecutive dots (prevent hidden-file tricks like "....exe")
        name = name.replaceAll("\\.{2,}", ".");
        // Remove leading dots & spaces
        name = name.replaceAll("^[. ]+", "");
        // Trim trailing dots & spaces (Windows compatibility)
        name = name.replaceAll("[. ]+$", "");
        return name.isBlank() ? "download_" + System.currentTimeMillis() : name;
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    private String getBaseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Immutable result of a validation check. */
    public static class ValidationResult {
        private final boolean allowed;
        private final String  sanitizedName;
        private final String  blockReason;

        private ValidationResult(boolean allowed, String sanitizedName, String blockReason) {
            this.allowed       = allowed;
            this.sanitizedName = sanitizedName;
            this.blockReason   = blockReason;
        }

        public static ValidationResult allow(String name)   { return new ValidationResult(true,  name, null); }
        public static ValidationResult block(String reason) { return new ValidationResult(false, null, reason); }

        public boolean isAllowed()        { return allowed;       }
        public String  getSanitizedName() { return sanitizedName; }
        public String  getBlockReason()   { return blockReason;   }
    }
}
