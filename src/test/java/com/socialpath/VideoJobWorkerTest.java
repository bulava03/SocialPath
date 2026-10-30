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
import java.time.LocalDateTime;
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

    @BeforeEach
    void setUp() {
        videoService = mock(VideoService.class);
        fileStorageService = mock(FileStorageService.class);
        videoTranscoder = mock(VideoTranscoder.class);
        worker = new VideoJobWorker(videoService, fileStorageService, videoTranscoder);
        job = new VideoJob("result.mp4", "upload.mov", LocalDateTime.now());
        job.setId(1L);
        when(videoService.claimNext()).thenReturn(Optional.of(job), Optional.empty());
        when(fileStorageService.pathOf(anyString())).thenAnswer(call -> Path.of(call.<String>getArgument(0)));
    }

    @Test
    void convertedVideo_IsStoredBeforeItIsMarkedReady_ThenTheUploadIsRemoved() throws IOException {
        when(videoService.complete(job)).thenReturn(true);

        worker.work();

        InOrder order = inOrder(videoTranscoder, fileStorageService, videoService);
        order.verify(videoTranscoder).toMp4(eq(Path.of("upload.mov")), any(), any());
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
    void failedConversion_IsRecorded_AndNothingIsStored() throws IOException {
        doThrow(new IOException("ffmpeg could not convert the video: Invalid data"))
                .when(videoTranscoder).toMp4(any(), any(), any());

        worker.work();

        verify(videoService).fail(job, "ffmpeg could not convert the video: Invalid data");
        verify(fileStorageService, never()).moveIn(any(), anyString());
        verify(videoService, never()).complete(any());
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
