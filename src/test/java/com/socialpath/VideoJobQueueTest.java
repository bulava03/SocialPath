package com.socialpath;

import com.socialpath.entity.VideoJob;
import com.socialpath.repository.VideoJobRepository;
import com.socialpath.service.VideoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The queue's claim query on MySQL: two workers taking jobs at the same time
 * must get different ones, and neither may wait for the other.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VideoJobQueueTest extends MySqlRepositoryTest {

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        videoJobRepository.deleteAll();
    }

    /** Locks the next job the way a worker does. */
    private Optional<VideoJob> lockNext() {
        LocalDateTime retryCutoff = LocalDateTime.now().minus(VideoService.RETRY_DELAY);
        return videoJobRepository.lockNextPending(retryCutoff);
    }

    private Long queue(String name) {
        return videoJobRepository.save(new VideoJob(name + ".mp4", name + ".mov", LocalDateTime.now())).getId();
    }

    @Test
    void lockNextPending_TakesTheOldestWaitingJob() {
        Long first = queue("first");
        queue("second");
        VideoJob done = videoJobRepository.findById(first).orElseThrow();
        done.setStatus(VideoJob.Status.READY);
        videoJobRepository.save(done);

        Optional<VideoJob> next = new TransactionTemplate(transactionManager)
                .execute(status -> lockNext());

        assertThat(next).map(VideoJob::getFileId).contains("second.mp4");
    }

    @Test
    void lockNextPending_AFailedJobWaitsBeforeItIsTriedAgain() {
        Long failed = queue("failed");
        queue("fresh");
        VideoJob job = videoJobRepository.findById(failed).orElseThrow();
        job.setAttempts(1);
        videoJobRepository.save(job);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // Just failed: the newer job goes first.
        assertThat(claimInTransaction(transaction)).isEqualTo("fresh.mp4");

        job.setUpdatedAt(LocalDateTime.now().minus(VideoService.RETRY_DELAY).minusSeconds(5));
        videoJobRepository.save(job);
        assertThat(claimInTransaction(transaction)).isEqualTo("failed.mp4");
    }

    private String claimInTransaction(TransactionTemplate transaction) {
        return transaction.execute(status -> lockNext().map(VideoJob::getFileId).orElse(null));
    }

    @Test
    void findByStatusAndUpdatedAtBefore_FindsOnlyJobsStuckLongEnough() {
        Long stuck = queue("stuck");
        Long busy = queue("busy");
        LocalDateTime now = LocalDateTime.now();
        for (Long id : List.of(stuck, busy)) {
            VideoJob job = videoJobRepository.findById(id).orElseThrow();
            job.setStatus(VideoJob.Status.PROCESSING);
            job.setUpdatedAt(id.equals(stuck) ? now.minusHours(1) : now);
            videoJobRepository.save(job);
        }

        List<VideoJob> found = videoJobRepository.findByStatusAndUpdatedAtBefore(
                VideoJob.Status.PROCESSING, now.minusMinutes(15));

        assertThat(found).extracting(VideoJob::getFileId).containsExactly("stuck.mp4");
    }

    @Test
    void twoWorkersAtOnce_GetDifferentJobs_WithoutWaiting() throws Exception {
        queue("first");
        queue("second");
        CountDownLatch firstHasLocked = new CountDownLatch(1);
        CountDownLatch secondHasClaimed = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            // The first worker locks a job and keeps its transaction open...
            Future<String> firstWorker = workers.submit(() -> transaction.execute(status -> {
                String fileId = lockNext().orElseThrow().getFileId();
                firstHasLocked.countDown();
                await(secondHasClaimed);
                return fileId;
            }));
            // ...while the second claims: it skips the locked row instead of waiting for it.
            await(firstHasLocked);
            Future<String> secondWorker = workers.submit(() -> transaction.execute(status ->
                    lockNext().map(VideoJob::getFileId).orElse(null)));
            String second = secondWorker.get(10, TimeUnit.SECONDS);
            secondHasClaimed.countDown();
            String first = firstWorker.get(10, TimeUnit.SECONDS);

            assertThat(first).isEqualTo("first.mp4");
            assertThat(second).isEqualTo("second.mp4");
        } finally {
            secondHasClaimed.countDown();
            workers.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The other worker did not get there in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
