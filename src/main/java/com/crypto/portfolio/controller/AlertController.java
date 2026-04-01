package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.AlertRequest;
import com.crypto.portfolio.dto.AlertResponse;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.AlertRepository;
import com.crypto.portfolio.repository.ExchangeRepository;
import com.crypto.portfolio.repository.UserRepository;
import com.crypto.portfolio.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;

    @PostMapping
    public String createAlert(@RequestBody AlertRequest request) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        alertService.createAlert(username, request);

        return "Alert created successfully";
    }

    @GetMapping
    public List<AlertResponse> getAlerts() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();


        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return alertRepository.findByUserAndTriggeredFalse(user)
                .stream()
                .map(a -> new AlertResponse(
                        a.getType(),
                        a.getSymbol(),
                        a.getTargetValue(),
                        a.getTriggered()
                ))
                .toList();
    }
}