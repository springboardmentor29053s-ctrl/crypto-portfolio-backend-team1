package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.PortfolioValueResponse;
import com.crypto.portfolio.model.Holding;
import com.crypto.portfolio.model.PortfolioSnapshot;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.HoldingRepository;
import com.crypto.portfolio.repository.PortfolioSnapshotRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final CryptoMarketService cryptoMarketService;
    private final PortfolioSnapshotRepository snapshotRepository;

    public void createSnapshots() {

        List<User> users = userRepository.findAll();

        // 🔥 STEP 1: Collect ALL symbols across users
        Map<Long, List<Holding>> userHoldingsMap = new HashMap<>();
        Set<String> allSymbols = new HashSet<>();

        for (User user : users) {
            List<Holding> holdings = holdingRepository.findByUser(user);

            if (holdings.isEmpty()) continue;

            userHoldingsMap.put(user.getId(), holdings);

            for (Holding h : holdings) {
                allSymbols.add(h.getAssetSymbol().toUpperCase());
            }
        }

        if (allSymbols.isEmpty()) return;

        // 🔥 STEP 2: ONE API CALL
        Map<String, Double> prices =
                cryptoMarketService.getPrices(new ArrayList<>(allSymbols));

        // 🔥 STEP 3: Process per user (NO API call inside loop)
        for (User user : users) {

            List<Holding> holdings = userHoldingsMap.get(user.getId());

            if (holdings == null || holdings.isEmpty()) continue;

            double totalValue = 0;

            for (Holding holding : holdings) {

                String symbol = holding.getAssetSymbol().toUpperCase();

                Double price = prices.get(symbol); // ✅ FIXED

                if (price == null) continue;

                totalValue += price * holding.getQuantity();
            }

            PortfolioSnapshot snapshot = new PortfolioSnapshot();
            snapshot.setUserId(user.getId());
            snapshot.setPortfolioValue(totalValue);
            snapshot.setTimestamp(LocalDateTime.now());

            snapshotRepository.save(snapshot);
        }

        System.out.println("Portfolio snapshots created (optimized)");
    }
}
