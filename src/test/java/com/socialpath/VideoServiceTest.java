package com.socialpath;

import com.socialpath.entity.VideoJob;
import com.socialpath.exception.InvalidVideoException;
import com.socialpath.media.PublicationMediaType;
import com.socialpath.media.VideoProbe;
import com.socialpath.media.VideoProbe.VideoInfo;
import com.socialpath.repository.VideoJobRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.VideoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoServiceTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private FileStorageService fileStorageService;
    private VideoProbe videoProbe;
    private VideoJobRepository videoJobRepository;
    private VideoServiceImpl service;
    private final MockMultipartFile upload = new MockMultipartFile("media", "clip", "video/any", new byte[]{1});

    @BeforeEach
    void setUp() throws Exception {
        fileStorageService = mock(FileStorageService.class);
        videoProbe = mock(VideoProbe.class);
        videoJobRepository = mock(VideoJobRepository.class);
        service = new VideoServiceImpl(fileStorageService, videoProbe, videoJobRepository);
        when(fileStorageService.storeFile(any(), anyString())).thenAnswer(call -> "upload." + call.getArgument(1));
        when(fileStorageService.pathOf(anyString())).thenAnswer(call -> Path.of(call.<String>getArgument(0)));
    }

    private void probed(VideoInfo info) throws Exception {
        when(videoProbe.probe(any())).thenReturn(info);
    }

    @Test
    void playsAsIs_OnlyBrowserCodecsWithinTheSizeLimit() {
        assertTrue(VideoServiceImpl.playsAsIs(PublicationMediaType.MP4, new VideoInfo(ONE_MINUTE, 1280, 720, "h264", "aac")));
        assertTrue(VideoServiceImpl.playsAsIs(PublicationMediaType.MP4, new VideoInfo(ONE_MINUTE, 640, 360, "h264", null)));
        assertTrue(VideoServiceImpl.playsAsIs(PublicationMediaType.WEBM, new VideoInfo(ONE_MINUTE, 640, 360, "vp9", "opus")));
        // iPhone HEVC in an MP4 container
        assertFalse(VideoServiceImpl.playsAsIs(PublicationMediaType.MP4, new VideoInfo(ONE_MINUTE, 1280, 720, "hevc", "aac")));
        // Right codec, but 1080p is over the size limit
        assertFalse(VideoServiceImpl.playsAsIs(PublicationMediaType.MP4, new VideoInfo(ONE_MINUTE, 1920, 1080, "h264", "aac")));
        // Portrait counts by its longer side too
        assertFalse(VideoServiceImpl.playsAsIs(PublicationMediaType.MP4, new VideoInfo(ONE_MINUTE, 1080, 1920, "h264", "aac")));
        // Containers browsers do not play are converted whatever is inside
        assertFalse(VideoServiceImpl.playsAsIs(PublicationMediaType.MOV, new VideoInfo(ONE_MINUTE, 640, 360, "h264", "aac")));
    }

    @Test
    void accept_PlayableMp4_IsPublishedAsUploaded() throws Exception {
        probed(new VideoInfo(ONE_MINUTE, 1280, 720, "h264", "aac"));

        assertEquals("upload.mp4", service.accept(upload, PublicationMediaType.MP4));
        verify(videoJobRepository, never()).save(any());
    }

    @Test
    void accept_Mov_IsQueuedUnderTheMp4ItWillBecome() throws Exception {
        probed(new VideoInfo(ONE_MINUTE, 1920, 1080, "hevc", "aac"));

        String fileId = service.accept(upload, PublicationMediaType.MOV);

        ArgumentCaptor<VideoJob> job = ArgumentCaptor.forClass(VideoJob.class);
        verify(videoJobRepository).save(job.capture());
        assertTrue(fileId.matches("[a-f0-9-]{36}\\.mp4"), fileId);
        assertEquals(fileId, job.getValue().getFileId());
        assertEquals("upload.mov", job.getValue().getSourceFileId());
        assertEquals(VideoJob.Status.PENDING, job.getValue().getStatus());
        // The upload is kept: the worker converts it later.
        verify(fileStorageService, never()).deleteFileById(anyString());
    }

    @Test
    void accept_TooLong_IsRefusedAndNothingIsKept() throws Exception {
        probed(new VideoInfo(Duration.ofMinutes(10).plusSeconds(1), 640, 360, "h264", "aac"));

        InvalidVideoException error = assertThrows(InvalidVideoException.class,
                () -> service.accept(upload, PublicationMediaType.MP4));

        assertEquals(VideoService.TOO_LONG, error.getMessage());
        verify(fileStorageService).deleteFileById("upload.mp4");
        verify(videoJobRepository, never()).save(any());
    }

    @Test
    void accept_Unreadable_IsRefusedAndNothingIsKept() throws Exception {
        when(videoProbe.probe(any())).thenThrow(new InvalidVideoException(VideoProbe.UNREADABLE));

        assertThrows(InvalidVideoException.class, () -> service.accept(upload, PublicationMediaType.MOV));

        verify(fileStorageService).deleteFileById("upload.mov");
    }

    @Test
    void displayStatus_WaitingAndConvertingLookTheSame() {
        assertEquals("READY", VideoService.displayStatus(null));
        assertEquals("READY", VideoService.displayStatus(VideoJob.Status.READY));
        assertEquals("PROCESSING", VideoService.displayStatus(VideoJob.Status.PENDING));
        assertEquals("PROCESSING", VideoService.displayStatus(VideoJob.Status.PROCESSING));
        assertEquals("FAILED", VideoService.displayStatus(VideoJob.Status.FAILED));
    }

    @Test
    void discard_ForgetsTheJobsAndHandsBackTheirUploads() {
        VideoJob job = new VideoJob("result.mp4", "upload.mov", LocalDateTime.now());
        when(videoJobRepository.findByFileIdIn(List.of("result.mp4", "photo.jpg"))).thenReturn(List.of(job));

        List<String> uploads = service.discard(List.of("result.mp4", "photo.jpg"));

        assertEquals(List.of("upload.mov"), uploads);
        verify(videoJobRepository).deleteAll(List.of(job));
    }

    @Test
    void fail_BeforeTheLastAttempt_QueuesTheJobAgain() {
        VideoJob job = storedJob(VideoService.MAX_ATTEMPTS - 1);

        assertFalse(service.fail(job, "ffmpeg crashed"));

        assertEquals(VideoJob.Status.PENDING, job.getStatus());
        assertEquals("ffmpeg crashed", job.getError());
    }

    @Test
    void fail_OnTheLastAttempt_GivesUp() {
        VideoJob job = storedJob(VideoService.MAX_ATTEMPTS);

        assertTrue(service.fail(job, "x".repeat(300)));

        assertEquals(VideoJob.Status.FAILED, job.getStatus());
        assertEquals(255, job.getError().length());
    }

    @Test
    void fail_JobGoneMeanwhile_NothingToGiveUp() {
        VideoJob job = new VideoJob("result.mp4", "upload.mov", LocalDateTime.now());
        job.setId(9L);

        assertFalse(service.fail(job, "ffmpeg crashed"));
    }

    @Test
    void releaseAbandoned_RequeuesThemOrGivesUpThoseOutOfAttempts() {
        VideoJob retried = storedJob(1);
        VideoJob exhausted = storedJob(VideoService.MAX_ATTEMPTS);
        exhausted.setSourceFileId("exhausted.mov");
        LocalDateTime before = LocalDateTime.now();
        when(videoJobRepository.findByStatusAndUpdatedAtBefore(eq(VideoJob.Status.PROCESSING), any()))
                .thenReturn(List.of(retried, exhausted));

        List<String> givenUp = service.releaseAbandoned(Duration.ofMinutes(15));

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoJobRepository).findByStatusAndUpdatedAtBefore(eq(VideoJob.Status.PROCESSING), cutoff.capture());
        assertFalse(cutoff.getValue().isAfter(LocalDateTime.now().minusMinutes(15)));
        assertFalse(cutoff.getValue().isBefore(before.minusMinutes(15)));
        assertEquals(List.of("exhausted.mov"), givenUp);
        assertEquals(VideoJob.Status.PENDING, retried.getStatus());
        assertEquals(VideoJob.Status.FAILED, exhausted.getStatus());
    }

    /** A job being converted, on its given attempt, that the repository finds by id. */
    private VideoJob storedJob(int attempts) {
        VideoJob job = new VideoJob("result.mp4", "upload.mov", LocalDateTime.now());
        job.setId((long) attempts);
        job.setStatus(VideoJob.Status.PROCESSING);
        job.setAttempts(attempts);
        when(videoJobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    @Test
    void statusOf_NoVideos_AsksNothing() {
        assertEquals(Map.of(), service.statusOf(List.of()));
        verify(videoJobRepository, never()).findByFileIdIn(any());
    }
}
