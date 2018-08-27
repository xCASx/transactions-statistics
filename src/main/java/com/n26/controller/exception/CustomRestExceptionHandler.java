package com.n26.controller.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Order(HIGHEST_PRECEDENCE)
@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatus status,
                                                                  final WebRequest request) {
        if (!(ex.getCause() instanceof JsonMappingException)) {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }

        String message = ex.getCause().getLocalizedMessage();
        if (!message.contains("BigDecimal") && !message.contains("ZonedDateTime")) {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }

        ApiError apiError = new ApiError(UNPROCESSABLE_ENTITY, "", message);
        return handleExceptionInternal(ex, apiError, headers, apiError.getHttpStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
                                                                  final HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  final WebRequest request) {
        List<String> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String message = error.getDefaultMessage();
            errors.add(error.getField() + ": " + message);

            if (message == null) continue;

            // TODO: is there a better way to determine failed constraint?
            if (message.contains("future") || message.contains("must be greater than or equal to 0.00")) {
                status = UNPROCESSABLE_ENTITY;
            } else if (message.contains("timed out")) {
                status = NO_CONTENT;
            }
        }

        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }

        ApiError apiError = new ApiError(status, ex.getLocalizedMessage(), errors);
        return handleExceptionInternal(ex, apiError, headers, apiError.getHttpStatus(), request);
    }

}
