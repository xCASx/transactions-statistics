package com.n26.controller.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

public class TimeValidator implements ConstraintValidator<TimeConstraint, ZonedDateTime> {
    @Override
    public void initialize(TimeConstraint constraintAnnotation) {
    }

    /**
     * Validate with accuracy up to seconds
     *
     * @param timestampField
     * @param constraintValidatorContext
     * @return
     */
    @Override
    public boolean isValid(ZonedDateTime timestampField, ConstraintValidatorContext constraintValidatorContext) {
        ZonedDateTime currentZonedDateTime = OffsetDateTime.now(UTC).toZonedDateTime();
        long diff = SECONDS.between(timestampField, currentZonedDateTime);
        return diff >= 0 && !(diff > 60 * 60 * 24);
    }
}
