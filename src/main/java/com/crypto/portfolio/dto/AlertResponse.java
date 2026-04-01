package com.crypto.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AlertResponse {

    private String type;
    private String symbol;
    private Double targetValue;
    private Boolean triggered;
}