package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RiskAlertResponse {

    private String assetSymbol;
    private String alertType;
    private String details;
    private LocalDateTime createdAt;
}