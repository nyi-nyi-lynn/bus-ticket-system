package com.busticket.service.impl;

import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.CreateUserRequest;
import com.busticket.dto.UserDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.model.User;
import com.busticket.service.UserService;
import com.busticket.util.PasswordHasher;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class UserServiceImpl implements UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,15}$");

    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public UserDTO login(String email, String password) {
        // Validate required credentials.
        User user = userDAO.findByEmail(email);
        if (user == null) {
            return null;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            UserDTO blocked = new UserDTO();
            blocked.setStatus(UserStatus.BLOCKED.name());
            return blocked;
        }

        if (!PasswordHasher.matches(password, user.getPassword())) {
            UserDTO invalid = new UserDTO();
            invalid.setStatus("INVALID_PASSWORD");
            return invalid;
        }

        return toDTO(user);
    }

    @Override
    public boolean register(UserDTO dto) {
        // Validate required fields and uniqueness.
        if (dto == null || dto.getEmail() == null || dto.getPassword() == null) {
            return false;
        }
        if (userDAO.findByEmail(dto.getEmail()) != null) {
            return false;
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(PasswordHasher.hash(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(parseRoleOrDefault(dto.getRole(), Role.PASSENGER));
        user.setStatus(parseStatusOrDefault(dto.getStatus(), UserStatus.ACTIVE));
        return userDAO.save(user);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userDAO.findById(userId);
        return user == null ? null : toDTO(user);
    }

    @Override
    public boolean updateUser(UserDTO dto) {
        // Validate required fields and identifiers.
        if (dto == null || dto.getUserId() == null) {
            return false;
        }

        User existing = userDAO.findById(dto.getUserId());
        if (existing == null) {
            return false;
        }

        User user = new User();
        user.setUserId(dto.getUserId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            user.setPassword(existing.getPassword());
        } else {
            user.setPassword(PasswordHasher.hash(dto.getPassword()));
        }
        user.setPhone(dto.getPhone());
        user.setRole(parseRoleOrDefault(dto.getRole(), existing.getRole()));
        user.setStatus(parseStatusOrDefault(dto.getStatus(), existing.getStatus()));

        return userDAO.update(user);
    }

    @Override
    public boolean deactivateUser(Long userId) {
        // Soft delete: block user account.
        return userDAO.deactivate(userId);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users =  userDAO.findAll();
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(toDTO(user));
        }
        return dtos;
    }

    @Override
    public UserDTO createUser(CreateUserRequest req) throws DuplicateResourceException, ValidationException {
        validateCreateRequest(req);

        User actor = userDAO.findById(req.getCreatedByUserId());
        if (actor == null || actor.getRole() != Role.ADMIN) {
            throw new ValidationException("ADMIN_ONLY");
        }

        req.setName(req.getName().trim());
        req.setEmail(req.getEmail().trim().toLowerCase(Locale.ROOT));
        req.setPhone(req.getPhone().trim());
        req.setStatus(req.getStatus() == null ? UserStatus.ACTIVE : req.getStatus());

        if (userDAO.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("EMAIL_EXISTS");
        }

        String passwordHash = PasswordHasher.hash(req.getPassword());

        try {
            UserDTO created = userDAO.insert(req, passwordHash);
            if (created == null) {
                throw new ValidationException("CREATE_USER_FAILED");
            }
            return created;
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new DuplicateResourceException("EMAIL_EXISTS");
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create user.", ex);
        }
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole() == null ? null : user.getRole().name());
        dto.setStatus(user.getStatus() == null ? null : user.getStatus().name());
        dto.setCreatedAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
        dto.setUpdatedAt(user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString());
        return dto;
    }

    private Role parseRoleOrDefault(String role, Role fallback) {
        if (role == null || role.isBlank()) {
            return fallback;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private UserStatus parseStatusOrDefault(String status, UserStatus fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private void validateCreateRequest(CreateUserRequest req) throws ValidationException {
        if (req == null) {
            throw new ValidationException("REQUEST_REQUIRED");
        }
        if (req.getCreatedByUserId() == null) {
            throw new ValidationException("ADMIN_ONLY");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ValidationException("NAME_REQUIRED");
        }
        if (req.getEmail() == null || !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            throw new ValidationException("INVALID_EMAIL");
        }
        if (req.getPhone() == null || !PHONE_PATTERN.matcher(req.getPhone().trim()).matches()) {
            throw new ValidationException("INVALID_PHONE");
        }
        if (req.getRole() == null) {
            throw new ValidationException("INVALID_ROLE");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new ValidationException("PASSWORD_TOO_SHORT");
        }
    }
}
