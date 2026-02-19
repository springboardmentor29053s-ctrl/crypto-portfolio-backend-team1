package com.crypto.portfolio.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ForgotPassword_dto {

    private String email;   // or username (we’ll use email)
}
