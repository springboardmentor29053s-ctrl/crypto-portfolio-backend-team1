package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.Crypto;
import com.blockfoliox.crypto.repository.CryptoRepository;
import com.blockfoliox.crypto.service.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "http://localhost:3000")
public class CryptoController {

    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);

    private final CryptoService cryptoService;
    private final CryptoRepository cryptoRepository;

    public CryptoController(CryptoService cryptoService,
                            CryptoRepository cryptoRepository) {
        this.cryptoService = cryptoService;
        this.cryptoRepository = cryptoRepository;
    }

    @GetMapping(value = "/prices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPrices() {
        log.info("Fetching crypto prices");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cryptoService.getCryptoPrices());
    }

    @GetMapping
    public List<Crypto> getAllCryptos() {
        log.info("Fetching all cryptos from DB");
        return cryptoRepository.findAll();
    }

    @PostMapping
    public Crypto addCrypto(@RequestBody Crypto crypto) {
        log.info("Adding crypto name={}", crypto.getName());
        return cryptoRepository.save(crypto);
    }
}