package com.busticket.service.impl;

import com.busticket.dao.ReportDAO;
import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.ReportDAOImpl;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.ChartPointDTO;
import com.busticket.dto.CustomerActivityRowDTO;
import com.busticket.dto.KpiDTO;
import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.ReportResponseDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.PaymentStatus;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.ValidationException;
import com.busticket.model.User;
import com.busticket.service.ReportService;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {
    private static final int MAX_RANGE_DAYS = 366;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final ReportDAO reportDAO;
    private final UserDAO userDAO;

    public ReportServiceImpl() {
        this.reportDAO = new ReportDAOImpl(DatabaseConnection.getConnection());
        this.userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public ReportResponseDTO getRevenueReport(ReportFilterDTO filter) throws ValidationException {
        ReportFilterDTO normalized = normalizeFilter(filter);
        ensureAdminActor(normalized.getRequestedByUserId());

        List<RevenueRowDTO> rows = reportDAO.fetchRevenueSummary(normalized);
        long totalBookings = rows.stream().mapToLong(RevenueRowDTO::getTotalBookings).sum();
        double totalRevenue = rows.stream().mapToDouble(RevenueRowDTO::getTotalRevenue).sum();
        long days = Math.max(1, ChronoUnit.DAYS.between(normalized.getFromDate(), normalized.getToDate()) + 1);

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Total Revenue", MONEY_FORMAT.format(totalRevenue), "Paid revenue"),
                new KpiDTO("Total Bookings", String.valueOf(totalBookings), "Confirmed bookings"),
                new KpiDTO("Avg / Day", MONEY_FORMAT.format(totalRevenue / days), "Across date range")
        );

        List<ChartPointDTO> chartPoints = new ArrayList<>();
        for (RevenueRowDTO row : rows) {
            String label = row.getDate() == null ? "-" : row.getDate().toString();
            chartPoints.add(new ChartPointDTO(label, row.getTotalRevenue()));
        }

        return buildResponse(kpis, chartPoints, rows);
    }

    @Override
    public ReportResponseDTO getBookingReport(ReportFilterDTO filter) throws ValidationException {
        ReportFilterDTO normalized = normalizeFilter(filter);
        ensureAdminActor(normalized.getRequestedByUserId());

        List<BookingRowDTO> rows = reportDAO.fetchBookingSummary(normalized);
        long totalBookings = rows.stream().mapToLong(BookingRowDTO::getTotalBookings).sum();
        double totalRevenue = rows.stream().mapToDouble(BookingRowDTO::getTotalRevenue).sum();
        long confirmed = rows.stream()
                .filter(row -> equalsIgnoreCase(row.getBookingStatus(), BookingStatus.CONFIRMED.name()))
                .mapToLong(BookingRowDTO::getTotalBookings)
                .sum();

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Total Bookings", String.valueOf(totalBookings), "All statuses"),
                new KpiDTO("Confirmed", String.valueOf(confirmed), "Confirmed only"),
                new KpiDTO("Total Revenue", MONEY_FORMAT.format(totalRevenue), "Paid revenue")
        );

        List<ChartPointDTO> chartPoints = new ArrayList<>();
        for (BookingRowDTO row : rows) {
            String label = safeUpper(row.getBookingStatus());
            chartPoints.add(new ChartPointDTO(label, row.getTotalBookings()));
        }

        return buildResponse(kpis, chartPoints, rows);
    }

    @Override
    public ReportResponseDTO getTripPerformanceReport(ReportFilterDTO filter) throws ValidationException {
        ReportFilterDTO normalized = normalizeFilter(filter);
        ensureAdminActor(normalized.getRequestedByUserId());

        List<TripPerformanceRowDTO> rows = reportDAO.fetchTripPerformance(normalized);
        int tripCount = rows.size();
        long totalSold = rows.stream().mapToLong(TripPerformanceRowDTO::getSoldSeats).sum();
        long totalSeats = rows.stream().mapToLong(TripPerformanceRowDTO::getTotalSeats).sum();
        double totalRevenue = rows.stream().mapToDouble(TripPerformanceRowDTO::getTotalRevenue).sum();
        double avgOccupancy = totalSeats == 0 ? 0 : (double) totalSold / totalSeats;

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Trips", String.valueOf(tripCount), "In date range"),
                new KpiDTO("Avg Occupancy", percent(avgOccupancy), "Sold seats / capacity"),
                new KpiDTO("Total Revenue", MONEY_FORMAT.format(totalRevenue), "Paid revenue")
        );

        List<ChartPointDTO> chartPoints = rows.stream()
                .sorted(Comparator.comparingDouble(TripPerformanceRowDTO::getOccupancyRate).reversed())
                .limit(10)
                .map(row -> new ChartPointDTO(routeLabel(row), row.getOccupancyRate() * 100.0))
                .collect(Collectors.toList());

        return buildResponse(kpis, chartPoints, rows);
    }

    @Override
    public ReportResponseDTO getRoutePopularityReport(ReportFilterDTO filter) throws ValidationException {
        ReportFilterDTO normalized = normalizeFilter(filter);
        ensureAdminActor(normalized.getRequestedByUserId());

        List<RoutePopularityRowDTO> rows = reportDAO.fetchRoutePopularity(normalized);
        long totalBookings = rows.stream().mapToLong(RoutePopularityRowDTO::getTotalBookings).sum();
        double totalRevenue = rows.stream().mapToDouble(RoutePopularityRowDTO::getTotalRevenue).sum();
        String topRoute = rows.stream()
                .max(Comparator.comparingLong(RoutePopularityRowDTO::getTotalBookings))
                .map(RoutePopularityRowDTO::getRouteLabel)
                .orElse("-");

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Total Bookings", String.valueOf(totalBookings), "Across routes"),
                new KpiDTO("Total Revenue", MONEY_FORMAT.format(totalRevenue), "Paid revenue"),
                new KpiDTO("Top Route", topRoute, "Most bookings")
        );

        List<ChartPointDTO> chartPoints = rows.stream()
                .sorted(Comparator.comparingLong(RoutePopularityRowDTO::getTotalBookings).reversed())
                .limit(10)
                .map(row -> new ChartPointDTO(row.getRouteLabel(), row.getTotalBookings()))
                .collect(Collectors.toList());

        return buildResponse(kpis, chartPoints, rows);
    }

    @Override
    public ReportResponseDTO getCustomerActivityReport(ReportFilterDTO filter) throws ValidationException {
        ReportFilterDTO normalized = normalizeFilter(filter);
        ensureAdminActor(normalized.getRequestedByUserId());

        List<CustomerActivityRowDTO> rows = reportDAO.fetchCustomerActivity(normalized);
        long customerCount = rows.size();
        double totalSpent = rows.stream().mapToDouble(CustomerActivityRowDTO::getTotalSpent).sum();
        String topCustomer = rows.stream()
                .max(Comparator.comparingDouble(CustomerActivityRowDTO::getTotalSpent))
                .map(row -> safeText(row.getName()))
                .orElse("-");

        List<KpiDTO> kpis = List.of(
                new KpiDTO("Customers", String.valueOf(customerCount), "Active in range"),
                new KpiDTO("Total Spent", MONEY_FORMAT.format(totalSpent), "Paid revenue"),
                new KpiDTO("Top Customer", topCustomer, "Highest spend")
        );

        List<ChartPointDTO> chartPoints = rows.stream()
                .sorted(Comparator.comparingDouble(CustomerActivityRowDTO::getTotalSpent).reversed())
                .limit(10)
                .map(row -> new ChartPointDTO(safeText(row.getName()), row.getTotalSpent()))
                .collect(Collectors.toList());

        return buildResponse(kpis, chartPoints, rows);
    }

    private ReportResponseDTO buildResponse(List<KpiDTO> kpis, List<ChartPointDTO> chart, List<?> rows) {
        ReportResponseDTO response = new ReportResponseDTO();
        response.setKpis(kpis);
        response.setChartData(chart);
        response.setTableRows(rows);
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    private ReportFilterDTO normalizeFilter(ReportFilterDTO filter) throws ValidationException {
        if (filter == null) {
            throw new ValidationException("INVALID_REQUEST");
        }
        LocalDate fromDate = filter.getFromDate();
        LocalDate toDate = filter.getToDate();
        if (fromDate == null || toDate == null) {
            throw new ValidationException("DATE_RANGE_REQUIRED");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ValidationException("INVALID_DATE_RANGE");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate);
        if (days > MAX_RANGE_DAYS) {
            throw new ValidationException("DATE_RANGE_TOO_LARGE");
        }

        ReportFilterDTO normalized = new ReportFilterDTO();
        normalized.setFromDate(fromDate);
        normalized.setToDate(toDate);
        normalized.setRouteId(safeId(filter.getRouteId()));
        normalized.setBusId(safeId(filter.getBusId()));
        normalized.setTripId(safeId(filter.getTripId()));
        normalized.setBookingStatus(normalizeBookingStatus(filter.getBookingStatus()));
        normalized.setPaymentStatus(normalizePaymentStatus(filter.getPaymentStatus()));
        normalized.setRequestedByUserId(filter.getRequestedByUserId());
        return normalized;
    }

    private Long safeId(Long value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private String normalizeBookingStatus(String value) throws ValidationException {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return BookingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("INVALID_BOOKING_STATUS");
        }
    }

    private String normalizePaymentStatus(String value) throws ValidationException {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("INVALID_PAYMENT_STATUS");
        }
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

    private boolean equalsIgnoreCase(String value, String expected) {
        if (value == null || expected == null) {
            return false;
        }
        return value.equalsIgnoreCase(expected);
    }

    private String percent(double ratio) {
        return String.format(Locale.ROOT, "%.1f%%", ratio * 100.0);
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String routeLabel(TripPerformanceRowDTO row) {
        if (row == null) {
            return "-";
        }
        String label = safeText(row.getRouteLabel());
        if (row.getTripId() == null) {
            return label;
        }
        return label + " (#" + row.getTripId() + ")";
    }

    private String safeUpper(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
