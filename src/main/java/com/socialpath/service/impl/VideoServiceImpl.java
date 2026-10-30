package com.socialpath.service.impl;

import com.socialpath.entity.VideoJob;
import com.socialpath.exception.InvalidVideoException;
import com.socialpath.media.PublicationMediaType;
import com.socialpath.media.VideoProbe;
import com.socialpath.media.VideoProbe.VideoInfo;
import com.socialpath.media.VideoTranscoder;
import com.socialpath.repository.VideoJobRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private static final Set<String> WEBM_VIDEO_CODECS = Set.of("vp8", "vp9", "av1");
    private static final Set<String> WEBM_AUDIO_CODECS = Set.of("opus", "vorbis");
    private static final int MAX_ERROR_LENGTH = 255;
    private static final String ABANDONED = "The conversion was interrupted.";

    private final FileStorageService fileStorageService;
    private final VideoProbe videoProbe;
    private final VideoJobRepository videoJobRepository;

    @Override
    public String accept(MultipartFile upload, PublicationMediaType type) throws IOException {
        String storedId = fileStorageService.storeFile(upload, type.extension());
        try {
            VideoInfo info = videoProbe.probe(fileStorageService.pathOf(storedId));
            if (info.duration().compareTo(MAX_DURATION) > 0) {
                throw new InvalidVideoException(TOO_LONG);
            }
            if (playsAsIs(type, info)) {
                return storedId;
            }
            String fileId = UUID.randomUUID() + "." + PublicationMediaType.MP4.extension();
            videoJobRepository.save(new VideoJob(fileId, storedId, LocalDateTime.now()));
            return fileId;
        } catch (IOException | RuntimeException e) {
            fileStorageService.deleteFileById(storedId);
            throw e;
        }
    }

    /**
     * Tells videos every browser plays from those that need converting.
     * @param type the container
     * @param info what the video consists of
     * @return true when it can be published as uploaded
     */
    public static boolean playsAsIs(PublicationMediaType type, VideoInfo info) {
        if (info.longerSide() > VideoTranscoder.MAX_SIDE) {
            return false;
        }
        String audio = info.audioCodec();
        return switch (type) {
            case MP4 -> "h264".equals(info.videoCodec()) && (audio == null || "aac".equals(audio));
            case WEBM -> WEBM_VIDEO_CODECS.contains(info.videoCodec())
                    && (audio == null || WEBM_AUDIO_CODECS.contains(audio));
            default -> false;
        };
    }

    @Override
    public Map<String, VideoJob.Status> statusOf(Collection<String> fileIds) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return videoJobRepository.findByFileIdIn(fileIds).stream()
                .collect(Collectors.toMap(VideoJob::getFileId, VideoJob::getStatus));
    }

    @Override
    @Transactional
    public List<String> discard(Collection<String> fileIds) {
        if (fileIds.isEmpty()) {
            return List.of();
        }
        List<VideoJob> jobs = videoJobRepository.findByFileIdIn(fileIds);
        videoJobRepository.deleteAll(jobs);
        return jobs.stream().map(VideoJob::getSourceFileId).toList();
    }

    @Override
    @Transactional
    public Optional<VideoJob> claimNext() {
        LocalDateTime now = LocalDateTime.now();
        return videoJobRepository.lockNextPending(now.minus(RETRY_DELAY)).map(job -> {
            job.setStatus(VideoJob.Status.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            job.setUpdatedAt(now);
            return videoJobRepository.save(job);
        });
    }

    @Override
    @Transactional
    public List<String> releaseAbandoned(Duration after) {
        LocalDateTime now = LocalDateTime.now();
        return videoJobRepository.findByStatusAndUpdatedAtBefore(VideoJob.Status.PROCESSING, now.minus(after))
                .stream()
                .filter(job -> recordFailure(job, ABANDONED, now))
                .map(VideoJob::getSourceFileId)
                .toList();
    }

    @Override
    @Transactional
    public boolean complete(VideoJob job) {
        return videoJobRepository.findById(job.getId()).map(stored -> {
            stored.setStatus(VideoJob.Status.READY);
            stored.setError(null);
            stored.setUpdatedAt(LocalDateTime.now());
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean fail(VideoJob job, String error) {
        return videoJobRepository.findById(job.getId())
                .map(stored -> recordFailure(stored, error, LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Queues the job again, or gives it up when it is out of attempts.
     * @param job the job, managed by the current transaction
     * @param error what went wrong
     * @param now the current time
     * @return true when the job was given up
     */
    private static boolean recordFailure(VideoJob job, String error, LocalDateTime now) {
        boolean givenUp = job.getAttempts() >= MAX_ATTEMPTS;
        job.setStatus(givenUp ? VideoJob.Status.FAILED : VideoJob.Status.PENDING);
        job.setError(error == null || error.length() <= MAX_ERROR_LENGTH
                ? error : error.substring(0, MAX_ERROR_LENGTH));
        job.setUpdatedAt(now);
        return givenUp;
    }
}
