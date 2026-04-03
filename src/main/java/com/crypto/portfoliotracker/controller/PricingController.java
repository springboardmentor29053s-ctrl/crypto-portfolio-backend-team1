package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.PriceSnapshot;
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

import java.util.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://10.14.189.34:3000"})
public class PricingController {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // CoinGecko API endpoints
    private static final String COINGECKO_BASE = "https://api.coingecko.com/api/v3";
    private static final String MARKET_DATA_URL = COINGECKO_BASE + "/coins/markets";
    private static final String HISTORICAL_DATA_URL = COINGECKO_BASE + "/coins/{id}/market_chart";

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }

    @GetMapping("/current/{symbol}")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(
            @PathVariable String symbol,
            Authentication authentication) {
        Map<String, Object> priceData = pricingService.getCurrentPrice(symbol);
        return ResponseEntity.ok(priceData);
    }

    @GetMapping("/historical/{symbol}")
    public ResponseEntity<Map<String, Object>> getHistoricalPrices(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        try {
            // Map symbol to CoinGecko ID
            String coinId = mapSymbolToCoinId(symbol);
            if (coinId == null) {
                return ResponseEntity.ok(getMockHistoricalData(symbol, days));
            }
            
            String url = HISTORICAL_DATA_URL.replace("{id}", coinId) + 
                        "?vs_currency=usd&days=" + days;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> data = response.getBody();
            
            if (data != null && data.containsKey("prices")) {
                return ResponseEntity.ok(data);
            } else {
                return ResponseEntity.ok(getMockHistoricalData(symbol, days));
            }
            
        } catch (Exception e) {
            // Fallback to mock data
            return ResponseEntity.ok(getMockHistoricalData(symbol, days));
        }
    }

    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolioPrices(
            @RequestParam List<String> symbols,
            Authentication authentication) {
        Map<String, Object> portfolioPrices = pricingService.getPortfolioPrices(symbols);
        return ResponseEntity.ok(portfolioPrices);
    }

    @GetMapping("/snapshots/{symbol}")
    public ResponseEntity<List<PriceSnapshot>> getRecentSnapshots(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int limit) {
        List<PriceSnapshot> snapshots = pricingService.getRecentSnapshots(symbol, limit);
        return ResponseEntity.ok(snapshots);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<PriceSnapshot>> getLatestPrices() {
        List<PriceSnapshot> latestPrices = pricingService.getLatestPrices();
        return ResponseEntity.ok(latestPrices);
    }

    @GetMapping("/market")
    public ResponseEntity<Map<String, Object>> getMarketData() {
        try {
            // Fetch top 100 cryptocurrencies by market cap
            String url = MARKET_DATA_URL + "?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false&price_change_percentage=24h";
            
            ResponseEntity<Object[]> response = restTemplate.getForEntity(url, Object[].class);
            Object[] coins = response.getBody();
            
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> prices = new ArrayList<>();
            
            if (coins != null) {
                for (Object coin : coins) {
                    if (coin instanceof Map) {
                        Map<String, Object> coinData = (Map<String, Object>) coin;
                        Map<String, Object> priceData = new HashMap<>();
                        
                        priceData.put("symbol", coinData.get("symbol"));
                        priceData.put("usd", coinData.get("current_price"));
                        priceData.put("usd_24h_change", coinData.get("price_change_percentage_24h"));
                        priceData.put("usd_market_cap", coinData.get("market_cap"));
                        priceData.put("usd_volume_24h", coinData.get("total_volume"));
                        priceData.put("name", coinData.get("name"));
                        priceData.put("id", coinData.get("id"));
                        
                        prices.add(priceData);
                    }
                }
            }
            
            result.put("prices", prices);
            result.put("lastUpdated", new Date());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // Fallback to mock data if API fails
            return ResponseEntity.ok(getMockMarketData());
        }
    }

    @PostMapping("/refresh/{symbol}")
    public ResponseEntity<Map<String, Object>> refreshPrice(@PathVariable String symbol) {
        Map<String, Object> refreshedPrice = pricingService.getCurrentPrice(symbol);
        return ResponseEntity.ok(refreshedPrice);
    }

    @PostMapping("/refresh/portfolio")
    public ResponseEntity<Map<String, Object>> refreshPortfolioPrices(
            @RequestParam List<String> symbols,
            Authentication authentication) {
        Map<String, Object> refreshedPrices = pricingService.getPortfolioPrices(symbols);
        return ResponseEntity.ok(refreshedPrices);
    }

    private String mapSymbolToCoinId(String symbol) {
        Map<String, String> symbolToId = new HashMap<>();
        symbolToId.put("BTC", "bitcoin");
        symbolToId.put("ETH", "ethereum");
        symbolToId.put("BNB", "binancecoin");
        symbolToId.put("ADA", "cardano");
        symbolToId.put("SOL", "solana");
        symbolToId.put("XRP", "ripple");
        symbolToId.put("DOT", "polkadot");
        symbolToId.put("DOGE", "dogecoin");
        symbolToId.put("MATIC", "matic-network");
        symbolToId.put("AVAX", "avalanche-2");
        symbolToId.put("LINK", "chainlink");
        symbolToId.put("UNI", "uniswap");
        symbolToId.put("ATOM", "cosmos");
        symbolToId.put("LTC", "litecoin");
        symbolToId.put("BCH", "bitcoin-cash");
        symbolToId.put("ETC", "ethereum-classic");
        symbolToId.put("TRX", "tron");
        symbolToId.put("XLM", "stellar");
        symbolToId.put("VET", "vechain");
        symbolToId.put("THETA", "theta-token");
        symbolToId.put("FIL", "filecoin");
        symbolToId.put("AAVE", "aave");
        symbolToId.put("COMP", "compound");
        symbolToId.put("MKR", "maker");
        symbolToId.put("SUSHI", "sushi");
        symbolToId.put("CRV", "curve-dao-token");
        
        return symbolToId.get(symbol.toUpperCase());
    }

    private Map<String, Object> getMockMarketData() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> prices = new ArrayList<>();
        
        // Generate some realistic mock data with variations
        Random random = new Random();
        String[] symbols = {"BTC", "ETH", "BNB", "ADA", "SOL", "XRP", "DOT", "AVAX", "LINK", "UNI"};
        
        for (String symbol : symbols) {
            Map<String, Object> priceData = new HashMap<>();
            double basePrice = 100 + random.nextDouble() * 1000;
            double change = (random.nextDouble() - 0.5) * 20; // -10% to +10%
            
            priceData.put("symbol", symbol);
            priceData.put("usd", basePrice);
            priceData.put("usd_24h_change", change);
            priceData.put("usd_market_cap", basePrice * 1000000);
            priceData.put("usd_volume_24h", basePrice * 10000);
            priceData.put("name", symbol + " Token");
            priceData.put("id", symbol.toLowerCase());
            
            prices.add(priceData);
        }
        
        result.put("prices", prices);
        result.put("lastUpdated", new Date());
        
        return result;
    }

    private Map<String, Object> getMockHistoricalData(String symbol, int days) {
        Map<String, Object> result = new HashMap<>();
        List<Object[]> prices = new ArrayList<>();
        
        Random random = new Random();
        double basePrice = 100 + random.nextDouble() * 1000;
        long now = System.currentTimeMillis();
        
        for (int i = days; i >= 0; i--) {
            long timestamp = now - (i * 24L * 60 * 60 * 1000);
            double variation = (random.nextDouble() - 0.5) * 0.1;
            double price = basePrice * (1 + variation);
            prices.add(new Object[]{timestamp, price});
        }
        
        result.put("prices", prices);
        return result;
    }
}
