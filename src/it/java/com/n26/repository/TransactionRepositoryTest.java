package com.n26.repository;

import com.n26.domain.Statistics;
import com.n26.domain.Transaction;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static com.n26.repository.StatisticsRepository.EMPTY_STATISTICS;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class TransactionRepositoryTest {

    @After
    public void tearDown() {
        Fixture fixture = new Fixture();
        fixture.statisticsRepository.wipe();
    }

    @Test
    public void shouldFillAndClearCacheOnRequest() {
        Fixture fixture = new Fixture();
        Transaction testTransaction = fixture.givenTransaction(OffsetDateTime.now(UTC).toZonedDateTime());

        fixture.statisticsRepository.insert(testTransaction);

        Statistics resultStatistics = fixture.statisticsRepository.getStatistics();
        BigDecimal expectedAmount = new BigDecimal("1.00");
        Statistics expectedStatistics = new Statistics(expectedAmount, expectedAmount, expectedAmount, expectedAmount, 1);

        assertEquals("Statistics should contain correct data", expectedStatistics, resultStatistics);

        fixture.statisticsRepository.wipe();

        resultStatistics = fixture.statisticsRepository.getStatistics();

        assertEquals("Statistics should be empty after cache wiping", EMPTY_STATISTICS, resultStatistics);
    }

    @Test
    public void shouldAutoRemoveExpiredTransaction() throws InterruptedException {
        Fixture fixture = new Fixture();
        ZonedDateTime stillValidTransaction = OffsetDateTime.now(UTC).toZonedDateTime().minus(59L, SECONDS);
        Transaction testTransaction = fixture.givenTransaction(stillValidTransaction);

        fixture.statisticsRepository.insert(testTransaction);

        Statistics resultStatistics = fixture.statisticsRepository.getStatistics();
        BigDecimal expectedAmount = new BigDecimal("1.00");
        Statistics expectedStatistics = new Statistics(expectedAmount, expectedAmount, expectedAmount, expectedAmount, 1);

        assertEquals("Statistics should contain correct data", expectedStatistics, resultStatistics);

        TimeUnit.SECONDS.sleep(1L);
        resultStatistics = fixture.statisticsRepository.getStatistics();

        assertEquals("Statistics should be empty after transaction expiration", EMPTY_STATISTICS, resultStatistics);
    }

    @Test
    public void shouldImmediatelyRemoveExpiredTransaction() throws InterruptedException {
        Fixture fixture = new Fixture();
        ZonedDateTime stillValidTransaction = OffsetDateTime.now(UTC).toZonedDateTime().minus(61L, SECONDS);
        Transaction testTransaction = fixture.givenTransaction(stillValidTransaction);

        fixture.statisticsRepository.insert(testTransaction);

        // still need to sleep a bit to overcome racing condition
        TimeUnit.MILLISECONDS.sleep(100L);
        Statistics resultStatistics = fixture.statisticsRepository.getStatistics();

        assertEquals("Statistics should be empty after transaction expiration", EMPTY_STATISTICS, resultStatistics);
    }

    @Test
    public void shouldCalculateProperStatistics() {
        Fixture fixture = new Fixture();
        Transaction t1 = fixture.givenTransaction(new BigDecimal("0.345234"));
        Transaction t2 = fixture.givenTransaction(new BigDecimal("563.67"));
        Transaction t3 = fixture.givenTransaction(new BigDecimal("0.0"));
        Transaction t4 = fixture.givenTransaction(new BigDecimal("12.455"));
        Transaction t5 = fixture.givenTransaction(new BigDecimal("10"));

        fixture.statisticsRepository.insert(t1);
        fixture.statisticsRepository.insert(t2);
        fixture.statisticsRepository.insert(t3);
        fixture.statisticsRepository.insert(t4);
        fixture.statisticsRepository.insert(t5);

        Statistics resultStatistics = fixture.statisticsRepository.getStatistics();
        Statistics expectedStatistics = new Statistics(
                new BigDecimal("586.47"),
                new BigDecimal("117.29"),
                new BigDecimal("563.67"),
                new BigDecimal("0.00"),
                5
        );

        assertEquals("Statistics should be empty after transaction expiration", expectedStatistics, resultStatistics);
    }

    private static final class Fixture {
        StatisticsRepository statisticsRepository;

        Fixture() {
            statisticsRepository = new StatisticsRepository();
        }

        Transaction givenTransaction(BigDecimal amount, ZonedDateTime zonedDateTime) {
            return new Transaction(amount, zonedDateTime);
        }

        Transaction givenTransaction(ZonedDateTime zonedDateTime) {
            return givenTransaction(new BigDecimal("1.00"), zonedDateTime);
        }

        Transaction givenTransaction(BigDecimal amount) {
            return givenTransaction(amount, OffsetDateTime.now(UTC).toZonedDateTime());
        }
    }
}