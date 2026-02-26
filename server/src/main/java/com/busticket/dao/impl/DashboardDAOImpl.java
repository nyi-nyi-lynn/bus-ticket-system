package com.busticket.dao.impl;

import com.busticket.dao.DashboardDAO;
import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAOImpl implements DashboardDAO {
    private final Connection connection;

    public DashboardDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public double fetchRevenueToday() {
        if (connection == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return fetchRevenueBetween(startOfDay(today), startOfDay(today.plusDays(1)));
    }

    @Override
    public double fetchRevenueThisMonth() {
        if (connection == null) {
            return 0;
        }
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.withDayOfMonth(1);
        LocalDate firstOfNextMonth = firstDay.plusMonths(1);
        return fetchRevenueBetween(startOfDay(firstDay), startOfDay(firstOfNextMonth));
    }

    @Override
    public long fetchBookingsToday() {
        if (connection == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        String sql = """
                SELECT COUNT(*) AS booking_count
                FROM bookings
                WHERE booking_date >= ? AND booking_date < ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(today));
            ps.setTimestamp(2, startOfDay(today.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("booking_count");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public long fetchActiveTrips() {
        if (connection == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) AS trip_count FROM trips WHERE status = 'OPEN'";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("trip_count");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public long fetchActiveBuses() {
        if (connection == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) AS bus_count FROM buses WHERE is_active = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("bus_count");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public long fetchInactiveBuses() {
        if (connection == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) AS bus_count FROM buses WHERE is_active = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("bus_count");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public long fetchTotalUsers() {
        if (connection == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) AS user_count FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("user_count");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<RevenueRowDTO> fetchRevenueTrend(LocalDate fromDate, LocalDate toDate) {
        List<RevenueRowDTO> rows = new ArrayList<>();
        if (connection == null || fromDate == null || toDate == null) {
            return rows;
        }
        String sql = """
                SELECT DATE(p.paid_at) AS report_date,
                       COUNT(DISTINCT p.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM payments p
                WHERE p.payment_status = 'PAID'
                  AND p.paid_at >= ? AND p.paid_at < ?
                GROUP BY DATE(p.paid_at)
                ORDER BY DATE(p.paid_at)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(fromDate));
            ps.setTimestamp(2, startOfDay(toDate.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevenueRowDTO row = new RevenueRowDTO();
                    Date date = rs.getDate("report_date");
                    row.setDate(date == null ? null : date.toLocalDate());
                    row.setTotalBookings(rs.getLong("booking_count"));
                    row.setTotalRevenue(rs.getDouble("total_revenue"));
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    @Override
    public List<BookingRowDTO> fetchBookingStatusSummary(LocalDate fromDate, LocalDate toDate) {
        List<BookingRowDTO> rows = new ArrayList<>();
        if (connection == null || fromDate == null || toDate == null) {
            return rows;
        }
        String sql = """
                SELECT b.status AS booking_status,
                       COUNT(*) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM bookings b
                LEFT JOIN payments p
                       ON p.booking_id = b.booking_id
                      AND p.payment_status = 'PAID'
                WHERE b.booking_date >= ? AND b.booking_date < ?
                GROUP BY b.status
                ORDER BY booking_count DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(fromDate));
            ps.setTimestamp(2, startOfDay(toDate.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingRowDTO row = new BookingRowDTO();
                    row.setBookingStatus(rs.getString("booking_status"));
                    row.setTotalBookings(rs.getLong("booking_count"));
                    row.setTotalRevenue(rs.getDouble("total_revenue"));
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    @Override
    public List<RoutePopularityRowDTO> fetchTopRoutes(LocalDate fromDate, LocalDate toDate, int limit) {
        List<RoutePopularityRowDTO> rows = new ArrayList<>();
        if (connection == null || fromDate == null || toDate == null) {
            return rows;
        }
        String sql = """
                SELECT r.route_id,
                       r.origin_city,
                       r.destination_city,
                       COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM routes r
                JOIN trips t ON t.route_id = r.route_id
                JOIN bookings b ON b.trip_id = t.trip_id AND b.status = 'CONFIRMED'
                JOIN payments p ON p.booking_id = b.booking_id
                               AND p.payment_status = 'PAID'
                WHERE p.paid_at >= ? AND p.paid_at < ?
                GROUP BY r.route_id, r.origin_city, r.destination_city
                ORDER BY total_revenue DESC, booking_count DESC
                LIMIT ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(fromDate));
            ps.setTimestamp(2, startOfDay(toDate.plusDays(1)));
            ps.setInt(3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoutePopularityRowDTO row = new RoutePopularityRowDTO();
                    row.setRouteId(rs.getLong("route_id"));
                    row.setRouteLabel(formatRoute(rs.getString("origin_city"), rs.getString("destination_city")));
                    row.setTotalBookings(rs.getLong("booking_count"));
                    row.setTotalRevenue(rs.getDouble("total_revenue"));
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    @Override
    public List<BookingDTO> fetchRecentBookings(int limit) {
        List<BookingDTO> rows = new ArrayList<>();
        if (connection == null) {
            return rows;
        }
        String sql = """
                SELECT b.booking_id,
                       b.booking_date,
                       b.status,
                       b.total_price,
                       u.name AS passenger_name,
                       r.origin_city,
                       r.destination_city,
                       t.travel_date,
                       t.departure_time,
                       t.arrival_time
                FROM bookings b
                JOIN users u ON u.user_id = b.user_id
                JOIN trips t ON t.trip_id = b.trip_id
                JOIN routes r ON r.route_id = t.route_id
                ORDER BY b.booking_date DESC
                LIMIT ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingDTO row = new BookingDTO();
                    row.setBookingId(rs.getLong("booking_id"));
                    Timestamp bookingDate = rs.getTimestamp("booking_date");
                    row.setBookingDate(bookingDate == null ? null : bookingDate.toLocalDateTime());
                    row.setStatus(rs.getString("status"));
                    row.setTotalPrice(rs.getDouble("total_price"));
                    row.setPassengerName(rs.getString("passenger_name"));
                    row.setOriginCity(rs.getString("origin_city"));
                    row.setDestinationCity(rs.getString("destination_city"));
                    Date travelDate = rs.getDate("travel_date");
                    row.setTravelDate(travelDate == null ? null : travelDate.toLocalDate());
                    Time departure = rs.getTime("departure_time");
                    row.setDepartureTime(departure == null ? null : departure.toLocalTime());
                    Time arrival = rs.getTime("arrival_time");
                    row.setArrivalTime(arrival == null ? null : arrival.toLocalTime());
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    @Override
    public List<TripPerformanceRowDTO> fetchLowOccupancyTrips(LocalDate fromDate, LocalDate toDate, double threshold, int limit) {
        List<TripPerformanceRowDTO> rows = new ArrayList<>();
        if (connection == null || fromDate == null || toDate == null) {
            return rows;
        }
        String sql = """
                SELECT t.trip_id,
                       r.origin_city,
                       r.destination_city,
                       bu.bus_number,
                       bu.total_seats,
                       t.travel_date,
                       IFNULL(sold.sold_seats, 0) AS sold_seats
                FROM trips t
                JOIN routes r ON r.route_id = t.route_id
                JOIN buses bu ON bu.bus_id = t.bus_id
                LEFT JOIN (
                    SELECT b.trip_id, COUNT(bs.seat_id) AS sold_seats
                    FROM bookings b
                    JOIN booking_seat bs ON bs.booking_id = b.booking_id
                    WHERE b.status = 'CONFIRMED'
                    GROUP BY b.trip_id
                ) sold ON sold.trip_id = t.trip_id
                WHERE t.status = 'OPEN'
                  AND t.travel_date BETWEEN ? AND ?
                  AND (IFNULL(sold.sold_seats, 0) * 1.0 / bu.total_seats) < ?
                ORDER BY t.travel_date ASC, t.trip_id ASC
                LIMIT ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            ps.setDouble(3, threshold);
            ps.setInt(4, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TripPerformanceRowDTO row = new TripPerformanceRowDTO();
                    row.setTripId(rs.getLong("trip_id"));
                    row.setRouteLabel(formatRoute(rs.getString("origin_city"), rs.getString("destination_city")));
                    row.setBusNumber(rs.getString("bus_number"));
                    Date travelDate = rs.getDate("travel_date");
                    row.setTravelDate(travelDate == null ? null : travelDate.toLocalDate());
                    int totalSeats = rs.getInt("total_seats");
                    row.setTotalSeats(totalSeats);
                    long soldSeats = rs.getLong("sold_seats");
                    row.setSoldSeats(soldSeats);
                    row.setOccupancyRate(totalSeats == 0 ? 0 : (double) soldSeats / totalSeats);
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return rows;
    }

    private double fetchRevenueBetween(Timestamp start, Timestamp end) {
        String sql = """
                SELECT IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM payments p
                WHERE p.payment_status = 'PAID'
                  AND p.paid_at >= ? AND p.paid_at < ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, start);
            ps.setTimestamp(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_revenue");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private Timestamp startOfDay(LocalDate date) {
        LocalDateTime start = date == null ? LocalDateTime.now() : date.atStartOfDay();
        return Timestamp.valueOf(start);
    }

    private String formatRoute(String origin, String destination) {
        String left = origin == null || origin.isBlank() ? "-" : origin.trim();
        String right = destination == null || destination.isBlank() ? "-" : destination.trim();
        return left + " -> " + right;
    }
}
