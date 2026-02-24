package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import com.blockfoliox.crypto.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER
    @PostMapping("/register")
    public String registerUser(@RequestBody User user) {
        User existing = userRepository.findByEmail(user.getEmail());
        if (existing != null) {
            return "Email already exists";
        }
        userRepository.save(user);
        return "User registered successfully";
    }

    // GET USERS
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // LOGIN
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User loginUser) {
        User dbUser = userRepository.findByEmail(loginUser.getEmail());

        if (dbUser == null) {
            return Map.of("error", "User not found");
        }

        if (!dbUser.getPassword().equals(loginUser.getPassword())) {
            return Map.of("error", "Wrong password");
        }

        String token = jwtUtil.generateToken(dbUser.getEmail());
        return Map.of("token", token);
    }
}