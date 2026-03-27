package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.Holding;
import com.crypto.cryptoPortfolio.entity.RiskAlert;
import com.crypto.cryptoPortfolio.entity.ScamToken;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.HoldingRepository;
import com.crypto.cryptoPortfolio.repository.RiskAlertRepository;
import com.crypto.cryptoPortfolio.repository.ScamTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Core service for scanning a user's holdings against
 * Etherscan and CryptoScamDB, and persisting RiskAlert rows.
 *
 * Known ERC-20 contract addresses for major coins.
 * You can expand this map or fetch contract addresses dynamically.
 */
@Service
public class RiskScanService {

    private static final Logger log = LoggerFactory.getLogger(RiskScanService.class);

    // Symbol → ERC-20 contract address on Ethereum mainnet
    // Add more as needed; coins without contracts are skipped for Etherscan
    private static final Map<String, String> SYMBOL_TO_CONTRACT = Map.of(
            "USDT",   "0xdac17f958d2ee523a2206206994597c13d831ec7",
            "USDC",   "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
            "LINK",   "0x514910771af9ca656af840dff83e8264ecf986ca",
            "UNI",    "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984",
            "SHIB",   "0x95ad61b0a150d79219dcf64e1e6cc01f0b64c4ce",
            "MATIC",  "0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0",
            "AAVE",   "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9"
    );

    private final HoldingRepository holdingRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final ScamTokenRepository scamTokenRepository;
    private final EtherscanService etherscanService;
    private final CryptoScamDBService cryptoScamDBService;

    public RiskScanService(HoldingRepository holdingRepository,
                           RiskAlertRepository riskAlertRepository,
                           ScamTokenRepository scamTokenRepository,
                           EtherscanService etherscanService,
                           CryptoScamDBService cryptoScamDBService) {
        this.holdingRepository    = holdingRepository;
        this.riskAlertRepository  = riskAlertRepository;
        this.scamTokenRepository  = scamTokenRepository;
        this.etherscanService     = etherscanService;
        this.cryptoScamDBService  = cryptoScamDBService;
    }

    /**
     * Scan all holdings for a single user.
     * Called directly from RiskAlertController (manual trigger)
     * and from RiskScheduler (automated every 6 hours).
     */
    public int scanUserHoldings(User user) {
        List<Holding> holdings = holdingRepository.findByUserId(user.getId());
        int alertsCreated = 0;

        for (Holding holding : holdings) {
            String symbol = holding.getAssetSymbol()
                    .replace("USDT", "")
                    .replace("BTC",  "")
                    .toUpperCase()
                    .trim();

            // ── DEDUP: skip if we already alerted this user for this asset in last 24h ──
            Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
            if (riskAlertRepository.existsByUserIdAndAssetSymbolAndCreatedAtAfter(
                    user.getId(), holding.getAssetSymbol(), since)) {
                log.info("Skipping {} for user {} — already alerted within 24h", symbol, user.getId());
                continue;
            }

            // ── CHECK 1: CryptoScamDB by name ──
            boolean isKnownScam = cryptoScamDBService.isKnownScamByName(symbol);
            if (isKnownScam) {
                createAlert(user.getId(), holding.getAssetSymbol(),
                        RiskAlert.AlertType.rugpull_warning,
                        String.format("⚠️ %s is listed in CryptoScamDB as a known scam.", symbol));
                alertsCreated++;
                continue; // don't need to check further if already flagged
            }

            // ── CHECK 2: Etherscan contract check (only for ERC-20 tokens we know) ──
            String contractAddress = SYMBOL_TO_CONTRACT.get(symbol);
            if (contractAddress != null) {
                String reputation = etherscanService.checkContractReputation(contractAddress);
                boolean sourceVerified = etherscanService.isSourceCodeVerified(contractAddress);

                if ("unverified".equals(reputation)) {
                    createAlert(user.getId(), holding.getAssetSymbol(),
                            RiskAlert.AlertType.contract_risk,
                            String.format("⚠️ %s contract (%s) is unverified on Etherscan.", symbol, contractAddress));
                    alertsCreated++;
                } else if (!sourceVerified) {
                    createAlert(user.getId(), holding.getAssetSymbol(),
                            RiskAlert.AlertType.contract_risk,
                            String.format("⚠️ %s contract source code is not publicly verified on Etherscan.", symbol));
                    alertsCreated++;
                }

                // ── CHECK 3: CryptoScamDB by contract address ──
                ScamToken.RiskLevel scamLevel = cryptoScamDBService.checkAddress(contractAddress);
                if (scamLevel != null) {
                    // Cache in ScamTokens table for future reference
                    cacheScamToken(contractAddress, "ETH", scamLevel, "CryptoScamDB");

                    RiskAlert.AlertType alertType = scamLevel == ScamToken.RiskLevel.high
                            ? RiskAlert.AlertType.rugpull_warning
                            : RiskAlert.AlertType.contract_risk;

                    createAlert(user.getId(), holding.getAssetSymbol(), alertType,
                            String.format("🚨 %s contract flagged as %s risk by CryptoScamDB.",
                                    symbol, scamLevel.name().toUpperCase()));
                    alertsCreated++;
                }
            }
        }

        log.info("Risk scan complete for user {} — {} alert(s) created", user.getId(), alertsCreated);
        return alertsCreated;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void createAlert(Long userId, String assetSymbol,
                             RiskAlert.AlertType type, String details) {
        RiskAlert alert = new RiskAlert();
        alert.setUserId(userId);
        alert.setAssetSymbol(assetSymbol);
        alert.setAlertType(type);
        alert.setDetails(details);
        alert.setDismissed(false);
        alert.setCreatedAt(Instant.now());
        riskAlertRepository.save(alert);
        log.info("Alert created: userId={} symbol={} type={}", userId, assetSymbol, type);
    }

    private void cacheScamToken(String address, String chain,
                                ScamToken.RiskLevel level, String source) {
        if (!scamTokenRepository.existsByContractAddress(address)) {
            ScamToken token = new ScamToken();
            token.setContractAddress(address);
            token.setChain(chain);
            token.setRiskLevel(level);
            token.setSource(source);
            scamTokenRepository.save(token);
        }
    }
}