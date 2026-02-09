package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Register User
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userRepository.save(user);
    }
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    @PostMapping("/login")
    public String login(@RequestBody User loginUser) {

        User dbUser = userRepository.findByEmail(loginUser.getEmail());

        if (dbUser == null) {
            return "User not found";
        }

        if (!dbUser.getPassword().equals(loginUser.getPassword())) {
            return "Wrong password";
        }

        return "Login successful";
    }

}
