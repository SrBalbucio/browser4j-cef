package balbucio.browser4j.cache.persistence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages physical storage of cached objects on disk.
 * Supports SHA-256 based naming and optional GZIP compression.
 */
public class CacheStorage {

    private static final Logger LOG = Logger.getLogger(CacheStorage.class.getName());
    private final Path objectsDir;

    public CacheStorage(Path cacheDir) {
        this.objectsDir = cacheDir.resolve("objects");
        try {
            Files.createDirectories(this.objectsDir);
        } catch (IOException e) {
            LOG.severe("[Cache] Failed to create objects directory: " + e.getMessage());
        }
    }

    /**
     * Stores data on disk.
     * @param hash SHA-256 hash of the content.
     * @param data Raw data.
     * @param compress If true, data will be GZIP compressed.
     */
    public void store(String hash, byte[] data, boolean compress) throws IOException {
        Path file = objectsDir.resolve(hash);
        if (Files.exists(file)) return; // Deduplication: already exists

        try (OutputStream os = Files.newOutputStream(file)) {
            if (compress) {
                try (GZIPOutputStream gzos = new GZIPOutputStream(os)) {
                    gzos.write(data);
                }
            } else {
                os.write(data);
            }
        }
    }

    /**
     * Loads data from disk.
     * @param hash SHA-256 hash.
     * @param compressed If true, data is expected to be GZIP'ed.
     */
    public byte[] load(String hash, boolean compressed) throws IOException {
        Path file = objectsDir.resolve(hash);
        if (!Files.exists(file)) throw new FileNotFoundException(hash);

        try (InputStream is = Files.newInputStream(file)) {
            if (compressed) {
                try (GZIPInputStream gzis = new GZIPInputStream(is);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = gzis.read(buf)) > 0) baos.write(buf, 0, len);
                    return baos.toByteArray();
                }
            } else {
                return Files.readAllBytes(file);
            }
        }
    }

    public void delete(String hash) {
        try {
            Files.deleteIfExists(objectsDir.resolve(hash));
        } catch (IOException e) {
            LOG.warning("[Cache] Failed to delete object: " + hash);
        }
    }

    public boolean exists(String hash) {
        return Files.exists(objectsDir.resolve(hash));
    }
}
