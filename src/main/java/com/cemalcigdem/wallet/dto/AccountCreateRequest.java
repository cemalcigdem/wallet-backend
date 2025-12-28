package com.cemalcigdem.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank(message = "currency must not be blank")
        @Size(min = 3, max = 3, message = "currency must be 3 characters")
        String currency) {
}