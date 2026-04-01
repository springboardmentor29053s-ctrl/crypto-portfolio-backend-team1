package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.*;
import com.crypto.portfolio.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/register")
    public String register(@RequestBody Register_dto request){
        authService.register(request);
        return "User Registered successfully";
    }

    @PostMapping("/login")
    public JwtResponse_dto login(@RequestBody Login_dto request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestBody ForgotPassword_dto request
    ) {
        authService.forgotPassword(request.getEmail());
        return "User verified. Proceed to reset password.";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestBody ResetPassword_dto request
    ) {
        authService.resetPassword(
                request.getEmail(),
                request.getNewPassword()
        );
        return "Password updated successfully";
    }

}
