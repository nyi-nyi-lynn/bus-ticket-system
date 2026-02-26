package com.busticket.dao;

import com.busticket.exception.DuplicateResourceException;
import com.busticket.model.User;

import java.util.List;

public interface UserDAO {
    User findById(Long userId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailExcludingUserId(Long userId, String email);

    User insert(User user) throws DuplicateResourceException;

    boolean save(User user);

    boolean update(User user);

    boolean deactivate(Long userId);

    List<User> findAll();
}
