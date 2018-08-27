package com.n26.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.n26.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Repository
public class TransactionRepository {

    private static final int INVALIDATION_POOL_SIZE = 4;
    private static final long TTL = Duration.of(1, MINUTES).toMillis();
    private static final ScheduledExecutorService INVALIDATION_POOL = newScheduledThreadPool(INVALIDATION_POOL_SIZE);

    private StatisticsRepository statisticsRepository;

    private final RemovalListener<UUID, Transaction> REMOVAL_LISTENER = notification -> {
        statisticsRepository.removeTransaction(notification.getValue());
        log.debug("Transaction removed {}", notification.getKey());
    };

    private final Cache<UUID, Transaction> transactionCache = CacheBuilder.newBuilder()
            .removalListener(REMOVAL_LISTENER)
            .build();

    @Autowired
    public TransactionRepository(final StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    public void insert(final Transaction transaction) {
        UUID key = UUID.randomUUID();
        transactionCache.put(key, transaction);
        statisticsRepository.addTransaction(transaction);
        scheduleForRemoval(transaction, key);
        log.debug("Transaction added {}", key);
    }

    private void scheduleForRemoval(Transaction transaction, UUID key) {
        ZonedDateTime currentZonedDateTime = OffsetDateTime.now(UTC).toZonedDateTime();
        long diff = TTL - MILLIS.between(transaction.getTimestamp(), currentZonedDateTime);
        INVALIDATION_POOL.schedule(() -> transactionCache.invalidate(key), diff, MILLISECONDS);
    }

    public void clear() {
        log.debug("Invalidating all caches");
        transactionCache.invalidateAll();
        statisticsRepository.wipeStatistics();
        transactionCache.cleanUp();
    }
}
