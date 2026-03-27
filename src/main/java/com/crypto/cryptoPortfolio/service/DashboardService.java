package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.DashboardSummaryResponse;
import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PortfolioService portfolioService;
    private final HoldingRepository holdingRepository;
    private final BinanceService binanceService;

    public DashboardSummaryResponse getDashboard(Long userId) {

        List<Holding> holdings = holdingRepository.findByUserId(userId);

        // ✅ FIX: Group holdings by symbol to prevent duplicates
        Map<String, List<Holding>> groupedBySymbol = holdings.stream()
                .filter(h -> h.getAssetSymbol() != null && !h.getAssetSymbol().isBlank())
                .collect(Collectors.groupingBy(
                        h -> h.getAssetSymbol().trim().toUpperCase()
                ));

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        List<DashboardSummaryResponse.AssetAllocation> allocations = new ArrayList<>();

        // ✅ Process each unique symbol
        for (Map.Entry<String, List<Holding>> entry : groupedBySymbol.entrySet()) {

            String symbol = entry.getKey();
            List<Holding> symbolHoldings = entry.getValue();

            // ✅ Sum all holdings for this symbol (manual + auto)
            BigDecimal totalQuantity = symbolHoldings.stream()
                    .map(Holding::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSymbolInvested = symbolHoldings.stream()
                    .map(h -> h.getQuantity().multiply(h.getAvgCost()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ✅ Get current price
            BigDecimal currentPrice;
            try {
                String pair = symbol.endsWith("USDT") ? symbol : symbol + "USDT";
                currentPrice = binanceService.getCurrentPrice(pair);
                if (currentPrice == null) {
                    currentPrice = BigDecimal.ZERO;
                }
            } catch (Exception e) {
                currentPrice = BigDecimal.ZERO;
            }

            // ✅ Calculate value for this symbol
            BigDecimal symbolValue = totalQuantity.multiply(currentPrice);

            totalValue = totalValue.add(symbolValue);
            totalInvested = totalInvested.add(totalSymbolInvested);

            // ✅ Store for allocation calculation
            allocations.add(new DashboardSummaryResponse.AssetAllocation(
                    symbol,
                    symbolValue // We'll calculate percentage later
            ));
        }

        // ✅ Calculate PnL
        BigDecimal totalPnL = totalValue.subtract(totalInvested);

        BigDecimal totalPnLPercentage = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercentage = totalPnL
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // ✅ Calculate allocation percentages
        for (DashboardSummaryResponse.AssetAllocation allocation : allocations) {
            BigDecimal percentage = BigDecimal.ZERO;

            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                // allocation.percentage currently stores symbolValue
                BigDecimal symbolValue = allocation.getPercentage();
                percentage = symbolValue
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            allocation.setPercentage(percentage);
        }

        return new DashboardSummaryResponse(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                totalInvested.setScale(2, RoundingMode.HALF_UP),
                totalPnL.setScale(2, RoundingMode.HALF_UP),
                totalPnLPercentage.setScale(2, RoundingMode.HALF_UP),
                allocations
        );
    }
}