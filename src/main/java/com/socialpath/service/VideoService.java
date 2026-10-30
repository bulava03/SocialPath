package com.socialpath.service;

import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.entity.VideoJob;
import com.socialpath.media.PublicationMediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Videos of posts and comments: accepting uploads, the queue of videos that
 * have to be converted to MP4 first, and their status for the feed.
 */
public interface VideoService {

    /** Longest video a post may carry. */
    Duration MAX_DURATION = Duration.ofMinutes(10);

    /** Error for a video over MAX_DURATION. */
    String TOO_LONG = "A video must not be longer than 10 minutes.";

    /**
     * What the feed shows for a video; waiting and converting look the same to the reader.
     * @param jobStatus the conversion status, null for a video that was never queued
     * @return one of the PublicationPresentable.VIDEO_* values
     */
    static String displayStatus(VideoJob.Status jobStatus) {
        if (jobStatus == null || jobStatus == VideoJob.Status.READY) {
            return PublicationPresentable.VIDEO_READY;
        }
        return jobStatus == VideoJob.Status.FAILED
                ? PublicationPresentable.VIDEO_FAILED : PublicationPresentable.VIDEO_PROCESSING;
    }

    /**
     * Stores an uploaded video. One browsers play as it is (H.264/AAC MP4 or
     * VP8/VP9/AV1 WebM, no larger than the size limit) is published right
     * away; any other is queued for conversion.
     * @param upload the uploaded file
     * @param type its detected format, a video one
     * @return the file id the publication references; for a queued video,
     *         the MP4 that does not exist until the conversion is done
     * @throws com.socialpath.exception.InvalidVideoException when the video
     *         is unreadable or too long; nothing is kept then
     */
    String accept(MultipartFile upload, PublicationMediaType type) throws IOException;

    /**
     * The conversion status of the given files, in one query. Files that were
     * never queued have no entry: they are ready.
     * @param fileIds file ids of videos
     * @return status by file id, for queued videos only
     */
    Map<String, VideoJob.Status> statusOf(Collection<String> fileIds);

    /**
     * Forgets the conversions of files that are being deleted. The caller
     * deletes the returned uploads together with the files themselves.
     * @param fileIds file ids of a deleted publication
     * @return the uploads still waiting to be converted
     */
    List<String> discard(Collection<String> fileIds);

    /**
     * Takes the oldest waiting job for conversion. Safe with several workers
     * at once: each gets a different job.
     * @return the job, now PROCESSING, or empty when nothing is waiting
     */
    Optional<VideoJob> claimNext();

    /**
     * Marks a job done.
     * @param job the job
     * @return false when the job no longer exists: its publication was
     *         deleted meanwhile and the result should be thrown away
     */
    boolean complete(VideoJob job);

    /**
     * Marks a job failed.
     * @param job the job
     * @param error what went wrong, for the log and the database
     */
    void fail(VideoJob job, String error);
}
