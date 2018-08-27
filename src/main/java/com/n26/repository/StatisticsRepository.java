package com.n26.repository;

import com.n26.domain.Statistics;
import com.n26.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Repository
public class StatisticsRepository {

    public static final RoundingMode DEFAULT_ROUNDING = HALF_UP;
    public static final int CALC_SCALE = 12;
    public static final BigDecimal SCALED_ZERO = ZERO.setScale(2, HALF_UP);
    public static final Statistics EMPTY_STATISTICS =
            new Statistics(SCALED_ZERO, SCALED_ZERO, SCALED_ZERO, SCALED_ZERO, 0);

    private static final int INVALIDATION_POOL_SIZE = 4;
    private static final long TTL = Duration.of(1, MINUTES).toMillis();
    private static final ScheduledExecutorService INVALIDATION_POOL = newScheduledThreadPool(INVALIDATION_POOL_SIZE);
    private static final AtomicReference<Statistics> GOLDEN_TRUTH_STATISTICS = new AtomicReference<>(EMPTY_STATISTICS);
    private static final Deque<BigDecimal> MIN_STACK = new ArrayDeque<>();
    private static final Deque<BigDecimal> MAX_STACK = new ArrayDeque<>();

    private static final BinaryOperator<Statistics> MERGE_ADD = (prev, t) -> {
        long newCount = prev.getCount() + 1;

        BigDecimal sum = prev.getSum().add(t.getSum());
        BigDecimal avg = addToAverage(prev.getAvg().setScale(CALC_SCALE, HALF_UP),
                t.getAvg().setScale(CALC_SCALE, HALF_UP),
                newCount);
        BigDecimal max = prev.getMax().max(t.getMax());
        BigDecimal min = prev.getCount() == 0 ? t.getMin() : prev.getMin().min(t.getMin());

        return new Statistics(sum, avg, max, min, newCount);
    };

    private static final BinaryOperator<Statistics> MERGE_SUBTRACT = (prev, t) -> {
        long count = prev.getCount() - 1;
        if (count == 0) return EMPTY_STATISTICS;

        BigDecimal maxFromStack = MAX_STACK.peek();
        BigDecimal minFromStack = MIN_STACK.peek();

        BigDecimal sum = prev.getSum().subtract(t.getSum());
        BigDecimal avg = subtractFromAverage(prev.getAvg().setScale(CALC_SCALE, HALF_UP),
                t.getAvg().setScale(CALC_SCALE, HALF_UP),
                prev.getCount());
        BigDecimal max = maxFromStack == null ? ZERO : maxFromStack;
        BigDecimal min = minFromStack == null ? ZERO : minFromStack;

        return new Statistics(sum, avg, max, min, count);
    };

    private final Lock writeLock = new ReentrantLock();

    public Statistics getStatistics() {
        return new Statistics(GOLDEN_TRUTH_STATISTICS.get());
    }

    public void insert(final Transaction transaction) {
        addTransaction(transaction);
        scheduleForRemoval(transaction);
        log.debug("Transaction added {}", transaction);
    }

    private void scheduleForRemoval(Transaction transaction) {
        ZonedDateTime currentZonedDateTime = OffsetDateTime.now(UTC).toZonedDateTime();
        long diff = TTL - MILLIS.between(transaction.getTimestamp(), currentZonedDateTime);
        Runnable invalidateTransaction = () -> removeTransaction(transaction);
        INVALIDATION_POOL.schedule(invalidateTransaction, diff, MILLISECONDS);
    }

    public void wipe() {
        log.debug("Invalidating all caches");
        writeLock.lock();
        try {
            GOLDEN_TRUTH_STATISTICS.set(EMPTY_STATISTICS);
            MIN_STACK.clear();
            MAX_STACK.clear();
        } finally {
            writeLock.unlock();
        }
    }

    private void addTransaction(final Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        Statistics newTransactionStat = new Statistics(amount, amount, amount, amount, 1);

        writeLock.lock();
        try {
            GOLDEN_TRUTH_STATISTICS.accumulateAndGet(newTransactionStat, MERGE_ADD);
            if (MAX_STACK.isEmpty()) {
                MAX_STACK.push(amount);
            } else if (MAX_STACK.peek().compareTo(GOLDEN_TRUTH_STATISTICS.get().getMax()) < 0) {
                MAX_STACK.push(GOLDEN_TRUTH_STATISTICS.get().getMax());
            }
            if (MIN_STACK.isEmpty()) {
                MIN_STACK.push(amount);
            } else if (MIN_STACK.peek().compareTo(GOLDEN_TRUTH_STATISTICS.get().getMin()) > 0) {
                MIN_STACK.push(GOLDEN_TRUTH_STATISTICS.get().getMin());
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void removeTransaction(final Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        Statistics newTransactionStat = new Statistics(amount, amount, amount, amount, 1);

        writeLock.lock();
        try {
            BigDecimal maxFromStack = MAX_STACK.peek();
            BigDecimal minFromStack = MIN_STACK.peek();
            if (maxFromStack != null && maxFromStack.compareTo(newTransactionStat.getMax()) == 0) {
                MAX_STACK.pop();
            }
            if (minFromStack != null && minFromStack.compareTo(newTransactionStat.getMin()) == 0) {
                MIN_STACK.pop();
            }
            GOLDEN_TRUTH_STATISTICS.accumulateAndGet(newTransactionStat, MERGE_SUBTRACT);
        } finally {
            writeLock.unlock();
        }
    }

    // avg_new = avg_old + (value_new - avg_old) / size_new
    private static BigDecimal addToAverage(final BigDecimal prev, final BigDecimal t, final long count) {
        BigDecimal newSize = new BigDecimal(count).setScale(CALC_SCALE, HALF_UP);
        return prev.add((t.subtract(prev)).divide(newSize, DEFAULT_ROUNDING));
    }

    // avg_new = ((avg_old * size_old) - value_new) / (size_old - 1)
    private static BigDecimal subtractFromAverage(final BigDecimal prev, final BigDecimal t, final long count) {
        BigDecimal oldSize = new BigDecimal(count).setScale(CALC_SCALE, HALF_UP);
        BigDecimal newSize = new BigDecimal(count - 1).setScale(CALC_SCALE, HALF_UP);
        return ((prev.multiply(oldSize)).subtract(t)).divide(newSize, DEFAULT_ROUNDING);
    }
}
