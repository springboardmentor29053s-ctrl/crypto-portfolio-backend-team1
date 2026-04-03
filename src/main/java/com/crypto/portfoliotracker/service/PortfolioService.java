package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.dto.TradeDTO;
import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.repository.HoldingRepository;
import com.crypto.portfoliotracker.repository.TradeRepository;
import com.crypto.portfoliotracker.repository.ApiKeyRepository;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.repository.ExchangeRepository;
import com.crypto.portfoliotracker.entity.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private ExchangeService exchangeService;

    public List<Holding> getUserHoldings(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return holdingRepository.findByUserOrderByAssetSymbol(user);
    }

    public Holding addHolding(Long userId, String assetSymbol, BigDecimal quantity, 
                            BigDecimal avgCost, String walletType, Long exchangeId, String address) {
        Holding holding = new Holding();
        
        // Set user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        holding.setUser(user);
        
        holding.setAssetSymbol(assetSymbol.toUpperCase());
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        
        // Convert wallet type string to enum (handle both lowercase and uppercase)
        if (walletType != null) {
            holding.setWalletType(Holding.WalletType.valueOf(walletType.toUpperCase()));
        } else {
            holding.setWalletType(Holding.WalletType.WALLET);
        }
        
        // Set exchange if provided
        if (exchangeId != null) {
            Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));
            holding.setExchange(exchange);
        }
        
        holding.setAddress(address);
        holding.setUpdatedAt(LocalDateTime.now());

        return holdingRepository.save(holding);
    }

    public Holding updateHolding(Long holdingId, BigDecimal quantity, BigDecimal avgCost) {
        Holding holding = holdingRepository.findById(holdingId)
            .orElseThrow(() -> new RuntimeException("Holding not found"));
        
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setUpdatedAt(LocalDateTime.now());

        return holdingRepository.save(holding);
    }

    public void deleteHolding(Long holdingId, Long userId) {
        Holding holding = holdingRepository.findById(holdingId)
            .orElseThrow(() -> new RuntimeException("Holding not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!holding.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this holding");
        }
        
        holdingRepository.delete(holding);
    }

    @Transactional
    public void syncHoldingsFromExchange(Long userId, Long exchangeId) {
        List<ApiKey> apiKeys = apiKeyRepository.findByUserIdAndExchangeId(userId, exchangeId);
        if (apiKeys.isEmpty()) {
            throw new RuntimeException("API key not found");
        }
        
        ApiKey apiKey = apiKeys.get(0);

        Map<String, BigDecimal> balances = exchangeService.fetchBalances(apiKey);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Exchange exchange = exchangeRepository.findById(exchangeId)
            .orElseThrow(() -> new RuntimeException("Exchange not found"));
        
        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal quantity = entry.getValue();
            
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                Holding existingHolding = holdingRepository
                    .findByUserAndAssetSymbolAndExchange(user, symbol, exchange)
                    .orElse(null);
                
                if (existingHolding != null) {
                    existingHolding.setQuantity(quantity);
                    existingHolding.setUpdatedAt(LocalDateTime.now());
                    holdingRepository.save(existingHolding);
                } else {
                    addHolding(userId, symbol, quantity, BigDecimal.ZERO, "exchange", exchangeId, null);
                }
            }
        }
    }

    public List<TradeDTO> getUserTrades(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);
        System.out.println("PortfolioService: Found raw trades count: " + trades.size());
        
        List<TradeDTO> tradeDTOs = new ArrayList<>();
        for (Trade trade : trades) {
            try {
                System.out.println("PortfolioService: Processing trade: " + trade.getId() + " - " + trade.getAssetSymbol());
                TradeDTO dto = new TradeDTO(
                    trade.getId(),
                    trade.getAssetSymbol(),
                    trade.getSide().toString(),
                    trade.getQuantity(),
                    trade.getPrice(),
                    trade.getFee(),
                    trade.getExchange() != null ? trade.getExchange().getName() : "Unknown",
                    trade.getExecutedAt()
                );
                tradeDTOs.add(dto);
                System.out.println("PortfolioService: Successfully processed trade DTO: " + dto.getId());
            } catch (Exception e) {
                System.err.println("PortfolioService: Error processing trade " + trade.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.err.println("PortfolioService: User " + userId + " - Returning DTOs count: " + tradeDTOs.size());
        return tradeDTOs;
    }

    @Transactional
    public Trade addTrade(Long userId, String assetSymbol, String side, BigDecimal quantity,
                         BigDecimal price, BigDecimal fee, Long exchangeId) {
        Trade trade = new Trade();
        trade.setAssetSymbol(assetSymbol.toUpperCase());
        trade.setSide(Trade.TradeSide.valueOf(side.toUpperCase()));
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setFee(fee);
        trade.setExecutedAt(LocalDateTime.now());
        
        // Set user and exchange
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        trade.setUser(user);
        
        Exchange exchange = exchangeRepository.findById(exchangeId)
            .orElseThrow(() -> new RuntimeException("Exchange not found"));
        trade.setExchange(exchange);

        return tradeRepository.save(trade);
    }

    @Transactional
    public void syncTradesFromExchange(Long userId, Long exchangeId) {
        List<ApiKey> apiKeys = apiKeyRepository.findByUserIdAndExchangeId(userId, exchangeId);
        if (apiKeys.isEmpty()) {
            throw new RuntimeException("API key not found");
        }
        
        ApiKey apiKey = apiKeys.get(0);

        List<Trade> exchangeTrades = exchangeService.fetchRecentTrades(apiKey);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Exchange exchange = exchangeRepository.findById(exchangeId)
            .orElseThrow(() -> new RuntimeException("Exchange not found"));
        
        for (Trade trade : exchangeTrades) {
            boolean exists = tradeRepository.findByUserAndAssetSymbolAndSideAndQuantityAndPriceAndExecutedAt(
                user, trade.getAssetSymbol(), trade.getSide(), trade.getQuantity(), 
                trade.getPrice(), trade.getExecutedAt()).isPresent();
            
            if (!exists) {
                trade.setUser(user);
                trade.setExchange(exchange);
                tradeRepository.save(trade);
            }
        }
    }

    public Map<String, Object> getPortfolioSummary(Long userId) {
        List<Holding> holdings = getUserHoldings(userId);
        List<TradeDTO> trades = getUserTrades(userId);

        BigDecimal totalValue = holdings.stream()
            .map(h -> h.getQuantity().multiply(h.getAvgCost()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = trades.stream()
            .map(t -> t.getQuantity().multiply(t.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> assetDistribution = holdings.stream()
            .collect(Collectors.toMap(
                Holding::getAssetSymbol,
                h -> h.getQuantity().multiply(h.getAvgCost()),
                BigDecimal::add
            ));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHoldings", holdings.size());
        summary.put("totalTrades", trades.size());
        summary.put("totalValue", totalValue);
        summary.put("totalCost", totalCost);
        summary.put("assetDistribution", assetDistribution);
        summary.put("lastUpdated", LocalDateTime.now());
        
        return summary;
    }
}
