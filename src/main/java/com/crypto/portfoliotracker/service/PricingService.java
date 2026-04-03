package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
import com.crypto.portfoliotracker.repository.PriceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class PricingService {

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3";

    public Map<String, Object> getCurrentPrice(String symbol) {
        try {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "Symbol not found");
                return errorMap;
            }

            String url = COINGECKO_BASE_URL + "/simple/price?ids=" + coinId + 
                        "&vs_currencies=usd&include_market_cap=true&include_24hr_change=true";
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey(coinId)) {
                Map<String, Object> coinData = (Map<String, Object>) response.get(coinId);
                
                // Save price snapshot
                savePriceSnapshot(symbol, 
                    new BigDecimal(coinData.get("usd").toString()),
                    coinData.containsKey("usd_market_cap") ? 
                        new BigDecimal(coinData.get("usd_market_cap").toString()) : null,
                    "coingecko");
                
                return coinData;
            }
            
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Price data not available");
            return errorMap;
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to fetch price: " + e.getMessage());
            return errorMap;
        }
    }

    public Map<String, Object> getHistoricalPrices(String symbol, int days) {
        try {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "Symbol not found");
                return errorMap;
            }

            String url = COINGECKO_BASE_URL + "/coins/" + coinId + 
                        "/market_chart?vs_currency=usd&days=" + days;
            
            Map<String, Object> response = null;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (retryCount < maxRetries && response == null) {
                try {
                    response = restTemplate.getForObject(url, Map.class);
                } catch (Exception e) {
                    retryCount++;
                    System.err.println("Historical data API attempt " + retryCount + " failed for " + symbol + ": " + e.getMessage());
                    if (retryCount < maxRetries) {
                        Thread.sleep(1000); // Wait 1 second before retry
                    }
                }
            }
            
            if (response != null) {
                return response;
            }
            
            // Fallback to cached snapshots for historical data
            System.err.println("Historical API failed after " + maxRetries + " attempts, using cached snapshots for " + symbol);
            List<PriceSnapshot> snapshots = getRecentSnapshots(symbol, Math.min(days, 100));
            if (!snapshots.isEmpty()) {
                Map<String, Object> cachedResponse = new HashMap<>();
                List<Object> prices = new ArrayList<>();
                
                for (PriceSnapshot snapshot : snapshots) {
                    List<Object> pricePoint = new ArrayList<>();
                    pricePoint.add(snapshot.getCapturedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    pricePoint.add(snapshot.getPriceUsd());
                    prices.add(pricePoint);
                }
                
                cachedResponse.put("prices", prices);
                return cachedResponse;
            }
            
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Historical data not available");
            return errorMap;
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to fetch historical data: " + e.getMessage());
            return errorMap;
        }
    }

    public List<PriceSnapshot> getRecentSnapshots(String symbol, int limit) {
        return priceSnapshotRepository.findByAssetSymbolOrderByCapturedAtDesc(symbol, 
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public Map<String, Object> getPortfolioPrices(List<String> symbols) {
        Map<String, Object> result = new HashMap<>();
        
        // Batch request for multiple symbols
        StringBuilder coinIds = new StringBuilder();
        Map<String, String> symbolToIdMap = new HashMap<>();
        
        for (String symbol : symbols) {
            String coinId = getCoinGeckoId(symbol);
            if (coinId != null) {
                if (coinIds.length() > 0) coinIds.append(",");
                coinIds.append(coinId);
                symbolToIdMap.put(coinId, symbol);
            }
        }
        
        if (coinIds.length() == 0) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "No valid symbols found");
                return errorMap;
            }

            try {
                // Add timeout and connection settings for faster response
                long startTime = System.currentTimeMillis();
                
                String url = COINGECKO_BASE_URL + "/simple/price?ids=" + coinIds.toString() + 
                            "&vs_currencies=usd&include_market_cap=true&include_24hr_change=true";
                
                Map<String, Object> response = null;
                int maxRetries = 3;
                int retryCount = 0;
                
                while (retryCount < maxRetries && response == null) {
                    try {
                        response = restTemplate.getForObject(url, Map.class);
                    } catch (Exception e) {
                        retryCount++;
                        System.err.println("CoinGecko API attempt " + retryCount + " failed: " + e.getMessage());
                        if (retryCount < maxRetries) {
                            Thread.sleep(1000); // Wait 1 second before retry
                        }
                    }
                }
                
                long endTime = System.currentTimeMillis();
                System.out.println("CoinGecko API call completed in " + (endTime - startTime) + "ms");
                
                if (response != null) {
                    for (Map.Entry<String, Object> entry : response.entrySet()) {
                        String coinId = entry.getKey();
                        Object coinData = entry.getValue();
                        
                        // Map back to original symbol
                        String originalSymbol = symbolToIdMap.get(coinId);
                        if (originalSymbol != null) {
                            result.put(originalSymbol, coinData);
                            
                            // Save price snapshot asynchronously (non-blocking)
                            if (coinData instanceof Map) {
                                Map<String, Object> dataMap = (Map<String, Object>) coinData;
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        savePriceSnapshot(originalSymbol, 
                                            new BigDecimal(dataMap.get("usd").toString()),
                                            dataMap.containsKey("usd_market_cap") ? 
                                                new BigDecimal(dataMap.get("usd_market_cap").toString()) : null,
                                            "coingecko");
                                    } catch (Exception e) {
                                        System.err.println("Failed to save price snapshot for " + originalSymbol + ": " + e.getMessage());
                                    }
                                });
                            }
                        }
                    }
                } else {
                    // Fallback to cached data from database
                    System.err.println("CoinGecko API failed after " + maxRetries + " attempts, using cached data");
                    for (String symbol : symbols) {
                        List<PriceSnapshot> snapshots = getRecentSnapshots(symbol, 1);
                        if (!snapshots.isEmpty()) {
                            PriceSnapshot snapshot = snapshots.get(0);
                            Map<String, Object> cachedData = new HashMap<>();
                            cachedData.put("usd", snapshot.getPriceUsd());
                            cachedData.put("usd_market_cap", snapshot.getMarketCap());
                            cachedData.put("usd_24h_change", 0.0); // No 24h change in cached data
                            result.put(symbol, cachedData);
                        }
                    }
                }
                
                return result;
            } catch (Exception e) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", "Failed to fetch portfolio prices: " + e.getMessage());
                return errorMap;
            }
    }

    @Transactional
    public void savePriceSnapshot(String symbol, BigDecimal price, BigDecimal marketCap, String source) {
        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setAssetSymbol(symbol.toUpperCase());
        snapshot.setPriceUsd(price);
        snapshot.setMarketCap(marketCap);
        snapshot.setSource(source);
        snapshot.setCapturedAt(LocalDateTime.now());
        
        priceSnapshotRepository.save(snapshot);
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
        symbolMap.put("DOGE", "dogecoin");
        symbolMap.put("AVAX", "avalanche-2");
        symbolMap.put("MATIC", "matic-network");
        symbolMap.put("LINK", "chainlink");
        symbolMap.put("UNI", "uniswap");
        symbolMap.put("LTC", "litecoin");
        symbolMap.put("BCH", "bitcoin-cash");
        symbolMap.put("ATOM", "cosmos");
        symbolMap.put("VET", "vechain");
        symbolMap.put("FIL", "filecoin");
        symbolMap.put("TRX", "tron");
        symbolMap.put("ETC", "ethereum-classic");
        symbolMap.put("XLM", "stellar");
        
        return symbolMap.get(symbol.toUpperCase());
    }

    public List<PriceSnapshot> getLatestPrices() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        return priceSnapshotRepository.findLatestPrices(threshold);
    }

    public Map<String, Object> getMarketData() {
        Map<String, Object> marketData = new HashMap<>();
        
        try {
            // Fetch top 100 cryptocurrencies from CoinGecko
            String url = COINGECKO_BASE_URL + "/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false&price_change_percentage=24h";
            
            Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);
            
            if (response != null) {
                List<Map<String, Object>> prices = new ArrayList<>();
                
                for (Map<String, Object> coin : response) {
                    Map<String, Object> priceData = new HashMap<>();
                    priceData.put("symbol", coin.get("symbol").toString().toUpperCase());
                    priceData.put("usd", coin.get("current_price"));
                    priceData.put("usd_24h_change", coin.get("price_change_percentage_24h"));
                    priceData.put("usd_market_cap", coin.get("market_cap"));
                    priceData.put("usd_volume_24h", coin.get("total_volume"));
                    prices.add(priceData);
                }
                
                marketData.put("prices", prices);
                marketData.put("timestamp", System.currentTimeMillis());
            }
        } catch (Exception e) {
            // Fallback to database if API fails
            List<PriceSnapshot> snapshots = getLatestPrices();
            Map<String, Object> prices = new HashMap<>();
            
            for (PriceSnapshot snapshot : snapshots) {
                Map<String, Object> priceData = new HashMap<>();
                priceData.put("symbol", snapshot.getAssetSymbol());
                priceData.put("usd", snapshot.getPriceUsd());
                priceData.put("usd_24h_change", 0.0); // Not available in snapshots
                priceData.put("usd_market_cap", snapshot.getMarketCap());
                priceData.put("usd_volume_24h", 0.0); // Not available in snapshots
                prices.put(snapshot.getAssetSymbol(), priceData);
            }
            
            marketData.put("prices", prices);
            marketData.put("timestamp", System.currentTimeMillis());
        }
        
        return marketData;
    }
}
