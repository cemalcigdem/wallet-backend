package com.cemalcigdem.wallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank(message = "fullName must not be blank")
        String fullName,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be valid")
        String email
) {
}
