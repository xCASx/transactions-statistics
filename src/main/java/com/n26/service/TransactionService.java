package com.n26.service;

import com.n26.domain.Transaction;
import com.n26.repository.TimestampTransactionRepository;
import com.n26.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class TransactionService {
//    private final TransactionRepository transactionRepository;
    private final TimestampTransactionRepository transactionRepository;

    public void create(final Transaction transaction) {
        transactionRepository.insert(transaction);
    }

    public void delete() {
        transactionRepository.clear();
    }
}
