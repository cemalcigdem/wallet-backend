package com.cemalcigdem.wallet.controller;

import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.dto.BalanceChangeRequest;
import com.cemalcigdem.wallet.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{accountId}")
public class BalanceController {

    private final AccountService accountService;

    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody BalanceChangeRequest request,
            @RequestHeader(value = "Idempotency-Key") @NotBlank String idempotencyKey
    ) {
        String normalizedKey = idempotencyKey.trim();
        return ResponseEntity.ok(accountService.deposit(accountId, request, normalizedKey));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody BalanceChangeRequest request,
            @RequestHeader(value = "Idempotency-Key") @NotBlank String idempotencyKey
    ) {
        String normalizedKey = idempotencyKey.trim();
        return ResponseEntity.ok(accountService.withdraw(accountId, request, normalizedKey));
    }
}
