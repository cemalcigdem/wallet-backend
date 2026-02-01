package com.cemalcigdem.wallet.controller;

import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.dto.BalanceChangeRequest;
import com.cemalcigdem.wallet.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{accountId}")
public class BalanceController {

    private final AccountService accountService;

    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody BalanceChangeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.ok(accountService.deposit(accountId, request, idempotencyKey));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody BalanceChangeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.ok(accountService.withdraw(accountId, request, idempotencyKey));
    }
}
