package com.crypto.portfoliotracker.config;

import com.crypto.portfoliotracker.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ExchangeService exchangeService;

    @Override
    public void run(String... args) throws Exception {
        exchangeService.initializeDefaultExchanges();
    }
}
