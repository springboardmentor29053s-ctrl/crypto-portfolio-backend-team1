package com.crypto.cryptoPortfolio.dto;

public class OrderRequest {

    private String symbol;
    private String side;      // BUY or SELL
    private String type;      // MARKET
    private String quantity;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
}
