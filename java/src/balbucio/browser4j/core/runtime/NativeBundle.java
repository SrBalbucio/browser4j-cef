package balbucio.browser4j.core.runtime;

import org.cef.SystemBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts and loads platform-specific native dependencies embedded as resources
 * under {@code natives/<os>/<arch>/...} in the shaded jar.
 */
final class NativeBundle {
    private static final Logger log = LoggerFactory.getLogger(NativeBundle.class);

    private NativeBundle() {}

    enum Os { WIN, LINUX }
    enum Arch { X64, ARM64 }

    static final class Platform {
        final Os os;
        final Arch arch;
        final String resourcePrefix; // e.g. natives/win/x64
        final String key; // e.g. win-x64

        Platform(Os os, Arch arch) {
            this.os = os;
            this.arch = arch;
            this.resourcePrefix = "natives/" + (os == Os.WIN ? "win" : "linux") + "/" + (arch == Arch.X64 ? "x64" : "arm64");
            this.key = (os == Os.WIN ? "win" : "linux") + "-" + (arch == Arch.X64 ? "x64" : "arm64");
        }
    }

    static Platform detectPlatform() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();

        Os os;
        if (osName.contains("win")) os = Os.WIN;
        else if (osName.contains("linux") || osName.contains("nix")) os = Os.LINUX;
        else throw new UnsupportedOperationException("Unsupported OS: " + osName);

        Arch arch;
        if (osArch.contains("aarch64") || osArch.contains("arm64")) arch = Arch.ARM64;
        else if (osArch.contains("x86_64") || osArch.contains("amd64") || osArch.contains("64")) arch = Arch.X64;
        else throw new UnsupportedOperationException("Unsupported architecture: " + osArch);

