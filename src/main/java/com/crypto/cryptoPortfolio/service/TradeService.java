package com.crypto.cryptoPortfolio.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crypto.cryptoPortfolio.dto.TradeRequest;
import com.crypto.cryptoPortfolio.dto.TradeResponse;
import com.crypto.cryptoPortfolio.entity.*;
import com.crypto.cryptoPortfolio.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final BinanceService binanceService;
    private final PortfolioService portfolioService;
    private final HoldingRepository holdingRepository;

    /**
     * Place a BUY or SELL order
     */
    @Transactional
    public TradeResponse placeOrder(TradeRequest request, String email) {

        if (request.getQuantity() == null ||
                request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid quantity");
        }

        TradeSide side = TradeSide.valueOf(request.getSide().toUpperCase());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findById(request.getExchangeId())
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, exchange)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        // Recalculate holdings before SELL validation
        portfolioService.recalculateAndStoreHoldings(email, exchange);

        if (side == TradeSide.SELL) {

            List<Holding> holdings =
                    holdingRepository.findByUserIdAndExchangeId(
                            user.getId(),
                            exchange.getId()
                    );

            String asset = request.getAssetSymbol().toUpperCase(); // "ETHUSDT"

            BigDecimal availableQty = holdings.stream()
                    .filter(h -> h.getAssetSymbol().equalsIgnoreCase(asset))
                    .map(Holding::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (availableQty.compareTo(request.getQuantity()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient asset quantity to sell");
            }
        }

        // Place Order on Binance
        String binanceResponse = binanceService.placeOrder(
                apiKey,
                request.getAssetSymbol().toUpperCase(),
                side.name(),
                "MARKET",
                request.getQuantity().toPlainString()
        );

        if (binanceResponse == null || binanceResponse.contains("\"code\":")) {
            throw new RuntimeException("Order failed on Binance: " + binanceResponse);
        }

        // FIX: Retry sync with delay — Binance takes time to reflect new trades
        // in /myTrades even after a successful order placement
        String symbol = request.getAssetSymbol().toUpperCase();
        int savedCount = 0;
        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries && savedCount == 0) {
            attempt++;
            try {
                // Wait longer on each retry (500ms, 1s, 1.5s, 2s, 2.5s)
                Thread.sleep(500L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            System.out.println("Sync attempt " + attempt + " for symbol: " + symbol);

            savedCount = binanceService.syncTrades(apiKey, symbol, user, exchange);
        }

        if (savedCount == 0) {
            // Trade executed on Binance but not yet visible in /myTrades.
            // Return a partial response — the trade WILL appear on next sync.
            System.out.println("Warning: Trade placed but not yet synced to DB after "
                    + maxRetries + " attempts for symbol: " + symbol);

            TradeResponse partial = new TradeResponse();
            partial.setAssetSymbol(symbol);
            partial.setSide(side.name());
            partial.setQuantity(request.getQuantity());
            partial.setExchangeName(exchange.getName());
            partial.setRealizedProfit(BigDecimal.ZERO);
            return partial;
        }

        // Recalculate holdings after trade is saved
        portfolioService.recalculateAndStoreHoldings(email, exchange);

        // Return the latest saved trade
        List<Trade> latestTrades = tradeRepository
                .findByUserAndExchangeAndAssetSymbolOrderByExecutedAtDesc(
                        user,
                        exchange,
                        symbol
                );

        if (latestTrades.isEmpty()) {
            TradeResponse response = new TradeResponse();
            response.setAssetSymbol(symbol);
            response.setSide(side.name());
            response.setQuantity(request.getQuantity());
            response.setExchangeName(exchange.getName());
            response.setRealizedProfit(BigDecimal.ZERO);
            return response;
        }

        return mapToResponse(latestTrades.get(0));
    }

    /**
     * Get user's trade history
     */
    public List<Trade> getTradeHistory(User user) {
        return tradeRepository.findByUser(user);
    }

    /**
     * Helper method to map Trade entity to TradeResponse DTO
     */
    private TradeResponse mapToResponse(Trade trade) {

        TradeResponse response = new TradeResponse();

        response.setId(trade.getId());
        response.setAssetSymbol(trade.getAssetSymbol());
        response.setSide(trade.getSide().name());
        response.setQuantity(trade.getQuantity());
        response.setPrice(trade.getPrice());
        response.setFee(trade.getFee());
        response.setExchangeName(trade.getExchange().getName());
        response.setExecutedAt(trade.getExecutedAt());
        response.setRealizedProfit(
                trade.getRealizedProfit() != null
                        ? trade.getRealizedProfit()
                        : BigDecimal.ZERO
        );

        return response;
    }
}