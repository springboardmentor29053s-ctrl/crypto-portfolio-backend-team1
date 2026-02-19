package com.crypto.portfolio.service;

import com.crypto.portfolio.config.CoinGeckoConfig;
import com.crypto.portfolio.dto.CryptoCoin_dto;
import com.crypto.portfolio.dto.PageResponse_dto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CryptoMarketService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CoinGeckoConfig config;

    public CryptoMarketService(CoinGeckoConfig config) {
        this.config = config;
    }

    public PageResponse_dto<CryptoCoin_dto> getDashboardData(int page, int size) {

        String url = config.getBaseUrl()
                + "/coins/markets"
                + "?vs_currency=usd"
                + "&order=market_cap_desc"
                + "&per_page=" + size
                + "&page=" + page
                + "&sparkline=true"
                + "&price_change_percentage=24h,7d";

        List<CryptoCoin_dto> coins =
                restTemplate.exchange(
                        url,
                        org.springframework.http.HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<CryptoCoin_dto>>() {}
                ).getBody();

        // Estimate total elements (CoinGecko has ~10000 coins)
        long estimatedTotal = 10000;

        int totalPages = (int) Math.ceil((double) estimatedTotal / size);

        PageResponse_dto<CryptoCoin_dto> response = new PageResponse_dto<>();

        response.setContent(coins);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(estimatedTotal);
        response.setTotalPages(totalPages);
        response.setLast(page >= totalPages);

        return response;
    }
}
/*


// changed
import com.crypto.portfolio.config.CoinGeckoConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CryptoMarketService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CoinGeckoConfig config;

    public CryptoMarketService(CoinGeckoConfig config) {
        this.config = config;
    }

    public String getDashboardData(int page, int size) {

            String url = config.getBaseUrl()
                    + "/coins/markets"
                    + "?vs_currency=usd"
                    + "&order=market_cap_desc"
                    + "&per_page=" + size
                    + "&page=" + page
                    + "&sparkline=false"
                    + "&price_change_percentage=1h,24h,7d,30d";

            return restTemplate.getForObject(url, String.class);

    }
}
*/