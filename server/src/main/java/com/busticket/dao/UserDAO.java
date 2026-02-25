package com.busticket.dao;

import com.busticket.dto.CreateUserRequest;
import com.busticket.dto.UserDTO;
import com.busticket.model.User;

import java.sql.SQLException;
import java.util.List;

public interface UserDAO {
    User findById(Long userId);

    User findByEmail(String email);

    boolean save(User user);

    boolean update(User user);

    boolean deactivate(Long userId);

    List<User> findAll();

    boolean existsByEmail(String email);

    UserDTO insert(CreateUserRequest req, String passwordHash) throws SQLException;
}
