package com.busticket.dao;

import com.busticket.model.User;

import java.util.List;

public interface UserDAO {
    User findById(Long userId);

    User findByEmail(String email);

    boolean save(User user);

    boolean update(User user);

    boolean deactivate(Long userId);

    List<User> findAll();
}
