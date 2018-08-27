package com.n26.controller;

import com.n26.domain.Transaction;
import com.n26.service.StatisticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@AllArgsConstructor
@RestController
public class TransactionsController {

    private final StatisticsService statisticsService;

    /**
     * 201 – in case of success
     * 204 – if the transaction is older than 60 seconds
     * 400 – if the JSON is invalid
     * 422 – if any of the fields are not parsable
     * 422 – if the transaction date is in the future
     *
     * 422 - if transaction amount is negative
     */
    @RequestMapping(method = POST)
    public ResponseEntity postTransaction(@RequestBody @Valid final Transaction transaction) {
        statisticsService.add(transaction);
        return ResponseEntity.status(CREATED).body(null);
    }

    @RequestMapping(method = DELETE)
    public ResponseEntity deleteTransactions() {
        statisticsService.wipeStatistics();
        return ResponseEntity.status(NO_CONTENT).body(null);
    }
}
