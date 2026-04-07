package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fetches crypto news from cryptocurrency.cv
 * Completely FREE — no API key, no registration needed.
 *
 * Endpoint: GET https://min-api.cryptocompare.com/data/v2/news/?lang=EN
 *
 * Just set app.news.enabled=true in application.properties — nothing else needed.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final String BASE_URL =
            "https://min-api.cryptocompare.com/data/v2/news/?lang=EN";

    private static final long NEWS_WINDOW_HOURS = 6;
    private static final long DEDUP_HOURS = 24;

    private final RestTemplate restTemplate = new RestTemplate();
    private final HoldingRepository holdingRepository;
    private final RiskAlertRepository riskAlertRepository;

    public NewsService(HoldingRepository holdingRepository,
                       RiskAlertRepository riskAlertRepository) {
        this.holdingRepository   = holdingRepository;
        this.riskAlertRepository = riskAlertRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public int scanNewsForUser(User user) {
        List<Holding> holdings = holdingRepository.findByUserId(user.getId());

        Set<String> symbols = holdings.stream()
                .map(h -> normalizeSymbol(h.getAssetSymbol()))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        if (symbols.isEmpty()) return 0;

        int alertsCreated = 0;
        Instant cutoff = Instant.now().minus(NEWS_WINDOW_HOURS, ChronoUnit.HOURS);

        for (String symbol : symbols) {
            String holdingSymbol = holdings.stream()
                    .filter(h -> normalizeSymbol(h.getAssetSymbol()).equals(symbol))
                    .map(Holding::getAssetSymbol)
                    .findFirst()
                    .orElse(symbol);

            if (isDuplicate(user.getId(), holdingSymbol)) {
                log.debug("Skipping news for {} — already alerted within {}h", symbol, DEDUP_HOURS);
                continue;
            }

            List<Map<String, Object>> articles = fetchNewsForTicker(symbol);
            if (articles.isEmpty()) continue;

            for (Map<String, Object> article : articles) {
                String pubDateStr = (String) article.get("pub_date");
                if (pubDateStr == null) continue;

                try {
                    Instant pubDate = Instant.parse(pubDateStr);
                    if (pubDate.isBefore(cutoff)) continue;

                    String title     = (String) article.getOrDefault("title", "Crypto news update");
                    String link      = (String) article.getOrDefault("link", "");
                    String sentiment = (String) article.getOrDefault("sentiment", "");

                    String sentimentNote = sentiment.isBlank() ? ""
                            : String.format(" Sentiment: %s.", capitalize(sentiment));

                    String details = String.format(
                            "📰 %s news: \"%s\"%s%s",
                            symbol,
                            title,
                            sentimentNote,
                            link.isBlank() ? "" : " — " + link
                    );

                    createAlert(user.getId(), holdingSymbol, details);
                    alertsCreated++;
                    break; // one alert per symbol per scan

                } catch (Exception e) {
                    log.warn("Failed to parse article for {}: {}", symbol, e.getMessage());
                }
            }
        }

        log.info("News scan complete for user {} — {} alert(s) created",
                user.getId(), alertsCreated);
        return alertsCreated;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchNewsForTicker(String ticker) {
        try {
            String url = String.format(BASE_URL, ticker);
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            // cryptocurrency.cv may return articles under different keys
            for (String key : List.of("articles", "results", "data", "items")) {
                Object val = response.get(key);
                if (val instanceof List<?> list && !list.isEmpty()) {
                    return (List<Map<String, Object>>) list;
                }
            }
            return List.of();

        } catch (RestClientException e) {
            log.warn("cryptocurrency.cv unavailable for {} ({}), skipping", ticker, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("News fetch error for {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    private String normalizeSymbol(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase().replace("USDT", "").replace("BUSD", "").trim();
    }

    private boolean isDuplicate(Long userId, String assetSymbol) {
        Instant since = Instant.now().minus(DEDUP_HOURS, ChronoUnit.HOURS);
        return riskAlertRepository.existsByUserIdAndAssetSymbolAndAlertTypeAndCreatedAtAfter(
                userId, assetSymbol, RiskAlert.AlertType.news, since);
    }

    private void createAlert(Long userId, String assetSymbol, String details) {
        RiskAlert alert = new RiskAlert();
        alert.setUserId(userId);
        alert.setAssetSymbol(assetSymbol);
        alert.setAlertType(RiskAlert.AlertType.news);
        alert.setDetails(details);
        alert.setDismissed(false);
        alert.setCreatedAt(Instant.now());
        riskAlertRepository.save(alert);
        log.info("News alert created: userId={} symbol={}", userId, assetSymbol);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}