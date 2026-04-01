package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.TradeResponse;
import com.crypto.portfolio.model.Trade;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.TradeRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TradeAnalysisService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    public List<TradeResponse> getAnalyzedTrades(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> trades =
                tradeRepository.findByUserOrderByExecutedAtAsc(user);

        Map<String, Double> avgCostMap = new HashMap<>();
        Map<String, Double> quantityMap = new HashMap<>();

        List<TradeResponse> result = new ArrayList<>();

        for (Trade trade : trades) {

            String symbol = trade.getAssetSymbol().toUpperCase();

            double qty = trade.getQuantity();
            double price = trade.getPrice();

            if (trade.getSide() == Trade.Side.BUY) {

                double existingQty = quantityMap.getOrDefault(symbol, 0.0);
                double existingCost = avgCostMap.getOrDefault(symbol, 0.0);

                double totalQty = existingQty + qty;

                double newAvgCost =
                        ((existingQty * existingCost) + (qty * price)) / totalQty;

                quantityMap.put(symbol, totalQty);
                avgCostMap.put(symbol, newAvgCost);

                result.add(new TradeResponse(
                        symbol,
                        "BUY",
                        qty,
                        price,
                        trade.getExecutedAt(),
                        0.0
                ));

            } else {

                double avgCost = avgCostMap.getOrDefault(symbol, 0.0);

                double profit = (price - avgCost) * qty;

                // reduce quantity
                double remainingQty = quantityMap.getOrDefault(symbol, 0.0) - qty;

                if (remainingQty <= 0) {
                    quantityMap.remove(symbol);
                    avgCostMap.remove(symbol);
                } else {
                    quantityMap.put(symbol, remainingQty);
                }

                result.add(new TradeResponse(
                        symbol,
                        "SELL",
                        qty,
                        price,
                        trade.getExecutedAt(),
                        round(profit)
                ));
            }
        }

        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

@AllArgsConstructor
class BuyLot {
    double quantity;
    double price;
}