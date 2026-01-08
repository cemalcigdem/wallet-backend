package com.cemalcigdem.wallet.controller;

import com.cemalcigdem.wallet.dto.TransactionResponse;
import com.cemalcigdem.wallet.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{accountId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.listByAccount(accountId));
    }
}
