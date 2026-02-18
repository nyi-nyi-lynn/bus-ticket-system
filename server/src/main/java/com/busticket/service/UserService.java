package com.busticket.service;

import com.busticket.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO login(String email, String password);

    boolean register(UserDTO dto);

    UserDTO getUserById(Long userId);

    boolean updateUser(UserDTO dto);

    boolean deleteUser(Long userId);

    List<UserDTO> getAllUsers();
}
