package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.model.Trade;
import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.TradeRepository;
import com.blockfoliox.crypto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    public ReportService(TradeRepository tradeRepository,
                         UserRepository userRepository) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
    }

    // ✅ P&L Summary per coin
    public List<Map<String, Object>> getPnLSummary(Long userId) {
        log.info("Generating P&L summary for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> allTrades = tradeRepository.findByUserOrderByExecutedAtDesc(user);

        // Group trades by asset symbol
        Map<String, List<Trade>> bySymbol = new LinkedHashMap<>();
        for (Trade t : allTrades) {
            bySymbol.computeIfAbsent(t.getAssetSymbol(), k -> new ArrayList<>()).add(t);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, List<Trade>> entry : bySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<Trade> trades = entry.getValue();

            BigDecimal totalBuyQty = BigDecimal.ZERO;
            BigDecimal totalBuyCost = BigDecimal.ZERO;
            BigDecimal totalSellQty = BigDecimal.ZERO;
            BigDecimal totalSellValue = BigDecimal.ZERO;
            BigDecimal totalFees = BigDecimal.ZERO;
            int buyCount = 0;
            int sellCount = 0;

            for (Trade t : trades) {
                BigDecimal qty = t.getQuantity();
                BigDecimal price = t.getPrice();
                BigDecimal fee = t.getFee() != null ? t.getFee() : BigDecimal.ZERO;
                totalFees = totalFees.add(fee);

                if (t.getSide() == Trade.Side.buy) {
                    totalBuyQty = totalBuyQty.add(qty);
                    totalBuyCost = totalBuyCost.add(qty.multiply(price));
                    buyCount++;
                } else {
                    totalSellQty = totalSellQty.add(qty);
                    totalSellValue = totalSellValue.add(qty.multiply(price));
                    sellCount++;
                }
            }

            // Avg buy price
            BigDecimal avgBuyPrice = totalBuyQty.compareTo(BigDecimal.ZERO) > 0
                    ? totalBuyCost.divide(totalBuyQty, 8, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Realized P&L = sell value - (sell qty * avg buy price) - fees
            BigDecimal costOfSold = totalSellQty.multiply(avgBuyPrice);
            BigDecimal realizedPnL = totalSellValue.subtract(costOfSold).subtract(totalFees);

            // Tax hint
            String taxHint = getTaxHint(trades);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", symbol);
            row.put("buyCount", buyCount);
            row.put("sellCount", sellCount);
            row.put("totalBuyQty", totalBuyQty.setScale(6, RoundingMode.HALF_UP));
            row.put("totalSellQty", totalSellQty.setScale(6, RoundingMode.HALF_UP));
            row.put("avgBuyPrice", avgBuyPrice.setScale(4, RoundingMode.HALF_UP));
            row.put("totalInvested", totalBuyCost.setScale(2, RoundingMode.HALF_UP));
            row.put("totalSellValue", totalSellValue.setScale(2, RoundingMode.HALF_UP));
            row.put("totalFees", totalFees.setScale(4, RoundingMode.HALF_UP));
            row.put("realizedPnL", realizedPnL.setScale(2, RoundingMode.HALF_UP));
            row.put("taxHint", taxHint);

            result.add(row);
        }

        log.info("✅ P&L summary generated for {} symbols", result.size());
        return result;
    }

    // ✅ Tax hint based on holding period
    private String getTaxHint(List<Trade> trades) {
        boolean hasLongTerm = trades.stream().anyMatch(t -> {
            if (t.getSide() != Trade.Side.buy || t.getExecutedAt() == null) return false;
            long daysHeld = java.time.temporal.ChronoUnit.DAYS.between(
                    t.getExecutedAt(), java.time.LocalDateTime.now());
            return daysHeld > 365;
        });

        boolean hasShortTerm = trades.stream().anyMatch(t -> {
            if (t.getSide() != Trade.Side.buy || t.getExecutedAt() == null) return false;
            long daysHeld = java.time.temporal.ChronoUnit.DAYS.between(
                    t.getExecutedAt(), java.time.LocalDateTime.now());
            return daysHeld <= 365;
        });

        if (hasLongTerm && hasShortTerm) return "Mixed — some trades qualify for long-term rates";
        if (hasLongTerm) return "Long-term — held over 1 year, lower tax rate may apply";
        if (hasShortTerm) return "Short-term — held under 1 year, standard tax rate applies";
        return "No tax data available";
    }

    // ✅ CSV Export — all trades
    public String exportTradesCSV(Long userId) {
        log.info("Exporting trades CSV for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtDesc(user);

        StringBuilder csv = new StringBuilder();
        csv.append("Symbol,Side,Quantity,Price,Total,Fee,Date\n");

        for (Trade t : trades) {
            BigDecimal total = t.getQuantity().multiply(t.getPrice());
            csv.append(t.getAssetSymbol()).append(",")
                    .append(t.getSide()).append(",")
                    .append(t.getQuantity()).append(",")
                    .append(t.getPrice()).append(",")
                    .append(total.setScale(2, RoundingMode.HALF_UP)).append(",")
                    .append(t.getFee() != null ? t.getFee() : "0").append(",")
                    .append(t.getExecutedAt()).append("\n");
        }

        return csv.toString();
    }
}