package com.n26.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.n26.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Repository
public class TimestampTransactionRepository {

    private static final ScheduledExecutorService INVALIDATION_POOL = newScheduledThreadPool(4);

    private StatisticsRepository statisticsRepository;

    private final RemovalListener<UUID, Transaction> REMOVAL_LISTENER = notification -> {
        statisticsRepository.removeTransaction(notification.getValue());
        log.debug("Transaction removed {}", notification.getKey());
    };

    private final Cache<UUID, Transaction> transactionCache = CacheBuilder.newBuilder()
//            .expireAfterWrite(1, MINUTES)
            .removalListener(REMOVAL_LISTENER)
            .build();

    @Autowired
    public TimestampTransactionRepository(final StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
        // Separate thread responsible for regular clean-ups of evicted transactions
//        ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
//        executor.scheduleAtFixedRate(transactionCache::cleanUp, 1, 1, MILLISECONDS);
    }

    public void insert(final Transaction transaction) {
        UUID key = UUID.randomUUID();
        transactionCache.put(key, transaction);
        statisticsRepository.addTransaction(transaction);

        ZonedDateTime currentZonedDateTime = OffsetDateTime.now(UTC).toZonedDateTime();
        long diff = MILLIS.between(transaction.getTimestamp(), currentZonedDateTime);
        INVALIDATION_POOL.schedule(() -> {
            transactionCache.invalidate(key);
//            log.info("Transaction invalidated after {}", transaction.retrievalTime);
        }, diff, MILLISECONDS);
        log.debug("Transaction added {}", key);
    }

    public void clear() {
        transactionCache.invalidateAll();
        transactionCache.cleanUp();
    }
}
