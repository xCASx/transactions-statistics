package com.n26.controller.validation;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeoutTimestampValidatorTest {

    @Test
    public void shouldPassValidation() {
        ZonedDateTime now = OffsetDateTime.now(UTC).toZonedDateTime();
        Fixture fixture = new Fixture();

        boolean result = fixture.validator.isValid(now, null);

        assertTrue("Current time should not be considered as expired", result);
    }

    @Test
    public void shouldFailValidation() {
        ZonedDateTime now = OffsetDateTime.now(UTC).toZonedDateTime().minus(61L, SECONDS);
        Fixture fixture = new Fixture();

        boolean result = fixture.validator.isValid(now, null);

        assertFalse("Outdated time should be considered as expired", result);
    }

    private static final class Fixture {
        TimeoutTimestampValidator validator = new TimeoutTimestampValidator();
    }
}