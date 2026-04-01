package com.crypto.portfolio.service;


import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.Holding;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.HoldingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public void updateHolding(User user,
                              Exchange exchange,
                              String symbol,
                              Double quantity,
                              Double price) {

        Holding holding =
                holdingRepository
                        .findByUserAndExchangeAndAssetSymbol(user, exchange, symbol.toUpperCase())
                        .orElse(null);

        if (holding == null) {

            holding = new Holding();

            holding.setUser(user);
            holding.setExchange(exchange);
            holding.setAssetSymbol(symbol.toUpperCase());
            holding.setQuantity(quantity);
            holding.setAvgCost(price);
            holding.setWalletType(Holding.WalletType.EXCHANGE);
            holding.setUpdatedAt(LocalDateTime.now());

        } else {

            double totalQty =
                    holding.getQuantity() + quantity;

            double totalCost =
                    (holding.getAvgCost() * holding.getQuantity())
                            + (price * quantity);

            holding.setQuantity(totalQty);
            holding.setAvgCost(totalCost / totalQty);
            holding.setUpdatedAt(LocalDateTime.now());
        }

        holdingRepository.save(holding);
        holdingRepository.flush();
    }


    @Transactional
    public void reduceHolding(User user,
                              Exchange exchange,
                              String symbol,
                              Double quantity) {

        Holding holding = holdingRepository
                .findByUserAndExchangeAndAssetSymbol(user, exchange, symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Holding not found"));

        double remainingQty = holding.getQuantity() - quantity;

        if (remainingQty <= 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remainingQty);
            // ❗ avgCost remains SAME in avg-cost model
            holding.setUpdatedAt(LocalDateTime.now());
            holdingRepository.save(holding);
        }
    }


    public Holding getHolding(User user,
                              Exchange exchange,
                              String symbol) {

        return holdingRepository
                .findByUserAndExchangeAndAssetSymbol(user, exchange, symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("No holdings found"));
    }

}