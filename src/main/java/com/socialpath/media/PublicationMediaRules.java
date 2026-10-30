package com.socialpath.media;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The upload rules for posts and comments, checked on the server. The feed's
 * script (static/script/comments.js) applies the same rules for quick
 * feedback, but a forged request skips the script, so the server must not
 * rely on it. The messages are the ones the script shows.
 */
public final class PublicationMediaRules {

    /** Most files one post or comment may carry. */
    public static final int MAX_FILES = 5;
    /** Largest photo, in bytes. */
    public static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    /** Largest video, in bytes. */
    public static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;

    /** Error for too many files. */
    public static final String TOO_MANY = "Up to 5 photos or 1 video.";
    /** Error for a video combined with other files. */
    public static final String VIDEO_NOT_ALONE = "You can attach only a single video without photos.";
    /** Error for a photo over the limit. */
    public static final String PHOTO_TOO_LARGE = "A photo must not exceed 10 MB.";
    /** Error for a video over the limit. */
    public static final String VIDEO_TOO_LARGE = "A video must not exceed 100 MB.";
    /** Error for a file of any other kind. */
    public static final String NOT_ALLOWED = "Only photos and videos are allowed.";

    private PublicationMediaRules() {
    }

    /**
     * Checks every uploaded file before any of them is stored.
     * @param files the uploads, possibly null or empty
     * @return the detected type of each file, in the same order
     * @throws IllegalArgumentException with a message for the user when a rule is broken
     */
    public static List<PublicationMediaType> check(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException(TOO_MANY);
        }

        List<PublicationMediaType> types = new ArrayList<>();
        for (MultipartFile file : files) {
            PublicationMediaType type = detect(file);
            long limit = type.isVideo() ? MAX_VIDEO_BYTES : MAX_PHOTO_BYTES;
            if (file.getSize() > limit) {
                throw new IllegalArgumentException(type.isVideo() ? VIDEO_TOO_LARGE : PHOTO_TOO_LARGE);
            }
            types.add(type);
        }
        if (files.size() > 1 && types.stream().anyMatch(PublicationMediaType::isVideo)) {
            throw new IllegalArgumentException(VIDEO_NOT_ALONE);
        }
        return types;
    }

    private static PublicationMediaType detect(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return PublicationMediaType.detect(in.readNBytes(PublicationMediaType.HEADER_LENGTH))
                    .orElseThrow(() -> new IllegalArgumentException(NOT_ALLOWED));
        }
    }
}
