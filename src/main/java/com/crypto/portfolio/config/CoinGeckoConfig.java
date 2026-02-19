package com.crypto.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinGeckoConfig {

    @Value("${coingecko.base.url}")
    private String baseUrl;


    public String getBaseUrl() {
        return baseUrl;
    }
}

/*


// changed to get full result
package com.crypto.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinGeckoConfig {

    @Value("${coingecko.base.url}")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }
}

*/