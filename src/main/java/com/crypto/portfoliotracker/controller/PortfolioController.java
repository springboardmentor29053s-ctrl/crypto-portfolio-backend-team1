package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.dto.TradeDTO;
import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.repository.TradeRepository;
import com.crypto.portfoliotracker.service.PortfolioService;
import com.crypto.portfoliotracker.service.RiskDetectionService;
import com.crypto.portfoliotracker.service.TradeRiskService;
import com.crypto.portfoliotracker.service.UserService;
import com.crypto.portfoliotracker.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://10.14.189.34:3000"})
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskDetectionService riskDetectionService;

    @Autowired
    private TradeRiskService tradeRiskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private TradeRepository tradeRepository;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Find user by email (username) and return ID
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<Holding>> getHoldings(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<Holding> holdings = portfolioService.getUserHoldings(userId);
        return ResponseEntity.ok(holdings);
    }

    @PostMapping("/holdings")
    public ResponseEntity<?> addHolding(@RequestBody Map<String, Object> holdingData, 
                                       Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Holding holding = portfolioService.addHolding(
                userId,
                (String) holdingData.get("assetSymbol"),
                new BigDecimal(holdingData.get("quantity").toString()),
                holdingData.get("avgCost") != null ? 
                    new BigDecimal(holdingData.get("avgCost").toString()) : BigDecimal.ZERO,
                (String) holdingData.getOrDefault("walletType", "wallet"),
                holdingData.get("exchangeId") != null ? 
                    Long.parseLong(holdingData.get("exchangeId").toString()) : null,
                (String) holdingData.get("address")
            );
            
            // Trigger risk analysis for the held asset
            User user = userService.getUserByEmail(((UserDetails) authentication.getPrincipal()).getUsername());
            riskDetectionService.checkCoinRisk(user, (String) holdingData.get("assetSymbol"));
            
            return ResponseEntity.ok(holding);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/holdings/{holdingId}")
    public ResponseEntity<?> updateHolding(@PathVariable Long holdingId,
                                          @RequestBody Map<String, Object> updateData,
                                          Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Holding holding = portfolioService.updateHolding(
                holdingId,
                new BigDecimal(updateData.get("quantity").toString()),
                new BigDecimal(updateData.get("avgCost").toString())
            );
            return ResponseEntity.ok(holding);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/holdings/{holdingId}")
    public ResponseEntity<?> deleteHolding(@PathVariable Long holdingId,
                                          Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            portfolioService.deleteHolding(holdingId, userId);
            return ResponseEntity.ok(Map.of("message", "Holding deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-holdings/{exchangeId}")
    public ResponseEntity<?> syncHoldingsFromExchange(
            @PathVariable Long exchangeId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            portfolioService.syncHoldingsFromExchange(userId, exchangeId);
            return ResponseEntity.ok(Map.of("message", "Holdings synced successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-trades/{apiKeyId}")
    public ResponseEntity<?> syncTradesFromExchange(
            @PathVariable Long apiKeyId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            // Get API key and sync trades
            List<com.crypto.portfoliotracker.entity.ApiKey> apiKeys = exchangeService.getUserApiKeys(userId);
            com.crypto.portfoliotracker.entity.ApiKey targetApiKey = apiKeys.stream()
                .filter(key -> key.getId().equals(apiKeyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("API Key not found"));
            
            List<Trade> trades = exchangeService.fetchRecentTrades(targetApiKey);
            // Save trades to database
            for (Trade trade : trades) {
                trade.setUser(userRepository.findById(userId).orElseThrow());
                trade.setExchange(targetApiKey.getExchange());
                tradeRepository.save(trade);
            }
            
            return ResponseEntity.ok(Map.of("message", "Trades synced successfully", "tradesCount", trades.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/test-trades")
    public ResponseEntity<String> testTrades(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.ok("Error: Authentication required");
            }
            
            Long userId = getCurrentUserId(authentication);
            User user = userRepository.findById(userId).orElse(null);
            
            if (user != null) {
                List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);
                return ResponseEntity.ok("User found: " + user.getId() + " - " + user.getEmail() + ". Trades count: " + trades.size());
            } else {
                return ResponseEntity.ok("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/add-sample-trades")
    public ResponseEntity<String> addSampleTrades(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok("User not found");
            }
            
            // Check if user already has trades
            List<Trade> existingTrades = tradeRepository.findByUserOrderByExecutedAtDesc(user);
            if (!existingTrades.isEmpty()) {
                return ResponseEntity.ok("User already has " + existingTrades.size() + " trades");
            }
            
            // Add sample trades
            for (int i = 0; i < 5; i++) {
                Trade trade = new Trade();
                trade.setUser(user);
                trade.setAssetSymbol(i % 2 == 0 ? "BTC" : "ETH");
                trade.setSide(i % 2 == 0 ? com.crypto.portfoliotracker.entity.Trade.TradeSide.BUY : com.crypto.portfoliotracker.entity.Trade.TradeSide.SELL);
                trade.setQuantity(new BigDecimal("0.1" + i));
                trade.setPrice(new BigDecimal(i % 2 == 0 ? "45000" : "3000"));
                trade.setFee(new BigDecimal("10"));
                trade.setExecutedAt(java.time.LocalDateTime.now().minusDays(i));
                trade.setExchange(null); // No exchange for sample data
                tradeRepository.save(trade);
            }
            
            return ResponseEntity.ok("Added 5 sample trades successfully");
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/test-simple")
    public ResponseEntity<String> testSimple() {
        return ResponseEntity.ok("SIMPLE TEST WORKING: " + java.time.LocalDateTime.now());
    }
    
    @GetMapping("/debug-user")
    public ResponseEntity<String> debugUser(Authentication authentication) {
        try {
            Long userId;
            if (authentication != null) {
                userId = getCurrentUserId(authentication);
            } else {
                return ResponseEntity.ok("Debug Error: Authentication required");
            }
            
            User user = userRepository.findById(userId).orElse(null);
            List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);
            return ResponseEntity.ok("Debug: User ID " + userId + " has " + trades.size() + " trades");
        } catch (Exception e) {
            return ResponseEntity.ok("Debug Error: " + e.getMessage());
        }
    }

    @GetMapping("/trades")
    public ResponseEntity<List<TradeDTO>> getTrades(Authentication authentication) {
        try {
            System.err.println("=== GET TRADES CALLED ===");
            if (authentication == null) {
                System.err.println("ERROR: Authentication is null");
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // Get the actual authenticated user
            Long userId = getCurrentUserId(authentication);
            System.err.println("User ID from auth: " + userId);
            
            User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                System.err.println("ERROR: User not found for ID: " + userId);
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            System.err.println("Found user: " + user.getEmail() + " (ID: " + user.getId() + ")");
            
            // Get trades for the authenticated user
            List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);
            System.err.println("Found raw trades count: " + trades.size());
            
            // Print each trade for debugging
            for (Trade trade : trades) {
                System.err.println("Trade: " + trade.getId() + " - " + trade.getAssetSymbol() + " " + trade.getSide() + " " + trade.getQuantity());
            }
            
            // Convert to DTOs
            List<TradeDTO> tradeDTOs = new ArrayList<>();
            for (Trade trade : trades) {
                TradeDTO dto = new TradeDTO();
                dto.setId(trade.getId());
                dto.setAssetSymbol(trade.getAssetSymbol());
                dto.setSide(trade.getSide().toString());
                dto.setQuantity(trade.getQuantity());
                dto.setPrice(trade.getPrice());
                dto.setFee(trade.getFee());
                dto.setExchangeName(trade.getExchange() != null ? trade.getExchange().getName() : "Unknown");
                dto.setExecutedAt(trade.getExecutedAt());
                tradeDTOs.add(dto);
            }
            
            System.err.println("Returning " + tradeDTOs.size() + " trade DTOs");
            return ResponseEntity.ok(tradeDTOs);
        } catch (Exception e) {
            System.err.println("Error in getTrades: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/trades")
    public ResponseEntity<?> addTrade(@RequestBody Map<String, Object> tradeData,
                                     Authentication authentication) {
        try {
            System.err.println("=== POST TRADE CALLED ===");
            if (authentication == null) {
                System.err.println("ERROR: Authentication is null in POST");
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }
            
            // Get the actual authenticated user
            Long userId = getCurrentUserId(authentication);
            System.err.println("POST: User ID from auth: " + userId);
            
            System.err.println("POST: Adding trade for user ID: " + userId);
            System.err.println("POST: Trade data: " + tradeData);
            
            Trade trade = portfolioService.addTrade(
                userId,
                (String) tradeData.get("assetSymbol"),
                (String) tradeData.get("side"),
                new BigDecimal(tradeData.get("quantity").toString()),
                new BigDecimal(tradeData.get("price").toString()),
                tradeData.get("fee") != null ? 
                    new BigDecimal(tradeData.get("fee").toString()) : BigDecimal.ZERO,
                Long.parseLong(tradeData.get("exchangeId").toString())
            );
            
            System.out.println("Controller: Trade added with ID: " + trade.getId());
            
            // Trigger risk analysis for the traded asset
            try {
                User user = userRepository.findById(userId).orElseThrow();
                System.out.println("Triggering risk analysis for asset: " + trade.getAssetSymbol());
                riskDetectionService.checkCoinRisk(user, trade.getAssetSymbol());
                System.out.println("Risk analysis completed for: " + trade.getAssetSymbol());
            } catch (Exception e) {
                System.err.println("Error during risk analysis: " + e.getMessage());
                // Don't fail the trade addition if risk analysis fails
            }
            
            // Return a simple success message instead of the Trade entity
            return ResponseEntity.ok(Map.of(
                "message", "Trade added successfully",
                "tradeId", trade.getId(),
                "assetSymbol", trade.getAssetSymbol(),
                "side", trade.getSide().toString(),
                "quantity", trade.getQuantity(),
                "price", trade.getPrice(),
                "fee", trade.getFee()
            ));
        } catch (Exception e) {
            System.err.println("Error adding trade: " + e.getMessage());
            System.err.println("Trade data received: " + tradeData.toString());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() + " - Trade data: " + tradeData.toString()));
        }
    }

    @PostMapping("/sync-trades/{exchangeId}")
    public ResponseEntity<?> syncTrades(@PathVariable Long exchangeId,
                                        Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            portfolioService.syncTradesFromExchange(userId, exchangeId);
            return ResponseEntity.ok(Map.of("message", "Trades synced successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getPortfolioSummary(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Map<String, Object> summary = portfolioService.getPortfolioSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
