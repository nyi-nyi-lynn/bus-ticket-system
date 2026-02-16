package com.busticket.dao;

import com.busticket.model.User;

import java.util.List;

public interface UserDAO {
    User findByEmail(String email);

    boolean save(User user);

    List<User> findAll();
}
