package com.crypto.portfolio.service;


import com.crypto.portfolio.dto.BuyRequest;
import com.crypto.portfolio.dto.SellRequest;
import com.crypto.portfolio.dto.TradeResponse;
import com.crypto.portfolio.exchange.BinanceClient;
import com.crypto.portfolio.model.*;
import com.crypto.portfolio.repository.ApiKeyRepository;
import com.crypto.portfolio.repository.ExchangeRepository;
import com.crypto.portfolio.repository.TradeRepository;
import com.crypto.portfolio.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradingService {

    private final TradeRepository tradeRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;
    private final CryptoMarketService cryptoMarketService;
    private final HoldingService holdingService;
    private final ApiKeyRepository apiKeyRepository;
    private final BinanceClient binanceClient;
    private final ExchangeService exchangeService;
    private final TradeAnalysisService tradeAnalysisService;
    //private final UserRepository userRepository;

    // buy crypto

    @Transactional
    public TradeResponse buyCrypto(BuyRequest request) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository
                .findByNameIgnoreCase("CoinGecko")
                .orElseThrow(() -> new RuntimeException("CoinGecko exchange not found"));

        String symbol = request.getSymbol().toUpperCase();

        
        Double price = cryptoMarketService.getCurrentPrice(symbol);

        if (price == null || price <= 0) {
            throw new RuntimeException("Price not available for " + symbol);
        }

        Trade trade = new Trade();
        trade.setUser(user);
        trade.setExchange(exchange);
        trade.setAssetSymbol(symbol);
        trade.setSide(Trade.Side.BUY);
        trade.setQuantity(request.getQuantity());
        trade.setPrice(price);
        trade.setFee(0.0);
        trade.setExecutedAt(LocalDateTime.now());

        tradeRepository.save(trade);

        holdingService.updateHolding(
                user,
                exchange,
                symbol,
                request.getQuantity(),
                price
        );

        return new TradeResponse(
                symbol,
                "BUY",
                request.getQuantity(),
                price,
                LocalDateTime.now(),
                null
        );
    }


    // Sell Crypto
    @Transactional
    public TradeResponse sellCrypto(SellRequest request) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Always use CoinGecko
        Exchange exchange = exchangeRepository
                .findByNameIgnoreCase("CoinGecko")
                .orElseThrow(() -> new RuntimeException("CoinGecko exchange not found"));

        String symbol = request.getSymbol().toUpperCase();

        // ✅ Get holding ONLY from CoinGecko
        Holding holding = holdingService.getHolding(user, exchange, symbol);

        if (holding.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Not enough crypto to sell");
        }

        // ✅ Get current price
        Double price = cryptoMarketService.getCurrentPrice(symbol);

        // ✅ Save trade
        Trade trade = new Trade();
        trade.setUser(user);
        trade.setExchange(exchange);
        trade.setAssetSymbol(symbol);
        trade.setSide(Trade.Side.SELL);
        trade.setQuantity(request.getQuantity());
        trade.setPrice(price);
        trade.setFee(0.0);
        trade.setExecutedAt(LocalDateTime.now());

        tradeRepository.save(trade);

        // ✅ Update holding
        holdingService.reduceHolding(
                user,
                exchange,
                symbol,
                request.getQuantity()
        );

        return new TradeResponse(
                symbol,
                "SELL",
                request.getQuantity(),
                price,
                LocalDateTime.now(),
                null
        );
    }

    // trade history


    public Page<TradeResponse> getTradeHistory(int page, int size) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        List<TradeResponse> analyzed =
                tradeAnalysisService.getAnalyzedTrades(username);

// 🔥 FIX: reverse to latest first
        Collections.reverse(analyzed);

        int start = page * size;
        int end = Math.min(start + size, analyzed.size());

        List<TradeResponse> paginated = analyzed.subList(start, end);

        return new PageImpl<>(
                paginated,
                PageRequest.of(page, size),
                analyzed.size()
        );
    }

    private Exchange resolveExchange(String exchangeName) {

        // ✅ Default = CoinGecko
        if (exchangeName == null || exchangeName.isBlank()) {
            return exchangeRepository.findByName("CoinGecko")
                    .orElseThrow(() -> new RuntimeException("Default exchange not found"));
        }

        return exchangeRepository.findByName(exchangeName)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));
    }

    private Exchange getActiveExchange(User user) {

        return apiKeyRepository.findByUserAndIsActiveTrue(user)
                .map(ApiKey::getExchange)
                .orElseGet(() ->
                        exchangeRepository.findByName("CoinGecko")
                                .orElseThrow(() -> new RuntimeException("Default exchange not found"))
                );
    }
}
