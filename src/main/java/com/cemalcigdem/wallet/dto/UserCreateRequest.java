package com.cemalcigdem.wallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "fullName must not be blank")
    private String fullName;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    private String email;
}
