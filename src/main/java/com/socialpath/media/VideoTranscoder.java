package com.socialpath.media;

import com.socialpath.media.BundledFfmpeg.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Re-encodes a video into the one format every browser plays: MP4 with H.264
 * video and AAC sound, its index at the front so playback starts before the
 * whole file has loaded. The picture is scaled down to fit MAX_SIDE and turned
 * upright as the file's rotation metadata says (phones store portrait videos
 * sideways), so the result needs no player support for either.
 */
@Component
@RequiredArgsConstructor
public class VideoTranscoder {

    /** Longest side of a published video, in pixels. */
    public static final int MAX_SIDE = 1280;

    /**
     * Fits the picture into MAX_SIDE x MAX_SIDE without ever enlarging it,
     * keeping the aspect ratio; H.264 needs even dimensions.
     */
    private static final String SCALE = "scale=w='min(" + MAX_SIDE + ",iw)':h='min(" + MAX_SIDE + ",ih)'"
            + ":force_original_aspect_ratio=decrease:force_divisible_by=2";

    private final BundledFfmpeg ffmpeg;

    /**
     * Converts a video. On failure nothing is left at the target.
     * @param source any video ffmpeg can read
     * @param target where the MP4 goes
     * @param timeout how long the conversion may take
     * @throws IOException when ffmpeg fails or runs out of time
     */
    public void toMp4(Path source, Path target, Duration timeout) throws IOException {
        BundledFfmpeg.Result result;
        try {
            result = ffmpeg.run(Tool.FFMPEG, timeout,
                    "-v", "error", "-y", "-i", source.toString(),
                    // The first video stream and, if there is one, the first sound stream.
                    "-map", "0:v:0", "-map", "0:a:0?",
                    "-vf", SCALE,
                    "-c:v", "libopenh264", "-b:v", "2500k", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "128k",
                    "-movflags", "+faststart",
                    "-f", "mp4", target.toString());
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        if (!result.succeeded() || !Files.exists(target) || Files.size(target) == 0) {
            Files.deleteIfExists(target);
            throw new IOException("ffmpeg could not convert the video: " + lastLine(result.stderr()));
        }
    }

    private static String lastLine(String text) {
        String trimmed = text.strip();
        int newline = trimmed.lastIndexOf('\n');
        return newline < 0 ? trimmed : trimmed.substring(newline + 1);
    }
}
