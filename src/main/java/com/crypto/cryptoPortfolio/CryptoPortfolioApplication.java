package com.crypto.cryptoPortfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CryptoPortfolioApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoPortfolioApplication.class, args);
	}

}
