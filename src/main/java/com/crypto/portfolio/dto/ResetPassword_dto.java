package com.crypto.portfolio.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPassword_dto {

    private String email;
    private String newPassword;
}
