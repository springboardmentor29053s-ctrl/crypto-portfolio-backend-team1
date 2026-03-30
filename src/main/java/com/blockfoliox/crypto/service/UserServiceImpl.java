package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.dto.*;
import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepo;

    public UserServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDTO registerUser(UserDTO dto, String password) {
        log.info("Registering user email={}", dto.getEmail());
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(password);
        userRepo.save(user);
        log.info(" User registered email={}", dto.getEmail());
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public UserDTO login(LoginRequestDTO req) {
        log.info("Login attempt email={}", req.getEmail());
        User user = userRepo.findByEmailAndPassword(req.getEmail(), req.getPassword());
        if (user == null) {
            log.warn(" Login failed for email={}", req.getEmail());
            return null;
        }
        log.info(" Login successful email={}", req.getEmail());
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public String forgotPassword(ForgotPasswordDTO req) {
        log.info("Password reset request for email={}", req.getEmail());
        User user = userRepo.findByEmail(req.getEmail());
        if (user == null) {
            log.warn(" User not found for email={}", req.getEmail());
            return "User not found";
        }
        user.setPassword(req.getNewPassword());
        userRepo.save(user);
        log.info(" Password updated for email={}", req.getEmail());
        return "Password Updated";
    }

    @Override
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        return userRepo.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());
    }
}