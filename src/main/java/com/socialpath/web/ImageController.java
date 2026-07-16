package com.socialpath.web;

import com.socialpath.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Locale;

/**
 * Streams user-uploaded media (avatars, publication photos and videos) from
 * the media directory. File ids are immutable server-generated names, so
 * responses are safely cacheable by the browser.
 */
@Controller
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    /**
     * Streams a stored file by its id.
     * @param id media file id
     * @return the file bytes with a best-effort content type, or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable String id) throws IOException {
        Resource resource = fileStorageService.getFileById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(resolveContentType(resource.getFilename()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(resource);
    }

    private MediaType resolveContentType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        // URLConnection does not know every type we care about (mp4/webp on
        // some JDKs), so cover those explicitly before falling back.
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mp4")) {
            return MediaType.parseMediaType("video/mp4");
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        String guessed = URLConnection.guessContentTypeFromName(filename);
        return guessed != null ? MediaType.parseMediaType(guessed) : MediaType.APPLICATION_OCTET_STREAM;
    }
}
