package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.JwtResponse_dto;
import com.crypto.portfolio.dto.Login_dto;
import com.crypto.portfolio.dto.Register_dto;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.model.Wallet;
import com.crypto.portfolio.repository.UserRepository;
import com.crypto.portfolio.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(Register_dto request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // plain for now
        user.setCreatedAt(LocalDateTime.now());

        // Create Wallet
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(1000000.0);   // starting demo

        // Link wallet to user
        user.setWallet(wallet);

        // Save user (wallet auto-saves because of cascade)
        return userRepository.save(user);
    }

    public JwtResponse_dto login(Login_dto request) {

        User user = userRepository
                .findByName(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getName());

        return new JwtResponse_dto(
                token,
                "Bearer",
                user.getName()
        );
    }


    public void forgotPassword(String email) {

        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User with this email does not exist")
                );
    }

    public void resetPassword(String email, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        user.setPassword(newPassword); // later → BCrypt
        userRepository.save(user);
    }

}
