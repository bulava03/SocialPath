package com.socialpath.service.impl;

import com.socialpath.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * File-system media storage. Ids are server-generated ({uuid}.{ext}), never
 * derived from client-supplied names, and every id is validated against a
 * strict pattern plus a normalized-path containment check before touching the
 * disk, so a crafted id like "../../etc/passwd" can never escape the media
 * directory.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    /** {uuid} optionally followed by a short alphanumeric extension. */
    private static final Pattern FILE_ID = Pattern.compile("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}(\\.[a-z0-9]{1,10})?");
    private static final Pattern EXTENSION = Pattern.compile("[a-z0-9]{1,10}");

    private final Path root;

    public FileStorageServiceImpl(@Value("${app.media.dir}") String mediaDir) {
        this.root = Path.of(mediaDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create media directory " + root, e);
        }
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Files.copy(file.getInputStream(), root.resolve(fileId), StandardCopyOption.REPLACE_EXISTING);
        return fileId;
    }

    @Override
    public Resource getFileById(String fileId) {
        Path path = resolveSafely(fileId);
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        return new FileSystemResource(path);
    }

    @Override
    public boolean isVideo(String fileId) {
        Path path = resolveSafely(fileId);
        return path != null && Files.isRegularFile(path)
                && fileId.toLowerCase(Locale.ROOT).endsWith(".mp4");
    }

    @Override
    public void deleteFileById(String fileId) {
        Path path = resolveSafely(fileId);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete media file " + fileId, e);
        }
    }

    /**
     * Resolves a file id inside the media directory, or returns null when the
     * id is malformed or would resolve outside of it.
     * @param fileId the id to resolve
     * @return the resolved path, or null
     */
    private Path resolveSafely(String fileId) {
        if (fileId == null || !FILE_ID.matcher(fileId).matches()) {
            return null;
        }
        Path path = root.resolve(fileId).normalize();
        if (!path.startsWith(root)) {
            return null;
        }
        return path;
    }

    /**
     * Extracts a safe, lowercase extension (with a leading dot) from the
     * uploaded file name, or an empty string when there is none usable.
     * @param originalFilename the client-supplied file name
     * @return ".ext" or ""
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSION.matcher(extension).matches()) {
            return "";
        }
        return "." + extension;
    }
}
