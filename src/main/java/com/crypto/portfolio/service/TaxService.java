package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.TaxReportResponse;
import com.crypto.portfolio.dto.TaxTransaction;
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
public class TaxService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    //private final CryptoMarketService cryptoMarketService;

    public TaxReportResponse generateTaxReport(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Trade> trades =
                tradeRepository.findByUserOrderByExecutedAtAsc(user);

        List<TaxTransaction> transactions = new ArrayList<>();

        double totalProfit = 0;
        double totalLoss = 0;

        Map<String, Queue<BuyLotT>> buyQueue = new HashMap<>();

        for (Trade trade : trades) {

            String symbol = trade.getAssetSymbol().toUpperCase();

            if (trade.getSide() == Trade.Side.BUY) {

                buyQueue.putIfAbsent(symbol, new LinkedList<>());

                buyQueue.get(symbol).add(
                        new BuyLotT(trade.getQuantity(), trade.getPrice())
                );

            } else {

                double sellQty = trade.getQuantity();
                double sellPrice = trade.getPrice();

                Queue<BuyLotT> queue = buyQueue.get(symbol);

                double totalSellProfit = 0;
                double totalBuyCost = 0;
                double totalMatchedQty = 0;

                while (sellQty > 0 && queue != null && !queue.isEmpty()) {

                    BuyLotT buyLot = queue.peek();

                    double matchedQty = Math.min(sellQty, buyLot.quantity);

                    double profit = (sellPrice - buyLot.price) * matchedQty;

                    totalSellProfit += profit; // ✅ accumulate
                    totalBuyCost += buyLot.price * matchedQty;
                    totalMatchedQty += matchedQty;

                    if (profit >= 0) totalProfit += profit;
                    else totalLoss += profit;

                    buyLot.quantity -= matchedQty;
                    sellQty -= matchedQty;

                    if (buyLot.quantity == 0) {
                        queue.poll();
                    }
                }
                double avgBuyPrice = totalMatchedQty == 0 ? 0 : totalBuyCost / totalMatchedQty;

                // ✅ IMPORTANT: Add ONE transaction per SELL trade
                transactions.add(
                        new TaxTransaction(
                                symbol,
                                round(trade.getQuantity()),
                                round(avgBuyPrice),
                                sellPrice,
                                round(totalSellProfit), // ✅ realized P&L
                                trade.getExecutedAt()
                        )
                );
            }
        }

        double net = totalProfit + totalLoss;

        double taxableIncome = Math.max(net, 0);
        double tax = taxableIncome * 0.30;
        double netAfterTax = net - tax;

        return new TaxReportResponse(
                round(totalProfit),
                round(totalLoss),
                round(taxableIncome),
                round(tax),
                round(netAfterTax),
                transactions
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

@AllArgsConstructor
class BuyLotT {
    double quantity;
    double price;
}