package com.socialpath.web;

import com.socialpath.service.FileStorageService;
import com.socialpath.service.HandleAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Streams user-uploaded media (avatars, publication photos and videos) from
 * the media directory. File ids are immutable server-generated names, so
 * responses are safely cacheable by the browser.
 */
@Controller
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    /** Avatars (always JPEG now, older ones PNG/BMP) and publication media. */
    private static final Map<String, MediaType> DISPLAYABLE = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "gif", MediaType.IMAGE_GIF,
            "bmp", MediaType.parseMediaType("image/bmp"),
            "webp", MediaType.parseMediaType("image/webp"),
            "mp4", MediaType.parseMediaType("video/mp4"),
            "webm", MediaType.parseMediaType("video/webm"));

    private final FileStorageService fileStorageService;

    /**
     * Streams a stored file by its id. With size=thumb an avatar is served as
     * its thumbnail; files without one (publication media, avatars not yet
     * converted) fall back to the full file.
     * @param id media file id
     * @param size "thumb" for the thumbnail, absent for the full file
     * @return the file bytes with a best-effort content type, or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable String id,
                                             @RequestParam(name = "size", required = false) String size)
            throws IOException {
        Resource resource = null;
        if ("thumb".equals(size)) {
            resource = fileStorageService.getFileById(HandleAvatarService.thumbnailIdOf(id));
        }
        if (resource == null) {
            resource = fileStorageService.getFileById(id);
        }
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType type = displayableType(resource.getFilename());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(type == null ? MediaType.APPLICATION_OCTET_STREAM : type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                // Even opened directly, a stored file can never run script on this site.
                .header("Content-Security-Policy", "sandbox");
        if (type == null) {
            // Not a picture or video we know: offer it as a download, never render it.
            response.header(HttpHeaders.CONTENT_DISPOSITION, "attachment");
        }
        return response.body(resource);
    }

    /**
     * The content type for files the site shows inline. Only known picture and
     * video types qualify: guessing from any extension would serve an uploaded
     * .html or .svg as a page of this site.
     * @param filename the stored file name
     * @return the type, or null for anything else
     */
    private static MediaType displayableType(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? null : DISPLAYABLE.get(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
