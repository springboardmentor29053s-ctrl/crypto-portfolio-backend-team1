package com.crypto.cryptoPortfolio.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.crypto.cryptoPortfolio.dto.LoginRequest;
import com.crypto.cryptoPortfolio.dto.SignupRequest;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LoginController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.crypto.cryptoPortfolio.service.JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User u = repo.findByEmail(request.getEmail()).orElse(null);

        if (u == null || !passwordEncoder.matches(request.getPassword(), u.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Credentials");
        }


        String token = jwtService.generateToken(u.getEmail());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", u.getEmail(),
                "name", u.getName()
        ));
    }

//    @PostMapping("/signup")
//    public ResponseEntity<String> signup(
//            @Valid @RequestBody SignupRequest request) {
//
//        if (repo.existsByEmail(request.getEmail())) {
//            return ResponseEntity
//                    .badRequest()
//                    .body("Email already registered");
//        }
//
//        User user = new User();
//        user.setName(request.getName());
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//
//        repo.save(user);
//        return ResponseEntity.ok("Registration Successful");
//    }

//    @GetMapping("/test-auth")
//    public String testAuth(Authentication authentication) {
//        return "Logged in as: " + authentication.getName();
//    }

}
