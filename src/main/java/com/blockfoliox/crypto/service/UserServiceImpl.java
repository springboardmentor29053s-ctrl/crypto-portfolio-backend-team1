package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.dto.*;
import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDTO registerUser(UserDTO dto, String password) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(password);

        userRepo.save(user);
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public UserDTO login(LoginRequestDTO req) {
        User user = userRepo.findByEmailAndPassword(req.getEmail(), req.getPassword());
        if (user == null) return null;
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Override
    public String forgotPassword(ForgotPasswordDTO req) {
        User user = userRepo.findByEmail(req.getEmail());
        if (user == null) return "User not found";

        user.setPassword(req.getNewPassword());
        userRepo.save(user);
        return "Password Updated";
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());
    }
}
