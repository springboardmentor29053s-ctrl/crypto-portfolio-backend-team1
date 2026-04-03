package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PricingController {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // CoinGecko API URLs
    private static final String BASE_URL = "https://api.coingecko.com/api/v3";
    private static final String HISTORICAL_DATA_URL = BASE_URL + "/coins/{id}/market_chart";
    private static final String MARKET_DATA_URL = BASE_URL + "/coins/markets";

    /**
     * Get current price for a symbol
     */
    @GetMapping("/current/{symbol}")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(@PathVariable String symbol) {
        try {
            Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
            return ResponseEntity.ok(priceData);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch price for " + symbol);
            error.put("timestamp", new Date());
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get market data for multiple symbols
     */
    @GetMapping("/market")
    public ResponseEntity<Map<String, Object>> getMarketData() {
        try {
            Map<String, Object> marketData = pricingService.getMarketData();
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            System.err.println("Error fetching market data: " + e.getMessage());
            return ResponseEntity.ok(getMarketDataFromAPI());
        }
    }

    /**
     * Get historical price data
     */
    @GetMapping("/historical/{symbol}")
    public ResponseEntity<Map<String, Object>> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> historicalData = pricingService.getHistoricalPrices(symbol, days);
            return ResponseEntity.ok(historicalData);
        } catch (Exception e) {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) {
                return ResponseEntity.ok(Map.of("error", "No data available for symbol: " + symbol));
            }
            
            String url = HISTORICAL_DATA_URL.replace("{id}", coinId) + 
                        "?vs_currency=usd&days=" + days;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> data = response.getBody();
            
            if (data != null && data.containsKey("prices")) {
                return ResponseEntity.ok(data);
            } else {
                return ResponseEntity.ok(Map.of("error", "No data available for symbol: " + symbol));
            }
        }
    }

    /**
     * Refresh price for a symbol
     */
    @PostMapping("/refresh/{symbol}")
    public ResponseEntity<Map<String, Object>> refreshPrice(@PathVariable String symbol) {
        try {
            Map<String, Object> refreshedPrice = pricingService.getCurrentPrice(symbol);
            return ResponseEntity.ok(refreshedPrice);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to refresh price for " + symbol);
            error.put("timestamp", new Date());
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get portfolio prices
     */
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolioPrices(
            @RequestParam List<String> symbols,
            Authentication authentication) {
        try {
            Map<String, Object> portfolioPrices = pricingService.getPortfolioPrices(symbols);
            return ResponseEntity.ok(portfolioPrices);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch portfolio prices");
            error.put("timestamp", new Date());
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Get current user ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getId();
        }
        return null;
    }

    /**
     * Map symbol to CoinGecko ID
     */
    private String getCoinGeckoId(String symbol) {
        Map<String, String> symbolToId = new HashMap<>();
        symbolToId.put("BTC", "bitcoin");
        symbolToId.put("ETH", "ethereum");
        symbolToId.put("BNB", "binancecoin");
        symbolToId.put("ADA", "cardano");
        symbolToId.put("SOL", "solana");
        symbolToId.put("XRP", "ripple");
        symbolToId.put("DOT", "polkadot");
        symbolToId.put("AVAX", "avalanche-2");
        symbolToId.put("LINK", "chainlink");
        symbolToId.put("UNI", "uniswap");
        symbolToId.put("MATIC", "polygon");
        symbolToId.put("ATOM", "cosmos");
        symbolToId.put("LTC", "litecoin");
        symbolToId.put("VET", "vechain");
        symbolToId.put("FIL", "filecoin");
        symbolToId.put("AAVE", "aave");
        symbolToId.put("COMP", "compound");
        symbolToId.put("MKR", "maker");
        symbolToId.put("SUSHI", "sushi");
        symbolToId.put("CRV", "curve-dao-token");
        
        return symbolToId.get(symbol.toUpperCase());
    }

    private Map<String, Object> getMarketDataFromAPI() {
        try {
            String url = MARKET_DATA_URL + "?vs_currency=usd&order=market_cap_desc&per_page=100&page=1";
            Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);
            
            Map<String, Object> result = new HashMap<>();
            if (response != null) {
                result.put("prices", java.util.Arrays.asList(response));
                result.put("lastUpdated", new Date());
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error fetching market data: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to fetch market data");
            errorResult.put("lastUpdated", new Date());
            return errorResult;
        }
    }
}
