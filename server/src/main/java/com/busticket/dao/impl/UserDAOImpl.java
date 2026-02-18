package com.busticket.dao.impl;

import com.busticket.dao.UserDAO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserDAOImpl implements UserDAO {

    private final Connection connection;

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }

    @Override
    public boolean save(User user) {
        return false;
    }

    @Override
    public List<User> findAll() {
        User user1 = new User(2L, "Aung Aung", "aung@gamil.com", "0987784648336", "member",Role.PASSENGER,UserStatus.ACTIVE );
        User user2= new User(1L, "Aung Aung", "aung@gamil.com", "0987784648336", "member",Role.PASSENGER,UserStatus.ACTIVE );

        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);

        return users;
    }
}

