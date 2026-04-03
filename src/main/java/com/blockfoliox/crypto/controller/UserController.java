package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import com.blockfoliox.crypto.security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody User user,
                                               BindingResult result) {
        // ✅ Return validation errors if any
        if (result.hasErrors()) {
            String errors = result.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            log.warn("⚠️ Validation failed: {}", errors);
            return ResponseEntity.badRequest().body(errors);
        }

        log.info("Register request for email={}", user.getEmail());
        User existing = userRepository.findByEmail(user.getEmail());
        if (existing != null) {
            log.warn("⚠️ Email already exists: {}", user.getEmail());
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        log.info("✅ User registered email={}", user.getEmail());
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginUser) {
        // ✅ Validate email and password not empty
        if (loginUser.getEmail() == null || loginUser.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (loginUser.getPassword() == null || loginUser.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        log.info("Login request for email={}", loginUser.getEmail());
        User dbUser = userRepository.findByEmail(loginUser.getEmail());

        if (dbUser == null) {
            log.warn("⚠️ User not found email={}", loginUser.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        if (!passwordEncoder.matches(loginUser.getPassword(), dbUser.getPassword())) {
            log.warn("⚠️ Wrong password for email={}", loginUser.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Wrong password"));
        }

        String token = jwtUtil.generateToken(dbUser.getEmail());
        log.info("✅ Login successful email={} userId={}", loginUser.getEmail(), dbUser.getId());
        return ResponseEntity.ok(Map.of("token", token, "userId", String.valueOf(dbUser.getId())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");

        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body("Email is required");
        if (newPassword == null || newPassword.isBlank())
            return ResponseEntity.badRequest().body("New password is required");
        if (newPassword.length() < 4)
            return ResponseEntity.badRequest().body("Password must be at least 4 characters");

        User user = userRepository.findByEmail(email);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("✅ Password reset for email={}", email);
        return ResponseEntity.ok("Password reset successfully");
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        log.info("Fetching user by id={}", id);
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}