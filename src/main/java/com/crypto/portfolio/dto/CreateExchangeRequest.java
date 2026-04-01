package com.crypto.portfolio.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateExchangeRequest {

    private String name;
    private String baseUrl;
}