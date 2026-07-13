package com.socialpath.web;

import com.socialpath.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
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

/**
 * Streams user-uploaded media (avatars, publication photos and videos)
 * straight from GridFS. Replaces the previous approach of inlining every
 * file into the page as a Base64 data URI, which bloated HTML and re-read
 * whole files on each page render. GridFS ids are immutable, so responses
 * are safely cacheable by the browser.
 */
@Controller
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    /**
     * Streams a stored file by its GridFS id.
     * @param id GridFS file id
     * @return the file bytes with a best-effort content type, or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String id) throws IOException {
        GridFsResource resource = fileStorageService.getFileById(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(resolveContentType(resource))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(new InputStreamResource(resource.getInputStream()));
    }

    private MediaType resolveContentType(GridFsResource resource) {
        // Older files were stored without a content type, so fall back to
        // guessing from the filename before defaulting to a generic type.
        try {
            String stored = resource.getContentType();
            if (stored != null) {
                return MediaType.parseMediaType(stored);
            }
        } catch (Exception ignored) {
            // no content type in GridFS metadata
        }
        String guessed = URLConnection.guessContentTypeFromName(resource.getFilename());
        return guessed != null ? MediaType.parseMediaType(guessed) : MediaType.APPLICATION_OCTET_STREAM;
    }
}
