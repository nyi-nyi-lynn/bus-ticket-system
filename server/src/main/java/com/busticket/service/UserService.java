package com.busticket.service;

import com.busticket.dto.CreateUserRequest;
import com.busticket.dto.UserDTO;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;

import java.util.List;

public interface UserService {
    UserDTO login(String email, String password);

    boolean register(UserDTO dto);

    UserDTO getUserById(Long userId);

    boolean updateUser(UserDTO dto);

    boolean deactivateUser(Long userId);

    List<UserDTO> getAllUsers();

    UserDTO createUser(CreateUserRequest request) throws DuplicateResourceException, ValidationException;
}
