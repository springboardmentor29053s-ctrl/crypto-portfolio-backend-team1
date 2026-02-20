package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.dto.*;
import java.util.List;

public interface UserService {
    UserDTO registerUser(UserDTO userDTO, String password);
    UserDTO login(LoginRequestDTO request);
    String forgotPassword(ForgotPasswordDTO request);
    List<UserDTO> getAllUsers();
}
