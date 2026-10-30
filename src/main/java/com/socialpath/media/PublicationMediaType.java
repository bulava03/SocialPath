package com.socialpath.media;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The media formats a post or comment may carry. The type of an upload is
 * read from its first bytes, never from the file name or the content type the
 * client sends, and the stored file gets the extension of that type: an HTML
 * page renamed to photo.jpg is not a photo.
 */
public enum PublicationMediaType {
    /** JPEG photo. */
    JPEG("jpg", false),
    /** PNG picture. */
    PNG("png", false),
    /** GIF picture. */
    GIF("gif", false),
    /** WebP picture. */
    WEBP("webp", false),
    /** MP4 video. */
    MP4("mp4", true),
    /** WebM video (Matroska with the "webm" document type; plain Matroska is not playable everywhere). */
    WEBM("webm", true);

    /** Bytes needed to tell the formats apart (the WebM document type sits a few dozen bytes in). */
    public static final int HEADER_LENGTH = 64;

    private static final Set<String> GIF_SIGNATURES = Set.of("GIF87a", "GIF89a");

    /** EBML header element of the document type "webm": id 0x4282, size 4, then the text. */
    private static final byte[] WEBM_DOC_TYPE = {0x42, (byte) 0x82, (byte) 0x84, 'w', 'e', 'b', 'm'};

    /** ISO base media brands of MP4 video (HEIF image brands are left out on purpose). */
    private static final Set<String> MP4_BRANDS = Set.of(
            "isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "dash", "mmp4", "MSNV");

    private final String extension;
    private final boolean video;

    PublicationMediaType(String extension, boolean video) {
        this.extension = extension;
        this.video = video;
    }

    /**
     * Extension stored files of this type get.
     * @return the extension without a dot
     */
    public String extension() {
        return extension;
    }

    /**
     * Tells videos from photos.
     * @return true for a video format
     */
    public boolean isVideo() {
        return video;
    }

    /**
     * Recognizes a format by the first bytes of a file.
     * @param header the first HEADER_LENGTH bytes, fewer if the file is shorter
     * @return the format, or empty when the file is none of the allowed ones
     */
    public static Optional<PublicationMediaType> detect(byte[] header) {
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (GIF_SIGNATURES.contains(ascii(header, 0, 6))) {
            return Optional.of(GIF);
        }
        if (ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP")) {
            return Optional.of(WEBP);
        }
        if (ascii(header, 4, 4).equals("ftyp") && MP4_BRANDS.contains(ascii(header, 8, 4))) {
            return Optional.of(MP4);
        }
        if (startsWith(header, 0x1A, 0x45, 0xDF, 0xA3) && contains(header, WEBM_DOC_TYPE)) {
            return Optional.of(WEBM);
        }
        return Optional.empty();
    }

    private static boolean contains(byte[] data, byte[] part) {
        for (int i = 0; i + part.length <= data.length; i++) {
            if (Arrays.equals(data, i, i + part.length, part, 0, part.length)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The format of a stored file, by the extension the server gave it.
     * @param fileId a stored file id such as {uuid}.mp4
     * @return the format, or empty for any other extension
     */
    public static Optional<PublicationMediaType> ofFileId(String fileId) {
        int dot = fileId.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = fileId.substring(dot + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String ascii(byte[] data, int from, int length) {
        if (data.length < from + length) {
            return "";
        }
        return new String(data, from, length, StandardCharsets.US_ASCII);
    }
}
