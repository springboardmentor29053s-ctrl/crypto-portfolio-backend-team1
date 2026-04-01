package com.crypto.portfolio.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse_dto {

    private String token;
    private String type;
    private String username;
}
