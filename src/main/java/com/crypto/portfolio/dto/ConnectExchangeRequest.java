package com.crypto.portfolio.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConnectExchangeRequest {

    private String exchangeName;
    private String apiKey;
    private String apiSecret;

}