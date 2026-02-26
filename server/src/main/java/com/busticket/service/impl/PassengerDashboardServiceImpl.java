package com.busticket.service.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.BookingDAOImpl;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingSummaryDTO;
import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.dto.RecentBookingDTO;
import com.busticket.dto.UpcomingTripDTO;
import com.busticket.dto.UserSummaryDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.model.User;
import com.busticket.service.PassengerDashboardService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PassengerDashboardServiceImpl implements PassengerDashboardService {
    private static final int RECENT_BOOKINGS_LIMIT = 10;
    private static final double CANCELLATION_RATE_ALERT = 0.2;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingDAO bookingDAO;
    private final UserDAO userDAO;

    public PassengerDashboardServiceImpl() {
        this.bookingDAO = new BookingDAOImpl(DatabaseConnection.getConnection());
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public PassengerDashboardDTO getDashboard(Long userId) throws ValidationException, UnauthorizedException {
        User user = ensurePassenger(userId);

        UserSummaryDTO userSummary = new UserSummaryDTO();
        userSummary.setUserId(user.getUserId());
        userSummary.setName(user.getName());
        userSummary.setEmail(user.getEmail());

        Map<String, Long> statusCounts = bookingDAO.countBookingsByStatus(userId);
        long totalBookings = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long cancelledBookings = statusCounts.getOrDefault("CANCELLED", 0L);
        long upcomingTrips = bookingDAO.countUpcomingTrips(userId);
        long completedTrips = bookingDAO.countCompletedTrips(userId);

        BookingSummaryDTO bookingSummary = new BookingSummaryDTO();
        bookingSummary.setTotalBookings(totalBookings);
        bookingSummary.setUpcomingTrips(upcomingTrips);
        bookingSummary.setCompletedTrips(completedTrips);
        bookingSummary.setCancelledBookings(cancelledBookings);

        UpcomingTripDTO upcomingTrip = bookingDAO.findNextUpcomingTrip(userId);
        List<RecentBookingDTO> recentBookings = bookingDAO.findRecentBookings(userId, RECENT_BOOKINGS_LIMIT);
        List<String> notifications = buildNotifications(upcomingTrip, statusCounts, totalBookings, cancelledBookings);

        PassengerDashboardDTO dashboard = new PassengerDashboardDTO();
        dashboard.setUserSummary(userSummary);
        dashboard.setBookingSummary(bookingSummary);
        dashboard.setUpcomingTrip(upcomingTrip);
        dashboard.setRecentBookings(recentBookings);
        dashboard.setNotifications(notifications);
        dashboard.setGeneratedAt(LocalDateTime.now());
        return dashboard;
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

    private List<String> buildNotifications(UpcomingTripDTO upcomingTrip,
                                            Map<String, Long> statusCounts,
                                            long totalBookings,
                                            long cancelledBookings) {
        List<String> notifications = new ArrayList<>();

        if (upcomingTrip != null) {
            LocalDate date = upcomingTrip.getTravelDate();
            LocalTime time = upcomingTrip.getDepartureTime();
            if (date != null && time != null) {
                LocalDateTime departure = LocalDateTime.of(date, time);
                Duration until = Duration.between(LocalDateTime.now(), departure);
                if (!until.isNegative() && until.toHours() <= 24) {
                    notifications.add("Trip within 24 hours: " + routeLabel(upcomingTrip)
                            + " at " + departure.format(DATE_TIME_FMT));
                }
            }
        }

        long pending = statusCounts.getOrDefault("PENDING", 0L);
        if (pending > 0) {
            notifications.add("Payment pending: " + pending + " booking(s).");
        }

        if (totalBookings > 0) {
            double ratio = (double) cancelledBookings / totalBookings;
            if (ratio >= CANCELLATION_RATE_ALERT) {
                notifications.add("High cancellation rate: " + String.format(Locale.ROOT, "%.1f%%", ratio * 100.0)
                        + " (" + cancelledBookings + " of " + totalBookings + ").");
            }
        }

        if (notifications.isEmpty()) {
            notifications.add("No notifications at the moment.");
        }

        return notifications;
    }

    private String routeLabel(UpcomingTripDTO trip) {
        if (trip == null) {
            return "-";
        }
        String origin = safe(trip.getOriginCity());
        String dest = safe(trip.getDestinationCity());
        return origin + " -> " + dest;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
