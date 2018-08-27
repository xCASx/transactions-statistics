package com.n26.controller;

import com.n26.domain.Statistics;
import com.n26.service.StatisticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@AllArgsConstructor
@RestController("/statistics")
public class StatisticsController {

    public static final int VIEW_SCALE = 2;

    private final StatisticsService statisticsService;

    @RequestMapping(method = GET)
    public ResponseEntity getStatistics() {
        Statistics statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