        return new Platform(os, arch);
    }

    static final class PreparedNatives {
        final Platform platform;
        final Path extractedRoot;
        final SystemBootstrap.Loader loader;

        PreparedNatives(Platform platform, Path extractedRoot, SystemBootstrap.Loader loader) {
            this.platform = platform;
            this.extractedRoot = extractedRoot;
            this.loader = loader;
        }
    }

    static PreparedNatives prepare(Path nativeCachePath) {
        Objects.requireNonNull(nativeCachePath, "nativeCachePath");
        Platform platform = detectPlatform();

        // The plan currently embeds win-x64 and linux-x64 only. Detecting arm64 is supported,
        // but we fail fast with a clear message until arm64 bundles are added.
        if (platform.arch == Arch.ARM64) {
            throw new UnsupportedOperationException("Native bundle for " + platform.key + " is not packaged in this build.");
        }

        Path artifactPath = getCodeSourcePath(NativeBundle.class);
        String fingerprint = fingerprintArtifact(artifactPath);

        Path extractionDir = nativeCachePath.resolve(platform.key).resolve(fingerprint);
        Path marker = extractionDir.resolve(".extracted.ok");

        if (!Files.exists(marker)) {
            log.info("Extracting native bundle for {} into {}", platform.key, extractionDir);
            try {
                Files.createDirectories(extractionDir);
                extractAll(platform.resourcePrefix, artifactPath, extractionDir);
                Files.writeString(marker, "ok");
            } catch (IOException e) {
                throw new RuntimeException("Failed to extract native bundle to " + extractionDir, e);
            }
        } else {
            log.debug("Native bundle already extracted for {} at {}", platform.key, extractionDir);
        }

        return new PreparedNatives(platform, extractionDir, createLoader(platform, extractionDir));
    }

    private static SystemBootstrap.Loader createLoader(Platform platform, Path extractedRoot) {
        return libname -> {
            // Always prefer system loader for JDK/AWT provided libraries.
            if ("jawt".equalsIgnoreCase(libname)) {
                System.loadLibrary(libname);
                return;
            }

            try {
                Path mapped = mapLibrary(platform, extractedRoot, libname);
                if (mapped != null) {
                    System.load(mapped.toAbsolutePath().toString());
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load native library '" + libname + "' from extracted bundle at " + extractedRoot, e);
            }

            // Fallback to default behavior when not part of the embedded bundle.
            System.loadLibrary(libname);
        };
    }

    private static Path mapLibrary(Platform platform, Path extractedRoot, String libname) throws IOException {
        if (platform.os == Os.WIN) {
            // JCEF expects these names.
            return switch (libname) {
                case "chrome_elf" -> extractedRoot.resolve("chrome_elf.dll");
                case "libcef" -> extractedRoot.resolve("libcef.dll");
                case "jcef" -> extractedRoot.resolve("jcef.dll");
                default -> null;
            };
        }

        // Linux: tolerate different naming patterns for CEF library.
        if (platform.os == Os.LINUX) {
            if ("jcef".equals(libname)) {
                Path candidate = extractedRoot.resolve("libjcef.so");
                return Files.exists(candidate) ? candidate : null;
            }
            if ("cef".equals(libname)) {
                Path exact = extractedRoot.resolve("libcef.so");
                if (Files.exists(exact)) return exact;
                // Search for libcef.so.* (common in some distribs).
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(extractedRoot, "libcef.so*")) {
                    for (Path p : ds) {
                        if (Files.isRegularFile(p)) return p;
                    }
                }
                return null;
            }
        }

        return null;
    }

    private static void extractAll(String resourcePrefix, Path artifactPath, Path extractionDir) throws IOException {
        // Ensure prefix ends without trailing slash for matching.
        String normalizedPrefix = resourcePrefix.endsWith("/") ? resourcePrefix.substring(0, resourcePrefix.length() - 1) : resourcePrefix;

        if (Files.isRegularFile(artifactPath) && artifactPath.toString().toLowerCase().endsWith(".jar")) {
            try (JarFile jar = new JarFile(artifactPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                boolean foundAny = false;
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(normalizedPrefix + "/") || entry.isDirectory()) continue;
                    foundAny = true;
                    Path out = extractionDir.resolve(name.substring((normalizedPrefix + "/").length()));
                    Files.createDirectories(out.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                if (!foundAny) {
                    throw new IOException("No embedded natives found at '" + normalizedPrefix + "/' inside " + artifactPath);
                }
            }
            return;
        }

        // Dev-mode/classpath mode: attempt to locate resources on filesystem.
        URL rootUrl = NativeBundle.class.getClassLoader().getResource(normalizedPrefix + "/");
        if (rootUrl == null) {
            throw new IOException("No embedded natives found at resource path '" + normalizedPrefix + "/'");
        }
        if (!"file".equalsIgnoreCase(rootUrl.getProtocol())) {
            throw new IOException("Unsupported resource URL protocol for natives: " + rootUrl);
        }
        Path resourceDir;
        try {
            resourceDir = Path.of(rootUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Failed to resolve natives resource directory URI: " + rootUrl, e);
        }
        copyTree(resourceDir, extractionDir);
    }

    private static void copyTree(Path srcDir, Path dstDir) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (var walk = Files.walk(srcDir)) {
            walk.forEach(paths::add);
        }
        for (Path p : paths) {
            Path rel = srcDir.relativize(p);
            Path out = dstDir.resolve(rel.toString());
            if (Files.isDirectory(p)) {
                Files.createDirectories(out);
            } else if (Files.isRegularFile(p)) {
                Files.createDirectories(out.getParent());
                Files.copy(p, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path getCodeSourcePath(Class<?> cls) {
        try {
            return Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve code source for " + cls.getName(), e);
        }
    }

    private static String fingerprintArtifact(Path artifactPath) {
        try {
            long size = Files.exists(artifactPath) ? Files.size(artifactPath) : -1L;
            long mtime = Files.exists(artifactPath) ? Files.getLastModifiedTime(artifactPath).toMillis() : -1L;
            String data = artifactPath.toAbsolutePath() + "|" + size + "|" + mtime;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Shorten to keep paths readable.
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (Exception e) {
            // Worst case: keep a stable but not too long marker.
            return "unknown";
        }
    }
}

