package com.n26.controller.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private HttpStatus httpStatus;
    private String message;
    private List<String> errors;

    public ApiError(HttpStatus httpStatus, String message, String error) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.errors = Collections.singletonList(error);
    }
}
