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
import com.busticket.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class UserServiceImpl implements UserService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public UserDTO login(String email, String password) {
        User user = userDAO.findByEmail(email);
        if (user == null) {
            return null;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            UserDTO blocked = new UserDTO();
            blocked.setStatus(UserStatus.BLOCKED.name());
            return blocked;
        }

        if (!PasswordUtil.verify(password, user.getPassword())) {
            UserDTO invalid = new UserDTO();
            invalid.setStatus("INVALID_PASSWORD");
            return invalid;
        }

        return toDTO(user);
    }

    @Override
    public boolean register(UserDTO dto) {
        if (dto == null || dto.getEmail() == null || dto.getPassword() == null) {
            return false;
        }
        if (userDAO.findByEmail(dto.getEmail()) != null) {
            return false;
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(PasswordUtil.hash(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(parseRoleOrDefault(dto.getRole(), Role.PASSENGER));
        user.setStatus(parseStatusOrDefault(dto.getStatus(), UserStatus.ACTIVE));
        return userDAO.save(user);
    }

    @Override
    public boolean updateUser(UserDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            return false;
        }

        User existing = userDAO.findById(dto.getUserId());
        if (existing == null) {
            return false;
        }

        String name = dto.getName() == null ? "" : dto.getName().trim();
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (name.isBlank() || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }
        if (userDAO.existsByEmailExcludingUserId(dto.getUserId(), email)) {
            return false;
        }

        User user = new User();
        user.setUserId(dto.getUserId());
        user.setName(name);
        user.setEmail(email);
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            user.setPassword(existing.getPassword());
        } else {
            user.setPassword(PasswordUtil.hash(dto.getPassword()));
        }
        user.setPhone(phone);
        user.setRole(parseRoleOrDefault(dto.getRole(), existing.getRole()));
        user.setStatus(parseStatusOrDefault(dto.getStatus(), existing.getStatus()));

        return userDAO.update(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userDAO.findAll();
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(toDTO(user));
        }
        return dtos;
    }

    @Override
    public UserDTO createUser(CreateUserRequest request) throws DuplicateResourceException, ValidationException {
        validateCreateRequest(request);
        ensureAdminActor(request.getRequestedByUserId());

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userDAO.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("EMAIL_EXISTS");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordUtil.hash(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setRole(parseRoleStrict(request.getRole()));
        user.setStatus(parseStatusOrDefaultStrict(request.getStatus(), UserStatus.ACTIVE));

        try {
            User created = userDAO.insert(user);
            if (created == null) {
                throw new ValidationException("CREATE_USER_FAILED");
            }
            return toDTO(created);
        } catch (DuplicateResourceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ValidationException("CREATE_USER_FAILED");
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

    private void validateCreateRequest(CreateUserRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("INVALID_REQUEST");
        }
        if (request.getRequestedByUserId() == null) {
            throw new ValidationException("MISSING_ADMIN_CONTEXT");
        }

        String name = requireText(request.getName(), "NAME_REQUIRED");
        if (name.length() > 120) {
            throw new ValidationException("NAME_TOO_LONG");
        }

        String email = requireText(request.getEmail(), "EMAIL_REQUIRED").toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("INVALID_EMAIL");
        }

        requireText(request.getPhone(), "PHONE_REQUIRED");

        String password = requireText(request.getPassword(), "PASSWORD_REQUIRED");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("PASSWORD_TOO_SHORT");
        }

        parseRoleStrict(request.getRole());
        parseStatusOrDefaultStrict(request.getStatus(), UserStatus.ACTIVE);
    }

    private void ensureAdminActor(Long requestedByUserId) throws ValidationException {
        User actor = userDAO.findById(requestedByUserId);
        if (actor == null || actor.getStatus() != UserStatus.ACTIVE || actor.getRole() != Role.ADMIN) {
            throw new ValidationException("FORBIDDEN_ONLY_ADMIN");
        }
    }

    private Role parseRoleStrict(String role) throws ValidationException {
        if (role == null || role.isBlank()) {
            throw new ValidationException("ROLE_REQUIRED");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("INVALID_ROLE");
        }
    }

    private UserStatus parseStatusOrDefaultStrict(String status, UserStatus fallback) throws ValidationException {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("INVALID_STATUS");
        }
    }

    private String requireText(String value, String code) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(code);
        }
        return value.trim();
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
}
