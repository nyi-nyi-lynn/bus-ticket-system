package com.busticket.service.impl;

import com.busticket.dao.DashboardDAO;
import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.DashboardDAOImpl;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.ChartPointDTO;
import com.busticket.dto.DashboardResponseDTO;
import com.busticket.dto.KpiDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.ValidationException;
import com.busticket.model.User;
import com.busticket.service.DashboardService;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardServiceImpl implements DashboardService {
    private static final int TREND_DAYS = 30;
    private static final int RECENT_BOOKINGS_LIMIT = 10;
    private static final int TOP_ROUTES_LIMIT = 5;
    private static final int LOW_OCCUPANCY_LOOKAHEAD_DAYS = 7;
    private static final int LOW_OCCUPANCY_LIMIT = 5;
    private static final double LOW_OCCUPANCY_THRESHOLD = 0.4;
    private static final double CANCELLATION_RATE_ALERT = 0.2;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DashboardDAO dashboardDAO;
    private final UserDAO userDAO;

    public DashboardServiceImpl() {
        this.dashboardDAO = new DashboardDAOImpl(DatabaseConnection.getConnection());
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public DashboardResponseDTO getDashboardSummary(Long requestedByUserId) throws ValidationException {
        ensureAdminActor(requestedByUserId);

        double revenueToday = dashboardDAO.fetchRevenueToday();
        double revenueMonth = dashboardDAO.fetchRevenueThisMonth();
        long bookingsToday = dashboardDAO.fetchBookingsToday();
        long activeTrips = dashboardDAO.fetchActiveTrips();
        long activeBuses = dashboardDAO.fetchActiveBuses();
        long totalUsers = dashboardDAO.fetchTotalUsers();

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Total Revenue (Today)", MONEY_FORMAT.format(revenueToday), "Paid revenue"),
                new KpiDTO("Total Revenue (This Month)", MONEY_FORMAT.format(revenueMonth), "Paid revenue"),
                new KpiDTO("Total Bookings (Today)", String.valueOf(bookingsToday), "All statuses"),
                new KpiDTO("Active Trips", String.valueOf(activeTrips), "Open trips"),
                new KpiDTO("Active Buses", String.valueOf(activeBuses), "In service"),
                new KpiDTO("Total Users", String.valueOf(totalUsers), "All accounts")
        );

        LocalDate today = LocalDate.now();
        LocalDate trendFrom = today.minusDays(TREND_DAYS - 1L);
        List<RevenueRowDTO> trendRows = dashboardDAO.fetchRevenueTrend(trendFrom, today);
        List<ChartPointDTO> trendPoints = new ArrayList<>();
        for (RevenueRowDTO row : trendRows) {
            String label = row.getDate() == null ? "-" : row.getDate().format(DATE_FMT);
            trendPoints.add(new ChartPointDTO(label, row.getTotalRevenue()));
        }

        List<BookingRowDTO> bookingRows = dashboardDAO.fetchBookingStatusSummary(trendFrom, today);
        Map<String, Long> bookingStatusSummary = new LinkedHashMap<>();
        for (BookingRowDTO row : bookingRows) {
            String status = safeUpper(row.getBookingStatus());
            bookingStatusSummary.put(status, row.getTotalBookings());
        }

        List<RoutePopularityRowDTO> topRoutes = dashboardDAO.fetchTopRoutes(trendFrom, today, TOP_ROUTES_LIMIT);
        List<BookingDTO> recentBookings = dashboardDAO.fetchRecentBookings(RECENT_BOOKINGS_LIMIT);

        List<String> alerts = buildAlerts(trendFrom, today, bookingRows);

        DashboardResponseDTO response = new DashboardResponseDTO();
        response.setKpis(kpis);
        response.setRevenueTrend(trendPoints);
        response.setBookingStatusSummary(bookingStatusSummary);
        response.setTopRoutes(topRoutes);
        response.setRecentBookings(recentBookings);
        response.setAlerts(alerts);
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    private List<String> buildAlerts(LocalDate trendFrom, LocalDate trendTo, List<BookingRowDTO> bookingRows) {
        List<String> alerts = new ArrayList<>();

        long inactiveCount = dashboardDAO.fetchInactiveBuses();
        if (inactiveCount > 0) {
            alerts.add("Buses in maintenance: " + inactiveCount);
        }

        LocalDate now = LocalDate.now();
        LocalDate lookahead = now.plusDays(LOW_OCCUPANCY_LOOKAHEAD_DAYS);
        List<TripPerformanceRowDTO> lowTrips = dashboardDAO.fetchLowOccupancyTrips(now, lookahead, LOW_OCCUPANCY_THRESHOLD, LOW_OCCUPANCY_LIMIT);
        for (TripPerformanceRowDTO trip : lowTrips) {
            String route = safeText(trip == null ? null : trip.getRouteLabel());
            String date = trip == null || trip.getTravelDate() == null ? "-" : trip.getTravelDate().format(DATE_FMT);
            double occ = trip == null ? 0 : trip.getOccupancyRate() * 100.0;
            alerts.add("Low occupancy: " + route + " on " + date + " (" + String.format(Locale.ROOT, "%.0f%%", occ) + ")");
        }

        long totalBookings = bookingRows == null ? 0 : bookingRows.stream().mapToLong(BookingRowDTO::getTotalBookings).sum();
        long cancelled = bookingRows == null ? 0 : bookingRows.stream()
                .filter(row -> "CANCELLED".equalsIgnoreCase(row.getBookingStatus()))
                .mapToLong(BookingRowDTO::getTotalBookings)
                .sum();
        if (totalBookings > 0) {
            double ratio = (double) cancelled / totalBookings;
            if (ratio >= CANCELLATION_RATE_ALERT) {
                alerts.add("High cancellation rate: " + String.format(Locale.ROOT, "%.1f%%", ratio * 100.0)
                        + " (" + cancelled + " of " + totalBookings + ")");
            }
        }

        if (alerts.isEmpty()) {
            alerts.add("No critical alerts. Operations look stable.");
        }
        return alerts;
    }

    private void ensureAdminActor(Long userId) throws ValidationException {
        if (userId == null) {
            throw new ValidationException("MISSING_ADMIN_CONTEXT");
        }
        User actor = userDAO.findById(userId);
        if (actor == null || actor.getRole() != Role.ADMIN || actor.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("FORBIDDEN_ONLY_ADMIN");
        }
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String safeUpper(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
