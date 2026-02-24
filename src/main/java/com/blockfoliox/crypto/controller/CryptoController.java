package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.Crypto;
import com.blockfoliox.crypto.repository.CryptoRepository;
import com.blockfoliox.crypto.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "http://localhost:3000")
public class CryptoController {

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private CryptoRepository cryptoRepository;

    // ✅ Fix: returns proper JSON, not raw string
    @GetMapping(value = "/prices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPrices() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cryptoService.getCryptoPrices());
    }

    // ✅ Fix: these were missing — MySQL portfolio endpoints
    @GetMapping
    public List<Crypto> getAllCryptos() {
        return cryptoRepository.findAll();
    }

    @PostMapping
    public Crypto addCrypto(@RequestBody Crypto crypto) {
        return cryptoRepository.save(crypto);
    }
}