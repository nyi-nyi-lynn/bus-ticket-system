package com.busticket.service.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.BookingDAOImpl;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.PassengerProfileDTO;
import com.busticket.dto.PassengerProfileUpdateDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.model.User;
import com.busticket.service.PassengerProfileService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class PassengerProfileServiceImpl implements PassengerProfileService {
    private static final DateTimeFormatter MEMBER_SINCE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BookingDAO bookingDAO;
    private final UserDAO userDAO;

    public PassengerProfileServiceImpl() {
        this.bookingDAO = new BookingDAOImpl(DatabaseConnection.getConnection());
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public PassengerProfileDTO getProfile(Long userId) throws ValidationException, UnauthorizedException {
        User user = ensurePassenger(userId);

        Map<String, Long> statusCounts = bookingDAO.countBookingsByStatus(userId);
        long totalBookings = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long completedTrips = statusCounts.getOrDefault("CONFIRMED", 0L);
        long cancelledTrips = statusCounts.getOrDefault("CANCELLED", 0L);

        PassengerProfileDTO dto = new PassengerProfileDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(safe(user.getName()));
        dto.setUsername(resolveUsername(user.getEmail()));
        dto.setEmail(safe(user.getEmail()));
        dto.setRole(user.getRole() == null ? Role.PASSENGER.name() : user.getRole().name());
        dto.setPhone(user.getPhone() == null ? "" : user.getPhone().trim());
        dto.setMemberSince(formatMemberSince(user.getCreatedAt()));
        dto.setTotalBookings(totalBookings);
        dto.setCompletedTrips(completedTrips);
        dto.setCancelledTrips(cancelledTrips);
        return dto;
    }

    @Override
    public void updateProfile(PassengerProfileUpdateDTO dto) throws ValidationException, UnauthorizedException {
        if (dto == null || dto.getUserId() == null) {
            throw new ValidationException("MISSING_PASSENGER_CONTEXT");
        }
        User user = ensurePassenger(dto.getUserId());

        String firstName = safe(dto.getFirstName());
        if (firstName.isBlank()) {
            throw new ValidationException("FIRST_NAME_REQUIRED");
        }
        String lastName = safe(dto.getLastName());
        String fullName = buildFullName(firstName, lastName);
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

        User updated = new User();
        updated.setUserId(user.getUserId());
        updated.setName(fullName);
        updated.setEmail(user.getEmail());
        updated.setPassword(user.getPassword());
        updated.setPhone(phone);
        updated.setRole(user.getRole());
        updated.setStatus(user.getStatus());

        if (!userDAO.update(updated)) {
            throw new ValidationException("PROFILE_UPDATE_FAILED");
        }
    }

    private User ensurePassenger(Long userId) throws ValidationException, UnauthorizedException {
        if (userId == null) {
            throw new ValidationException("MISSING_PASSENGER_CONTEXT");
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ValidationException("PASSENGER_NOT_FOUND");
        }
        if (user.getRole() != Role.PASSENGER) {
            throw new UnauthorizedException("FORBIDDEN_ONLY_PASSENGER");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("INACTIVE_ACCOUNT");
        }
        return user;
    }

    private String resolveUsername(String email) {
        if (email == null || email.isBlank()) {
            return "user";
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex > 0) {
            return trimmed.substring(0, atIndex);
        }
        return trimmed;
    }

    private String buildFullName(String firstName, String lastName) {
        String first = safe(firstName);
        String last = safe(lastName);
        if (last.isBlank()) {
            return first;
        }
        return first + " " + last;
    }

    private String formatMemberSince(Timestamp createdAt) {
        if (createdAt == null) {
            return "-";
        }
        LocalDateTime dateTime = createdAt.toLocalDateTime();
        return dateTime.format(MEMBER_SINCE_FMT);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
