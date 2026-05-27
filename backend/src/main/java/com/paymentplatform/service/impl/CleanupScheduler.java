package com.paymentplatform.service.impl;

import com.paymentplatform.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupScheduler {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * Purge expired idempotency records every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredIdempotencyRecords() {
        int deleted = idempotencyRecordRepository.deleteExpiredRecords(ZonedDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired idempotency records", deleted);
        }
    }

    /**
     * Log system health metrics every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    public void logSystemMetrics() {
        Runtime rt = Runtime.getRuntime();
        long usedMb  = (rt.totalMemory() - rt.freeMemory()) / 1_048_576;
        long totalMb = rt.totalMemory() / 1_048_576;
        log.debug("JVM memory: {}MB used / {}MB total, threads: {}",
            usedMb, totalMb, Thread.activeCount());
    }
}
