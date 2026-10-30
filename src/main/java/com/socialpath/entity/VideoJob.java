package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A video waiting to be converted to MP4, or the record of that conversion.
 * The publication references fileId from the start; until the job is READY
 * that file does not exist yet and the feed shows a placeholder instead.
 */
@Data
@Entity
@Table(name = "video_jobs")
@NoArgsConstructor
public class VideoJob {

    /** Where a job is in its life. */
    public enum Status {
        /** Waiting for a worker. */
        PENDING,
        /** A worker is converting it. */
        PROCESSING,
        /** Converted; fileId exists. */
        READY,
        /** Could not be converted. */
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The MP4 the publication shows once the job is done. */
    @Column(name = "file_id", length = 64, nullable = false, unique = true)
    private String fileId;

    /** The uploaded video, deleted after a successful conversion. */
    @Column(name = "source_file_id", length = 64, nullable = false)
    private String sourceFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private Status status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "error", length = 255)
    private String error;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * A new job waiting for a worker.
     * @param fileId the MP4 to produce
     * @param sourceFileId the stored upload
     * @param now the current time
     */
    public VideoJob(String fileId, String sourceFileId, LocalDateTime now) {
        this.fileId = fileId;
        this.sourceFileId = sourceFileId;
        this.status = Status.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
