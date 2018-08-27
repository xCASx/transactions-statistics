package com.n26.controller.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class TimeoutTimestampValidator implements ConstraintValidator<TimeoutTimestampConstraint, ZonedDateTime> {

    private static final long TIME_CONSTRAINT = MINUTES.toMillis(1L);

    @Override
    public void initialize(final TimeoutTimestampConstraint constraintAnnotation) {
    }

    /**
     * Validate with accuracy up to millis
     */
    @Override
    public boolean isValid(final ZonedDateTime timestampField, final ConstraintValidatorContext context) {
        ZonedDateTime currentZonedDateTime = OffsetDateTime.now(UTC).toZonedDateTime();
        long diff = MILLIS.between(timestampField, currentZonedDateTime);
        return diff < TIME_CONSTRAINT;
    }
}
