package com.n26.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class ValidTransaction {
    private BigDecimal amount;
    private ZonedDateTime timestamp;

    private ValidTransaction() {}
}
