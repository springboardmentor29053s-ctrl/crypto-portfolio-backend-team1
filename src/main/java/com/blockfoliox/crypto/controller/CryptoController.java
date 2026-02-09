package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.Crypto;
import com.blockfoliox.crypto.repository.CryptoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoRepository repo;

    public CryptoController(CryptoRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Crypto addCrypto(@RequestBody Crypto crypto) {
        return repo.save(crypto);
    }

    @GetMapping
    public List<Crypto> getAllCrypto() {
        return repo.findAll();
    }
}
