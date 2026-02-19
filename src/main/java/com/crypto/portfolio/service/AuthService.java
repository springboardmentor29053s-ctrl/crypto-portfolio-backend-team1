package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.Login_dto;
import com.crypto.portfolio.dto.Register_dto;
import com.crypto.portfolio.model.Register_model;
import com.crypto.portfolio.repository.UserRepository;
import com.crypto.portfolio.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public Register_model register(Register_dto request){
        Register_model user = new Register_model();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // plain for now
        user.setCreated_at(LocalDateTime.now());
        return userRepository.save(user);
    }

    public String login(Login_dto request) {

        Register_model user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(user.getUsername());
    }


    public void forgotPassword(String email) {

        userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User with this email does not exist")
                );
    }

    public void resetPassword(String email, String newPassword) {

        Register_model user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        user.setPassword(newPassword); // later → BCrypt
        userRepository.save(user);
    }

}
