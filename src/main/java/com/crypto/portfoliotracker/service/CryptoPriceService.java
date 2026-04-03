package com.crypto.portfoliotracker.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CryptoPriceService {

    private final RestTemplate restTemplate = new RestTemplate();

    public BigDecimal getCurrentPrice(String symbol) {
        // Mock implementation - in real app would call CoinGecko API
        Map<String, BigDecimal> mockPrices = Map.of(
            "BTC", new BigDecimal("45000000"),
            "ETH", new BigDecimal("3000"),
            "USDT", new BigDecimal("1"),
            "LINK", new BigDecimal("15"),
            "UNI", new BigDecimal("8")
        );
        
        return mockPrices.getOrDefault(symbol.toUpperCase(), BigDecimal.ZERO);
    }

    public List<Map<String, Object>> getAllMarketData() {
        // Mock market data - in real app would call CoinGecko API
        List<Map<String, Object>> marketData = new ArrayList<>();
        
        // BTC
        Map<String, Object> btc = new HashMap<>();
        btc.put("id", "bitcoin");
        btc.put("symbol", "btc");
        btc.put("market_cap", 850000000000.0);
        btc.put("price_change_percentage_24h", 2.5);
        marketData.add(btc);
        
        // ETH
        Map<String, Object> eth = new HashMap<>();
        eth.put("id", "ethereum");
        eth.put("symbol", "eth");
        eth.put("market_cap", 360000000000.0);
        eth.put("price_change_percentage_24h", -1.2);
        marketData.add(eth);
        
        // Small cap token (for testing low market cap alerts)
        Map<String, Object> smallCap = new HashMap<>();
        smallCap.put("id", "smalltoken");
        smallCap.put("symbol", "small");
        smallCap.put("market_cap", 5000000.0); // 5M USD - below 6B threshold
        smallCap.put("price_change_percentage_24h", 25.0); // High volatility
        marketData.add(smallCap);
        
        return marketData;
    }
}
