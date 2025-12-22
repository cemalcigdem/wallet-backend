package com.cemalcigdem.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {
    private String fullName;
    private String email;
}
