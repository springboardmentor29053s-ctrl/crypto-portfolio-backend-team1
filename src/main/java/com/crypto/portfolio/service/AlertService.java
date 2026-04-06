package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.AlertRequest;
import com.crypto.portfolio.dto.PortfolioValueResponse;
import com.crypto.portfolio.model.Alert;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.AlertRepository;
import com.crypto.portfolio.repository.ExchangeRepository;
import com.crypto.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final PriceCacheService priceCacheService;
    private final PortfolioService portfolioService;
    public void createAlert(String username, AlertRequest request) {


        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Alert alert = new Alert();

        alert.setUser(user);
        alert.setType(request.getType());
        alert.setSymbol(request.getSymbol() != null
                ? request.getSymbol().toUpperCase()
                : null);
        alert.setTargetValue(request.getTargetValue());
        alert.setTriggered(false);
        alert.setSeen(false); // 🔥 important
        alert.setTriggeredAt(LocalDateTime.now()); // optional
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    private void checkPriceAlert(Alert alert) {

        String symbol = alert.getSymbol();


        Map<String, Double> prices =
                priceCacheService.getPrices(Set.of(symbol));

        Double currentPrice = prices.get(symbol);

        if (currentPrice == null) {
            System.out.println("⚠️ Price not found for " + symbol);
            return;
        }

        if (currentPrice >= alert.getTargetValue()) {

            alert.setTriggered(true);
            alert.setSeen(false); // ensure it's visible in notification
            alert.setTriggeredAt(LocalDateTime.now());

            System.out.println("🔥 PRICE ALERT TRIGGERED: " + symbol);

            alertRepository.save(alert);
        }
    }

    private void checkProfitAlert(Alert alert) {

        String username = alert.getUser().getName();

        PortfolioValueResponse portfolio =
                portfolioService.getPortfolioValue(username);

        double profitPercent = portfolio.getProfitPercentage();

        if (profitPercent >= alert.getTargetValue()) {

            alert.setTriggered(true);
            alert.setSeen(false); // ensure it's visible in notification
            alert.setTriggeredAt(LocalDateTime.now());

            System.out.println("🚀 PROFIT ALERT TRIGGERED: " + username);

            alertRepository.save(alert);
        }
    }

    public void processAlerts() {

        List<Alert> alerts = alertRepository.findByTriggeredFalse();

        for (Alert alert : alerts) {

            if ("PRICE".equals(alert.getType())) {
                checkPriceAlert(alert);
            }

            else if ("PROFIT".equals(alert.getType())) {
                checkProfitAlert(alert);
            }

            else if ("LOSS".equals(alert.getType())) {
                checkLossAlert(alert);
            }
        }
    }
    private void checkLossAlert(Alert alert) {

        String username = alert.getUser().getName();

        PortfolioValueResponse portfolio =
                portfolioService.getPortfolioValue(username);

        double profitPercent = portfolio.getProfitPercentage();

        if (profitPercent <= alert.getTargetValue()) {

            alert.setTriggered(true);
            alert.setSeen(false); // ensure it's visible in notification
            alert.setTriggeredAt(LocalDateTime.now());

            System.out.println("💀 LOSS ALERT TRIGGERED: " + username);

            alertRepository.save(alert);
        }
    }
}
