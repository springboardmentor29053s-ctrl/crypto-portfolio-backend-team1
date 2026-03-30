package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import com.blockfoliox.crypto.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public String registerUser(@RequestBody User user) {
        log.info("Register request for email={} name={} password={}",
                user.getEmail(), user.getName(), user.getPassword());
        User existing = userRepository.findByEmail(user.getEmail());
        if (existing != null) {
            log.warn("⚠️ Email already exists: {}", user.getEmail());
            return "Email already exists";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        log.info("✅ User registered email={}", user.getEmail());
        return "User registered successfully";
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User loginUser) {
        log.info("Login attempt email={} password={}", loginUser.getEmail(), loginUser.getPassword());
        User dbUser = userRepository.findByEmail(loginUser.getEmail());

        if (dbUser == null) {
            log.warn("⚠️ User not found email={}", loginUser.getEmail());
            return Map.of("error", "User not found");
        }

        log.info("DB password={}", dbUser.getPassword());
        log.info("Input password={}", loginUser.getPassword());

        boolean matches = passwordEncoder.matches(loginUser.getPassword(), dbUser.getPassword());
        log.info("Password matches={}", matches);

        if (!matches) {
            log.warn("⚠️ Wrong password for email={}", loginUser.getEmail());
            return Map.of("error", "Wrong password");
        }

        String token = jwtUtil.generateToken(dbUser.getEmail());
        log.info("✅ Login successful email={} userId={}", loginUser.getEmail(), dbUser.getId());
        return Map.of("token", token, "userId", String.valueOf(dbUser.getId()));
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody Map<String, String> body) {
        log.info("Password reset request for email={}", body.get("email"));
        User user = userRepository.findByEmail(body.get("email"));
        if (user == null) {
            log.warn("⚠️ User not found for email={}", body.get("email"));
            return "User not found";
        }
        user.setPassword(passwordEncoder.encode(body.get("newPassword")));
        userRepository.save(user);
        log.info("✅ Password reset for email={}", body.get("email"));
        return "Password reset successfully";
    }

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable Long id) {
        log.info("Fetching user by id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/generate-hash/{password}")
    public String generateHash(@PathVariable String password) {
        return passwordEncoder.encode(password);
    }
}