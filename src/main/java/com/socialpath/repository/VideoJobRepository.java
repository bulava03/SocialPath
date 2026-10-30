package com.socialpath.repository;

import com.socialpath.entity.VideoJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {

    /**
     * Locks the oldest waiting job for the current transaction. Rows another
     * transaction has already locked are skipped rather than waited for, so
     * concurrent workers each get a different job (MySQL 8 SKIP LOCKED).
     * Must run inside a transaction; the lock lasts until it ends.
     * @return the job, or empty when nothing is waiting
     */
    @Query(value = """
            SELECT * FROM video_jobs
            WHERE status = 'PENDING'
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<VideoJob> lockNextPending();

    List<VideoJob> findByFileIdIn(Collection<String> fileIds);

    Optional<VideoJob> findByFileId(String fileId);
}
