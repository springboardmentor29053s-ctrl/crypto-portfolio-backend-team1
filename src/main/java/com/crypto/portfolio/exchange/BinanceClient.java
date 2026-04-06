package com.crypto.portfolio.exchange;



import org.springframework.stereotype.Component;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;



@Component
public class BinanceClient {

    private static final String BASE_URL = "https://api.binance.com/api/v3";

    // 🔐 Signature
    public String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec keySpec =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Error signing Binance request", e);
        }
    }

    // 🧾 Account URL
    public String buildAccountUrl(String apiSecret) {

        long timestamp = System.currentTimeMillis();

        String query = "timestamp=" + timestamp;

        String signature = sign(query, apiSecret);

        return BASE_URL + "/account?" + query + "&signature=" + signature;
    }

    // 📊 Trades URL
    public String buildTradeUrl(String apiSecret, String symbol, Long startTime) {

        long timestamp = System.currentTimeMillis();

        String query = "symbol=" + symbol + "&timestamp=" + timestamp;

        // 🔥 ADD THIS
        if (startTime != null) {
            query += "&startTime=" + startTime;
        }

        String signature = sign(query, apiSecret);

        return BASE_URL + "/myTrades?" + query + "&signature=" + signature;
    }
}