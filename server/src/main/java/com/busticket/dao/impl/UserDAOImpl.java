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
    private static final Map<String, User> MEMORY_USERS = new ConcurrentHashMap<>();
    private static final AtomicLong MEMORY_ID_SEQUENCE = new AtomicLong(1);

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        if (connection == null) {
            return MEMORY_USERS.get(normalizeEmail(email));
        }

        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setPhone(rs.getString("phone"));
                user.setRole(Role.valueOf(rs.getString("role")));
                user.setStatus(UserStatus.valueOf(rs.getString("status")));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean save(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        if (connection == null) {
            String key = normalizeEmail(user.getEmail());
            if (MEMORY_USERS.containsKey(key)) {
                return false;
            }
            if (user.getUserId() == null) {
                user.setUserId(MEMORY_ID_SEQUENCE.getAndIncrement());
            }
            MEMORY_USERS.put(key, user);
            return true;
        }

        String sql = "INSERT INTO users(name, email, password, phone, role, status) VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getStatus().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<User> findAll() {
        if (connection == null) {
            return new ArrayList<>(MEMORY_USERS.values());
        }

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setPhone(rs.getString("phone"));
                user.setRole(Role.valueOf(rs.getString("role")));
                user.setStatus(UserStatus.valueOf(rs.getString("status")));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
