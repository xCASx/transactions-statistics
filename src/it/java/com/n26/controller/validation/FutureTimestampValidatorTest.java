package com.n26.controller.validation;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.*;

public class FutureTimestampValidatorTest {

    @Test
    public void shouldPassValidation() {
        ZonedDateTime now = OffsetDateTime.now(UTC).toZonedDateTime();
        Fixture fixture = new Fixture();

        boolean result = fixture.validator.isValid(now, null);

        assertTrue("Past time should not be considered as expired", result);
    }

    @Test
    public void shouldFailValidation() {
        ZonedDateTime now = OffsetDateTime.now(UTC).toZonedDateTime().plus(5L, SECONDS);
        Fixture fixture = new Fixture();

        boolean result = fixture.validator.isValid(now, null);

        assertFalse("Future time should be considered as expired", result);
    }

    private static final class Fixture {
        FutureTimestampValidator validator = new FutureTimestampValidator();
    }
}