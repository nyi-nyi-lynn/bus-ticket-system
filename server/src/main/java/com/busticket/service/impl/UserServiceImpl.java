package com.busticket.service.impl;

import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dao.UserDAO;
import com.busticket.dto.UserDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.mapper.UserMapper;
import com.busticket.model.User;
import com.busticket.service.UserService;
import com.busticket.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
        seedDefaultUsers();
    }

    @Override
    public UserDTO login(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return null;
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            return null;
        }

        String hashedInput = PasswordUtil.hashPassword(password);
        if (!hashedInput.equals(user.getPassword())) {
            return null;
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            return null;
        }

        return UserMapper.toDTO(user);
    }

    @Override
    public boolean register(UserDTO dto) {
        if (dto == null || dto.getEmail() == null || dto.getEmail().isBlank()
                || dto.getName() == null || dto.getName().isBlank()
                || dto.getPassword() == null || dto.getPassword().isBlank()) {
            return false;
        }

        if (userDAO.findByEmail(dto.getEmail()) != null) {
            return false;
        }

        User user = UserMapper.toEntity(dto);
        user.setPassword(PasswordUtil.hashPassword(dto.getPassword()));
        user.setRole(parseRole(dto.getRole()));
        user.setStatus(parseStatus(dto.getStatus()));

        return userDAO.save(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<UserDTO> result = new ArrayList<>();
        for (User user : userDAO.findAll()) {
            result.add(UserMapper.toDTO(user));
        }
        return result;
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.PASSENGER;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.PASSENGER;
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return UserStatus.ACTIVE;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserStatus.ACTIVE;
        }
    }

    private void seedDefaultUsers() {
        String adminEmail = "admin@busticket.com";
        if (userDAO.findByEmail(adminEmail) == null) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(PasswordUtil.hashPassword("admin123"));
            admin.setPhone("0000000000");
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userDAO.save(admin);
        }

        String staffEmail = "staff@busticket.com";
        if (userDAO.findByEmail(staffEmail) == null) {
            User staff = new User();
            staff.setName("System Staff");
            staff.setEmail(staffEmail);
            staff.setPassword(PasswordUtil.hashPassword("staff123"));
            staff.setPhone("0000000001");
            staff.setRole(Role.STAFF);
            staff.setStatus(UserStatus.ACTIVE);
            userDAO.save(staff);
        }
    }
}
