package com.crypto.portfolio.dto;

import lombok.Data;

import java.util.List;

@Data
public class CryptoCoin_dto {

    private String id;
    private String symbol;
    private String name;
    private String image;

    private Double current_price;
    private Double market_cap;
    private Integer market_cap_rank;

    private Double total_volume;
    private Double price_change_percentage_24h;
    private Double price_change_percentage_7d_in_currency;

    private Sparkline sparkline_in_7d;

    @Data
    public static class Sparkline {
        private List<Double> price;
    }
}
