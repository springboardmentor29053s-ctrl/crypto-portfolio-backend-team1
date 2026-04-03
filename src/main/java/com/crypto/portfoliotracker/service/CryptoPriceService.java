package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
import com.crypto.portfoliotracker.repository.PriceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CryptoPriceService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    public BigDecimal getCurrentPrice(String symbol) {
        try {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) {
                return BigDecimal.ZERO;
            }
            
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey(coinId)) {
                Map<String, Object> coinData = (Map<String, Object>) response.get(coinId);
                return new BigDecimal(coinData.get("usd").toString());
            }
        } catch (Exception e) {
            System.err.println("Error fetching price for " + symbol + ": " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }

    public List<Map<String, Object>> getAllMarketData() {
        try {
            String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1";
            Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);
            
            if (response != null) {
                return Arrays.asList(response);
            }
        } catch (Exception e) {
            System.err.println("Error fetching market data: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    public Map<String, Object> getMarketData() {
        List<Map<String, Object>> marketData = getAllMarketData();
        
        Map<String, Object> result = new HashMap<>();
        result.put("prices", marketData);
        result.put("lastUpdated", new Date());
        
        return result;
    }

    public Map<String, Object> getHistoricalData(String symbol, int days) {
        try {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) {
                return Map.of("error", "No data available for symbol: " + symbol);
            }
            
            String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart?vs_currency=usd&days=" + days;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("prices")) {
                return response;
            }
        } catch (Exception e) {
            System.err.println("Error fetching historical data: " + e.getMessage());
        }
        
        return Map.of("error", "Failed to fetch historical data");
    }

    public Map<String, Object> getPortfolioPrices(List<String> symbols) {
        Map<String, Object> portfolioPrices = new HashMap<>();
        
        for (String symbol : symbols) {
            BigDecimal price = getCurrentPrice(symbol);
            portfolioPrices.put(symbol, Map.of(
                "price", price,
                "timestamp", LocalDateTime.now(),
                "source", "CoinGecko"
            ));
        }
        
        return portfolioPrices;
    }

    public void savePriceSnapshot(String symbol, BigDecimal price) {
        try {
            PriceSnapshot snapshot = new PriceSnapshot();
            snapshot.setAssetSymbol(symbol);
            snapshot.setPriceUsd(price);
            snapshot.setCapturedAt(LocalDateTime.now());
            snapshot.setSource("CoinGecko");
            
            priceSnapshotRepository.save(snapshot);
        } catch (Exception e) {
            System.err.println("Error saving price snapshot: " + e.getMessage());
        }
    }

    private String getCoinGeckoId(String symbol) {
        // Map common symbols to CoinGecko IDs
        Map<String, String> symbolMap = new HashMap<>();
        symbolMap.put("BTC", "bitcoin");
        symbolMap.put("ETH", "ethereum");
        symbolMap.put("BNB", "binancecoin");
        symbolMap.put("ADA", "cardano");
        symbolMap.put("SOL", "solana");
        symbolMap.put("XRP", "ripple");
        symbolMap.put("DOT", "polkadot");
        symbolMap.put("AVAX", "avalanche-2");
        symbolMap.put("LINK", "chainlink");
        symbolMap.put("UNI", "uniswap");
        symbolMap.put("MATIC", "polygon");
        symbolMap.put("ATOM", "cosmos");
        symbolMap.put("LTC", "litecoin");
        symbolMap.put("VET", "vechain");
        symbolMap.put("FIL", "filecoin");
        symbolMap.put("TRX", "tron");
        symbolMap.put("ETC", "ethereum-classic");
        symbolMap.put("XLM", "stellar");
        
        return symbolMap.get(symbol.toUpperCase());
    }
}
