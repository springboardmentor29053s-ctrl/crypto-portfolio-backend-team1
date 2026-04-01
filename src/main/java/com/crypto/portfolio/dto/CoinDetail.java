package com.crypto.portfolio.dto;

import lombok.Data;

@Data
public class CoinDetail {

    private String id;
    private String symbol;
    private String name;
    private Image image;
    private MarketData market_data;

    @Data
    public static class Image {
        private String large;
    }

    @Data
    public static class MarketData {

        private PriceObject current_price;
        private PriceObject market_cap;
        private PriceObject total_volume;

        private PriceObject ath;
        private PriceObject ath_change_percentage;
        private DateObject ath_date;

        private Double circulating_supply;
        private Double total_supply;
        private Double max_supply;
        private Double price_change_percentage_24h;
    }

    @Data
    public static class PriceObject {
        private Double usd;
    }
    @Data
    public static class DateObject {
        private String usd;
    }
}