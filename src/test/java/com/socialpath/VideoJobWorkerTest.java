package com.socialpath;

import com.socialpath.entity.VideoJob;
import com.socialpath.media.VideoJobWorker;
import com.socialpath.media.VideoTranscoder;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoJobWorkerTest {

    private VideoService videoService;
    private FileStorageService fileStorageService;
    private VideoTranscoder videoTranscoder;
    private VideoJobWorker worker;
    private VideoJob job;
    private final Duration timeout = Duration.ofMinutes(7);

    @BeforeEach
    void setUp() {
        videoService = mock(VideoService.class);
        fileStorageService = mock(FileStorageService.class);
        videoTranscoder = mock(VideoTranscoder.class);
        worker = new VideoJobWorker(videoService, fileStorageService, videoTranscoder, timeout);
        job = new VideoJob("result.mp4", "upload.mov", LocalDateTime.now());
        job.setId(1L);
        job.setAttempts(1);
        when(videoService.claimNext()).thenReturn(Optional.of(job), Optional.empty());
        when(fileStorageService.pathOf(anyString())).thenAnswer(call -> Path.of(call.<String>getArgument(0)));
    }

    @Test
    void convertedVideo_IsStoredBeforeItIsMarkedReady_ThenTheUploadIsRemoved() throws IOException {
        when(videoService.complete(job)).thenReturn(true);

        worker.work();

        InOrder order = inOrder(videoTranscoder, fileStorageService, videoService);
        order.verify(videoTranscoder).toMp4(eq(Path.of("upload.mov")), any(), eq(timeout));
        order.verify(fileStorageService).moveIn(any(), eq("result.mp4"));
        order.verify(videoService).complete(job);
        order.verify(fileStorageService).deleteFileById("upload.mov");
    }

    @Test
    void publicationDeletedMeanwhile_TheResultIsThrownAway() throws IOException {
        when(videoService.complete(job)).thenReturn(false);

        worker.work();

        verify(fileStorageService).deleteFileById("result.mp4");
    }

    @Test
    void failedConversion_IsRecorded_AndTheUploadKeptForTheNextAttempt() throws IOException {
        doThrow(new IOException("ffmpeg could not convert the video: Invalid data"))
                .when(videoTranscoder).toMp4(any(), any(), any());
        when(videoService.fail(any(), anyString())).thenReturn(false);

        worker.work();

        verify(videoService).fail(job, "ffmpeg could not convert the video: Invalid data");
        verify(fileStorageService, never()).moveIn(any(), anyString());
        verify(videoService, never()).complete(any());
        verify(fileStorageService, never()).deleteFileById(anyString());
    }

    @Test
    void lastAttemptFailed_TheUploadIsDeleted() throws IOException {
        doThrow(new IOException("ffmpeg ran out of time")).when(videoTranscoder).toMp4(any(), any(), any());
        when(videoService.fail(any(), anyString())).thenReturn(true);

        worker.work();

        verify(fileStorageService).deleteFileById("upload.mov");
    }

    @Test
    void abandonedJobs_AreReleasedFirst_OnlyAfterAConversionWouldHaveTimedOut() {
        when(videoService.releaseAbandoned(any())).thenReturn(List.of("given-up.mov"));
        when(videoService.complete(any())).thenReturn(true);

        worker.work();

        InOrder order = inOrder(videoService, fileStorageService);
        order.verify(videoService).releaseAbandoned(timeout.plus(VideoJobWorker.ABANDON_MARGIN));
        order.verify(fileStorageService).deleteFileById("given-up.mov");
        order.verify(videoService).claimNext();
    }

    @Test
    void severalWaitingVideos_AreAllConvertedInOneRound() throws IOException {
        VideoJob second = new VideoJob("second.mp4", "second.avi", LocalDateTime.now());
        second.setId(2L);
        when(videoService.claimNext()).thenReturn(Optional.of(job), Optional.of(second), Optional.empty());
        when(videoService.complete(any())).thenReturn(true);

        worker.work();

        verify(fileStorageService).moveIn(any(), eq("result.mp4"));
        verify(fileStorageService).moveIn(any(), eq("second.mp4"));
    }
}
