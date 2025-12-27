package com.cemalcigdem.wallet.controller;

import com.cemalcigdem.wallet.dto.AccountCreateRequest;
import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @PathVariable Long userId,
            @Valid @RequestBody AccountCreateRequest request
    ) {
        AccountResponse created = accountService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.listByUser(userId));
    }
}