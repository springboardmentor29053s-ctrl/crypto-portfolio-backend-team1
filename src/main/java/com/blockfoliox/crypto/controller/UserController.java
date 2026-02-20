package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import com.blockfoliox.crypto.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ REGISTER USER
    @PostMapping("/register")
    public String registerUser(@RequestBody User user) {

        User existing = userRepository.findByEmail(user.getEmail());
        if (existing != null) {
            return "Email already exists";
        }

        userRepository.save(user);
        return "User registered successfully";
    }


    // ✅ GET ALL USERS (for testing)
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ LOGIN USER
    @PostMapping("/login")
    public String login(@RequestBody User loginUser) {

        User dbUser = userRepository.findByEmail(loginUser.getEmail());

        if (dbUser == null) {
            return "User not found";
        }

        if (!dbUser.getPassword().equals(loginUser.getPassword())) {
            return "Wrong password";
        }

        return jwtUtil.generateToken(dbUser.getEmail());
    }

}
