package com.n26.controller.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = TimeValidator.class)
@Target( { METHOD, FIELD })
@Retention(RUNTIME)
public @interface TimeConstraint {
    String message() default "Time validation constraint violated";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
