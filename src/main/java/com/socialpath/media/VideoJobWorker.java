package com.socialpath.media;

import com.socialpath.entity.VideoJob;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * A failed conversion is tried again a few times. Jobs a worker took but
 * never finished, because the application stopped in the middle, go back to
 * the queue once they have been PROCESSING longer than a conversion may last.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.video.worker.enabled", havingValue = "true", matchIfMissing = true)
public class VideoJobWorker {

    /**
     * Time between a conversion running out and its job counting as
     * abandoned: enough to stop ffmpeg and record the failure.
     */
    public static final Duration ABANDON_MARGIN = Duration.ofMinutes(5);

    private final VideoService videoService;
    private final FileStorageService fileStorageService;
    private final VideoTranscoder videoTranscoder;
    private final Duration timeout;

    /**
     * Creates the worker.
     * @param videoService the queue
     * @param fileStorageService where uploads and results are stored
     * @param videoTranscoder the converter
     * @param timeout how long one conversion may take before it is stopped
     */
    public VideoJobWorker(VideoService videoService, FileStorageService fileStorageService,
                          VideoTranscoder videoTranscoder,
                          @Value("${app.video.worker.timeout:10m}") Duration timeout) {
        this.videoService = videoService;
        this.fileStorageService = fileStorageService;
        this.videoTranscoder = videoTranscoder;
        this.timeout = timeout;
    }

    /** Converts every waiting video, then waits for the next round. */
    @Scheduled(fixedDelayString = "${app.video.worker.delay-ms:3000}")
    public void work() {
        for (String upload : videoService.releaseAbandoned(timeout.plus(ABANDON_MARGIN))) {
            log.warn("Gave up converting video {}: interrupted too many times", upload);
            fileStorageService.deleteFileById(upload);
        }
        Optional<VideoJob> job;
        while ((job = videoService.claimNext()).isPresent()) {
            process(job.get());
        }
    }

    private void process(VideoJob job) {
        Path output = null;
        try {
            output = Files.createTempFile("video-", ".mp4");
            videoTranscoder.toMp4(fileStorageService.pathOf(job.getSourceFileId()), output, timeout);
            fileStorageService.moveIn(output, job.getFileId());
            if (videoService.complete(job)) {
                fileStorageService.deleteFileById(job.getSourceFileId());
                log.info("Converted video {} -> {}", job.getSourceFileId(), job.getFileId());
            } else {
                // The publication was deleted while its video was being converted.
                fileStorageService.deleteFileById(job.getFileId());
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Could not convert video {} (attempt {}): {}",
                    job.getSourceFileId(), job.getAttempts(), e.getMessage());
            if (videoService.fail(job, e.getMessage())) {
                fileStorageService.deleteFileById(job.getSourceFileId());
            }
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
