-- Videos browsers cannot play as uploaded wait here to be converted to MP4.
-- The publication already references file_id (the MP4 to be produced);
-- source_file_id is the upload, removed once the conversion is done.
-- Workers take the oldest PENDING job with SELECT ... FOR UPDATE SKIP LOCKED,
-- so several application instances never pick the same job.
CREATE TABLE video_jobs (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    file_id        VARCHAR(64)  NOT NULL,
    source_file_id VARCHAR(64)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    attempts       INT          NOT NULL DEFAULT 0,
    error          VARCHAR(255) NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_video_jobs_file UNIQUE (file_id),
    KEY idx_video_jobs_status (status, id),
    CONSTRAINT chk_video_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
