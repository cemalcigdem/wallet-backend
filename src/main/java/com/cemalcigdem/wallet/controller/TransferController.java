package com.cemalcigdem.wallet.controller;

import com.cemalcigdem.wallet.dto.TransferRequest;
import com.cemalcigdem.wallet.dto.TransferResponse;
import com.cemalcigdem.wallet.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts/{fromAccountId}/transfers")
public class TransferController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable Long fromAccountId,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        accountService.transfer(fromAccountId, request, idempotencyKey);
        return ResponseEntity.ok(accountService.transfer(fromAccountId, request, idempotencyKey));
    }
}
