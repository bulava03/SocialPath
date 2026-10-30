package com.socialpath.media;

import com.socialpath.entity.VideoJob;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Converts queued videos in the background. Every few seconds it takes the
 * waiting jobs one by one; the conversion itself runs outside any database
 * transaction, so a long one holds no locks. The MP4 is written to a
 * temporary file and moved into storage only when complete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.video.worker.enabled", havingValue = "true", matchIfMissing = true)
public class VideoJobWorker {

    /** How long one conversion may take before it is given up. */
    static final Duration TIMEOUT = Duration.ofMinutes(10);

    private final VideoService videoService;
    private final FileStorageService fileStorageService;
    private final VideoTranscoder videoTranscoder;

    /** Converts every waiting video, then waits for the next round. */
    @Scheduled(fixedDelayString = "${app.video.worker.delay-ms:3000}")
    public void work() {
        Optional<VideoJob> job;
        while ((job = videoService.claimNext()).isPresent()) {
            process(job.get());
        }
    }

    private void process(VideoJob job) {
        Path output = null;
        try {
            output = Files.createTempFile("video-", ".mp4");
            videoTranscoder.toMp4(fileStorageService.pathOf(job.getSourceFileId()), output, TIMEOUT);
            fileStorageService.moveIn(output, job.getFileId());
            if (videoService.complete(job)) {
                fileStorageService.deleteFileById(job.getSourceFileId());
                log.info("Converted video {} -> {}", job.getSourceFileId(), job.getFileId());
            } else {
                // The publication was deleted while its video was being converted.
                fileStorageService.deleteFileById(job.getFileId());
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Could not convert video {}: {}", job.getSourceFileId(), e.getMessage());
            videoService.fail(job, e.getMessage());
        } finally {
            deleteQuietly(output);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete temporary file {}", file, e);
        }
    }
}
