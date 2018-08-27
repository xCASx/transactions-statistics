package com.n26.service;

import com.n26.domain.Statistics;
import com.n26.domain.Transaction;
import com.n26.repository.StatisticsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class StatisticsService {
    private final StatisticsRepository statisticsRepository;

    public Statistics getStatistics() {
        return statisticsRepository.getStatistics();
    }

    public void add(final Transaction transaction) {
        statisticsRepository.insert(transaction);
    }

    public void wipeStatistics() {
        statisticsRepository.wipe();
    }
}
