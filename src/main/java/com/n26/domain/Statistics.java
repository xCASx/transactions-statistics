package com.n26.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static com.n26.controller.StatisticsController.VIEW_SCALE;
import static com.n26.repository.StatisticsRepository.DEFAULT_ROUNDING;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Statistics {
    @JsonFormat(shape = STRING)
    private final BigDecimal sum;

    @JsonFormat(shape = STRING)
    private final BigDecimal avg;

    @JsonFormat(shape = STRING)
    private final BigDecimal max;

    @JsonFormat(shape = STRING)
    private final BigDecimal min;

    private final long count;

    public Statistics(Statistics that) {
        this.sum = that.getSum().setScale(VIEW_SCALE, DEFAULT_ROUNDING);
        this.avg = that.getAvg().setScale(VIEW_SCALE, DEFAULT_ROUNDING);
        this.max = that.getMax().setScale(VIEW_SCALE, DEFAULT_ROUNDING);
        this.min = that.getMin().setScale(VIEW_SCALE, DEFAULT_ROUNDING);
        this.count = that.getCount();
    }
}
