package com.busticket.dao.impl;

import com.busticket.dao.ReportDAO;
import com.busticket.dto.BookingRowDTO;
import com.busticket.dto.CustomerActivityRowDTO;
import com.busticket.dto.ReportFilterDTO;
import com.busticket.dto.RevenueRowDTO;
import com.busticket.dto.RoutePopularityRowDTO;
import com.busticket.dto.TripPerformanceRowDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAOImpl implements ReportDAO {
    private final Connection connection;

    public ReportDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<RevenueRowDTO> fetchRevenueSummary(ReportFilterDTO filter) {
        List<RevenueRowDTO> rows = new ArrayList<>();
        if (connection == null || filter == null) {
            return rows;
        }

        StringBuilder sql = new StringBuilder("""
                SELECT DATE(p.paid_at) AS report_date,
                       COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM bookings b
                JOIN payments p ON p.booking_id = b.booking_id
                JOIN trips t ON t.trip_id = b.trip_id
                JOIN routes r ON r.route_id = t.route_id
                JOIN buses bu ON bu.bus_id = t.bus_id
                WHERE DATE(p.paid_at) BETWEEN ? AND ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(filter.getFromDate()));
        params.add(Date.valueOf(filter.getToDate()));

        String bookingStatus = filter.getBookingStatus();
        if (bookingStatus != null) {
            sql.append(" AND b.status = ?");
            params.add(bookingStatus);
        } else {
            sql.append(" AND b.status = 'CONFIRMED'");
        }

        String paymentStatus = filter.getPaymentStatus();
        if (paymentStatus != null) {
            sql.append(" AND p.payment_status = ?");
            params.add(paymentStatus);
        } else {
            sql.append(" AND p.payment_status = 'PAID'");
        }

        if (filter.getRouteId() != null) {
            sql.append(" AND r.route_id = ?");
            params.add(filter.getRouteId());
        }
        if (filter.getBusId() != null) {
            sql.append(" AND t.bus_id = ?");
            params.add(filter.getBusId());
        }
        if (filter.getTripId() != null) {
            sql.append(" AND t.trip_id = ?");
            params.add(filter.getTripId());
        }

        sql.append(" GROUP BY DATE(p.paid_at) ORDER BY DATE(p.paid_at)");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParams(ps, params);
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
    public List<BookingRowDTO> fetchBookingSummary(ReportFilterDTO filter) {
        List<BookingRowDTO> rows = new ArrayList<>();
        if (connection == null || filter == null) {
            return rows;
        }

        String paymentStatus = filter.getPaymentStatus();
        String paymentJoin = paymentStatus == null
                ? "LEFT JOIN payments p ON p.booking_id = b.booking_id"
                : "JOIN payments p ON p.booking_id = b.booking_id AND p.payment_status = ?";

        StringBuilder sql = new StringBuilder("""
                SELECT b.status AS booking_status,
                       COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM bookings b
                JOIN trips t ON t.trip_id = b.trip_id
                JOIN routes r ON r.route_id = t.route_id
                JOIN buses bu ON bu.bus_id = t.bus_id
                """);
        sql.append(" ").append(paymentJoin).append("\n");
        sql.append(" WHERE DATE(b.created_at) BETWEEN ? AND ?");

        List<Object> params = new ArrayList<>();
        if (paymentStatus != null) {
            params.add(paymentStatus);
        }
        params.add(Date.valueOf(filter.getFromDate()));
        params.add(Date.valueOf(filter.getToDate()));

        if (filter.getRouteId() != null) {
            sql.append(" AND r.route_id = ?");
            params.add(filter.getRouteId());
        }
        if (filter.getBusId() != null) {
            sql.append(" AND t.bus_id = ?");
            params.add(filter.getBusId());
        }
        if (filter.getTripId() != null) {
            sql.append(" AND t.trip_id = ?");
            params.add(filter.getTripId());
        }

        if (filter.getBookingStatus() != null) {
            sql.append(" AND b.status = ?");
            params.add(filter.getBookingStatus());
        }

        sql.append(" GROUP BY b.status ORDER BY booking_count DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParams(ps, params);
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
    public List<TripPerformanceRowDTO> fetchTripPerformance(ReportFilterDTO filter) {
        List<TripPerformanceRowDTO> rows = new ArrayList<>();
        if (connection == null || filter == null) {
            return rows;
        }

        String bookingStatus = filter.getBookingStatus();
        String paymentStatus = filter.getPaymentStatus();

        StringBuilder soldSql = new StringBuilder("""
                SELECT b.trip_id, COUNT(bs.seat_id) AS sold_seats
                FROM bookings b
                JOIN booking_seat bs ON bs.booking_id = b.booking_id
                """);
        List<Object> soldParams = new ArrayList<>();
        if (paymentStatus != null) {
            soldSql.append(" JOIN payments p ON p.booking_id = b.booking_id AND p.payment_status = ?");
            soldParams.add(paymentStatus);
        }
        soldSql.append(" WHERE 1=1");
        if (bookingStatus != null) {
            soldSql.append(" AND b.status = ?");
            soldParams.add(bookingStatus);
        } else {
            soldSql.append(" AND b.status = 'CONFIRMED'");
        }
        soldSql.append(" GROUP BY b.trip_id");

        StringBuilder revenueSql = new StringBuilder("""
                SELECT b.trip_id, IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM bookings b
                JOIN payments p ON p.booking_id = b.booking_id
                WHERE 1=1
                """);
        List<Object> revenueParams = new ArrayList<>();
        if (paymentStatus != null) {
            revenueSql.append(" AND p.payment_status = ?");
            revenueParams.add(paymentStatus);
        } else {
            revenueSql.append(" AND p.payment_status = 'PAID'");
        }
        if (bookingStatus != null) {
            revenueSql.append(" AND b.status = ?");
            revenueParams.add(bookingStatus);
        } else {
            revenueSql.append(" AND b.status = 'CONFIRMED'");
        }
        revenueSql.append(" GROUP BY b.trip_id");

        StringBuilder sql = new StringBuilder("""
                SELECT t.trip_id,
                       r.origin_city,
                       r.destination_city,
                       bu.bus_number,
                       bu.total_seats,
                       t.travel_date,
                       IFNULL(sold.sold_seats, 0) AS sold_seats,
                       IFNULL(rev.total_revenue, 0) AS total_revenue
                FROM trips t
                JOIN routes r ON r.route_id = t.route_id
                JOIN buses bu ON bu.bus_id = t.bus_id
                LEFT JOIN (
                """);
        sql.append(soldSql).append("\n) sold ON sold.trip_id = t.trip_id\n");
        sql.append("LEFT JOIN (\n");
        sql.append(revenueSql).append("\n) rev ON rev.trip_id = t.trip_id\n");
        sql.append("WHERE t.travel_date BETWEEN ? AND ?");

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(filter.getFromDate()));
        params.add(Date.valueOf(filter.getToDate()));

        if (filter.getRouteId() != null) {
            sql.append(" AND r.route_id = ?");
            params.add(filter.getRouteId());
        }
        if (filter.getBusId() != null) {
            sql.append(" AND t.bus_id = ?");
            params.add(filter.getBusId());
        }
        if (filter.getTripId() != null) {
            sql.append(" AND t.trip_id = ?");
            params.add(filter.getTripId());
        }

        sql.append(" ORDER BY t.travel_date DESC, t.trip_id DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            index = bindParams(ps, soldParams, index);
            index = bindParams(ps, revenueParams, index);
            bindParams(ps, params, index);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TripPerformanceRowDTO row = new TripPerformanceRowDTO();
                    row.setTripId(rs.getLong("trip_id"));
                    String origin = rs.getString("origin_city");
                    String destination = rs.getString("destination_city");
                    row.setRouteLabel(formatRoute(origin, destination));
                    row.setBusNumber(rs.getString("bus_number"));
                    Date travelDate = rs.getDate("travel_date");
                    row.setTravelDate(travelDate == null ? null : travelDate.toLocalDate());
                    int totalSeats = rs.getInt("total_seats");
                    row.setTotalSeats(totalSeats);
                    long soldSeats = rs.getLong("sold_seats");
                    row.setSoldSeats(soldSeats);
                    row.setOccupancyRate(totalSeats == 0 ? 0 : (double) soldSeats / totalSeats);
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
    public List<RoutePopularityRowDTO> fetchRoutePopularity(ReportFilterDTO filter) {
        List<RoutePopularityRowDTO> rows = new ArrayList<>();
        if (connection == null || filter == null) {
            return rows;
        }

        String paymentStatus = filter.getPaymentStatus();
        String paymentJoin = paymentStatus == null
                ? "LEFT JOIN payments p ON p.booking_id = b.booking_id"
                : "JOIN payments p ON p.booking_id = b.booking_id AND p.payment_status = ?";

        StringBuilder sql = new StringBuilder("""
                SELECT r.route_id,
                       r.origin_city,
                       r.destination_city,
                       COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM routes r
                JOIN trips t ON t.route_id = r.route_id
                LEFT JOIN bookings b ON b.trip_id = t.trip_id
                """);
        sql.append(" ").append(paymentJoin).append("\n");
        sql.append(" WHERE t.travel_date BETWEEN ? AND ?");

        List<Object> params = new ArrayList<>();
        if (paymentStatus != null) {
            params.add(paymentStatus);
        }
        params.add(Date.valueOf(filter.getFromDate()));
        params.add(Date.valueOf(filter.getToDate()));

        if (filter.getRouteId() != null) {
            sql.append(" AND r.route_id = ?");
            params.add(filter.getRouteId());
        }
        if (filter.getBusId() != null) {
            sql.append(" AND t.bus_id = ?");
            params.add(filter.getBusId());
        }
        if (filter.getTripId() != null) {
            sql.append(" AND t.trip_id = ?");
            params.add(filter.getTripId());
        }
        if (filter.getBookingStatus() != null) {
            sql.append(" AND b.status = ?");
            params.add(filter.getBookingStatus());
        }

        sql.append(" GROUP BY r.route_id, r.origin_city, r.destination_city");
        sql.append(" ORDER BY booking_count DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParams(ps, params);
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
    public List<CustomerActivityRowDTO> fetchCustomerActivity(ReportFilterDTO filter) {
        List<CustomerActivityRowDTO> rows = new ArrayList<>();
        if (connection == null || filter == null) {
            return rows;
        }

        String paymentStatus = filter.getPaymentStatus();
        String paymentJoin = paymentStatus == null
                ? "LEFT JOIN payments p ON p.booking_id = b.booking_id"
                : "JOIN payments p ON p.booking_id = b.booking_id AND p.payment_status = ?";

        StringBuilder sql = new StringBuilder("""
                SELECT u.user_id, u.name, u.email,
                       COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_spent
                FROM users u
                JOIN bookings b ON b.user_id = u.user_id
                JOIN trips t ON t.trip_id = b.trip_id
                """);
        sql.append(" ").append(paymentJoin).append("\n");
        sql.append(" WHERE DATE(b.created_at) BETWEEN ? AND ?");

        List<Object> params = new ArrayList<>();
        if (paymentStatus != null) {
            params.add(paymentStatus);
        }
        params.add(Date.valueOf(filter.getFromDate()));
        params.add(Date.valueOf(filter.getToDate()));

        if (filter.getRouteId() != null) {
            sql.append(" AND t.route_id = ?");
            params.add(filter.getRouteId());
        }
        if (filter.getBusId() != null) {
            sql.append(" AND t.bus_id = ?");
            params.add(filter.getBusId());
        }
        if (filter.getTripId() != null) {
            sql.append(" AND t.trip_id = ?");
            params.add(filter.getTripId());
        }
        if (filter.getBookingStatus() != null) {
            sql.append(" AND b.status = ?");
            params.add(filter.getBookingStatus());
        }

        sql.append(" GROUP BY u.user_id, u.name, u.email");
        sql.append(" ORDER BY total_spent DESC");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerActivityRowDTO row = new CustomerActivityRowDTO();
                    row.setUserId(rs.getLong("user_id"));
                    row.setName(rs.getString("name"));
                    row.setEmail(rs.getString("email"));
                    row.setTotalBookings(rs.getLong("booking_count"));
                    row.setTotalSpent(rs.getDouble("total_spent"));
                    rows.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return rows;
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        bindParams(ps, params, 1);
    }

    private int bindParams(PreparedStatement ps, List<Object> params, int startIndex) throws SQLException {
        int index = startIndex;
        for (Object value : params) {
            if (value instanceof Long) {
                ps.setLong(index++, (Long) value);
            } else if (value instanceof Integer) {
                ps.setInt(index++, (Integer) value);
            } else if (value instanceof Double) {
                ps.setDouble(index++, (Double) value);
            } else if (value instanceof Date) {
                ps.setDate(index++, (Date) value);
            } else if (value instanceof String) {
                ps.setString(index++, (String) value);
            } else {
                ps.setObject(index++, value);
            }
        }
        return index;
    }

    private String formatRoute(String origin, String destination) {
        String left = origin == null || origin.isBlank() ? "-" : origin.trim();
        String right = destination == null || destination.isBlank() ? "-" : destination.trim();
        return left + " -> " + right;
    }
}
