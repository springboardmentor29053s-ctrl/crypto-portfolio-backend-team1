package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.model.PriceSnapshot;
import com.blockfoliox.crypto.repository.PriceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PriceSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PriceSnapshotService.class);

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final RestTemplate restTemplate;

    public PriceSnapshotService(PriceSnapshotRepository priceSnapshotRepository,
                                RestTemplate restTemplate) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 3600000)
    public void captureSnapshots() {
        log.info("📸 Capturing price snapshots at {}", LocalDateTime.now());

        try {
            String url = "https://api.coingecko.com/api/v3/coins/markets"
                    + "?vs_currency=usd&order=market_cap_desc&per_page=20&page=1";

            List<Map> coins = restTemplate.getForObject(url, List.class);

            if (coins == null) return;

            for (Map coin : coins) {
                PriceSnapshot snapshot = new PriceSnapshot();
                snapshot.setAssetSymbol(((String) coin.get("symbol")).toUpperCase());
                snapshot.setPriceUsd(new BigDecimal(coin.get("current_price").toString()));

                Object marketCap = coin.get("market_cap");
                if (marketCap != null) {
                    snapshot.setMarketCap(new BigDecimal(marketCap.toString()));
                }

                snapshot.setSource("coingecko");
                snapshot.setCapturedAt(LocalDateTime.now());
                priceSnapshotRepository.save(snapshot);
            }

            log.info(" Saved {} price snapshots", coins.size());

        } catch (Exception e) {
            log.error(" Snapshot error: {}", e.getMessage());
        }
    }
}