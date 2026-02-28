package com.busticket.dao.impl;

import com.busticket.dao.UserDAO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAOImpl implements UserDAO {

    private final Connection connection;

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findById(Long userId) {
        String sql = "SELECT user_id, name, email, password, phone, role, status, created_at, updated_at FROM users WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password, phone, role, status, created_at, updated_at FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean existsByEmailExcludingUserId(Long userId, String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? AND user_id <> ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public User insert(User user) throws DuplicateResourceException {
        String sql = "INSERT INTO users(name, email, password, phone, role, status) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            return findByEmail(user.getEmail());
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new DuplicateResourceException("EMAIL_EXISTS");
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                throw new DuplicateResourceException("EMAIL_EXISTS");
            }
            throw new RuntimeException("Failed to insert user.", ex);
        }
    }

    @Override
    public boolean save(User user) {
        try {
            return insert(user) != null;
        } catch (DuplicateResourceException ex) {
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, password = ?, phone = ?, role = ?, status = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getStatus().name());
            ps.setLong(7, user.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deactivate(Long userId) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UserStatus.BLOCKED.name());
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT user_id, name, email, password, phone, role, status, created_at, updated_at FROM users ORDER BY user_id DESC";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setRole(parseRoleOrDefault(rs.getString("role")));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }

    private Role parseRoleOrDefault(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return Role.PASSENGER;
        }
        try {
            return Role.valueOf(roleValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Role.PASSENGER;
        }
    }
}
