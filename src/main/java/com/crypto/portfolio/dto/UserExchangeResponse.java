package com.crypto.portfolio.dto;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserExchangeResponse {
    private Long id;

    private String exchangeName;
    private boolean active;


}
