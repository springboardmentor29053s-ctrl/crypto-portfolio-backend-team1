package com.crypto.portfolio.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateExchangeRequest {
// add exchanges
    private String name;
    private String baseUrl;
}