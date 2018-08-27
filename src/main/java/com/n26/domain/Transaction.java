package com.n26.domain;

import com.n26.controller.validation.FutureTimestampConstraint;
import com.n26.controller.validation.TimeoutTimestampConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @DecimalMin("0.00")
    private BigDecimal amount;

    @FutureTimestampConstraint
    @TimeoutTimestampConstraint
    private ZonedDateTime timestamp;
}
