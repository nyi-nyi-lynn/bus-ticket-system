package com.busticket.dao.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dto.RecentBookingDTO;
import com.busticket.dto.UpcomingTripDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.PaymentStatus;
import com.busticket.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class BookingDAOImpl implements BookingDAO {

    private final Connection connection ;

    public BookingDAOImpl(Connection connection) {
        this.connection = connection;
    }

    private Long createBooking(Booking booking) {
        String sqlWithPaymentStatus = """
            INSERT INTO bookings(user_id, trip_id, total_price, ticket_code, status, payment_status)
            VALUES(?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sqlWithPaymentStatus, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, booking.getUserId());
            ps.setLong(2, booking.getTripId());
            ps.setDouble(3, booking.getTotalPrice());
            ps.setString(4, booking.getTicketCode());
            ps.setString(5, booking.getStatus().name());
            ps.setString(6, "PENDING");

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return null;
        } catch (SQLException withPaymentStatusEx) {
            String sql = """
                INSERT INTO bookings(user_id, trip_id, total_price, ticket_code, status)
                VALUES(?,?,?,?,?)
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, booking.getUserId());
                ps.setLong(2, booking.getTripId());
                ps.setDouble(3, booking.getTotalPrice());
                ps.setString(4, booking.getTicketCode());
                ps.setString(5, booking.getStatus().name());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    return null;
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public Long save(Booking booking) {
        return createBooking(booking);
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }

        String sql = """
            SELECT b.booking_id, b.user_id, b.trip_id, b.booking_date, b.total_price, b.ticket_code, b.status,
                   COALESCE(p.payment_status, 'PENDING') AS payment_status,
                   GROUP_CONCAT(s.seat_number ORDER BY s.seat_number SEPARATOR ',') AS seat_numbers
            FROM bookings b
            LEFT JOIN payments p ON p.booking_id = b.booking_id
            LEFT JOIN booking_seat bs ON bs.booking_id = b.booking_id
            LEFT JOIN seats s ON s.seat_id = bs.seat_id
            WHERE b.user_id = ?
            GROUP BY b.booking_id, b.user_id, b.trip_id, b.booking_date, b.total_price, b.ticket_code, b.status, p.payment_status
            ORDER BY b.booking_date DESC
        """;

        List<Booking> bookings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setUserId(rs.getLong("user_id"));
                    booking.setTripId(rs.getLong("trip_id"));
                    Timestamp bookingTs = rs.getTimestamp("booking_date");
                    if (bookingTs != null) {
                        booking.setBookingDate(bookingTs.toLocalDateTime());
                    }
                    booking.setTotalPrice(rs.getDouble("total_price"));
                    booking.setTicketCode(rs.getString("ticket_code"));
                    booking.setStatus(BookingStatus.valueOf(rs.getString("status")));
                    booking.setPaymentStatus(parsePaymentStatus(rs.getString("payment_status")));
                    String seats = rs.getString("seat_numbers");
                    if (seats == null || seats.isBlank()) {
                        booking.setSeatNumbers(List.of());
                    } else {
                        booking.setSeatNumbers(Arrays.stream(seats.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList());
                    }
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    private PaymentStatus parsePaymentStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return PaymentStatus.PENDING;
        }
        try {
            return PaymentStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PaymentStatus.PENDING;
        }
    }

    @Override
    public List<Long> findBookedSeatIdsByTrip(Long tripId) {
        if (tripId == null) {
            return List.of();
        }
        String sql = """
            SELECT bs.seat_id
            FROM booking_seat bs
            JOIN bookings b ON b.booking_id = bs.booking_id
            WHERE b.trip_id = ? AND b.status <> 'CANCELLED'
            FOR UPDATE
        """;
        List<Long> seatIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seatIds.add(rs.getLong("seat_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return seatIds;
    }

    @Override
    public List<Long> findByTripAndSeatIds(Long tripId, List<Long> seatIds) {
        if (tripId == null || seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }

        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < seatIds.size(); i++) {
            placeholders.add("?");
        }

        String sql = """
            SELECT bs.seat_id
            FROM booking_seat bs
            JOIN bookings b ON b.booking_id = bs.booking_id
            WHERE b.trip_id = ?
              AND b.status IN ('PENDING', 'CONFIRMED')
              AND bs.seat_id IN (%s)
            FOR UPDATE
        """.formatted(placeholders);

        List<Long> bookedIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            for (int i = 0; i < seatIds.size(); i++) {
                ps.setLong(i + 2, seatIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookedIds.add(rs.getLong("seat_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return bookedIds;
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long userId) {
        if (bookingId == null || userId == null) {
            return false;
        }
        String sql = """
            UPDATE bookings
            SET status = 'CANCELLED'
            WHERE booking_id = ?
              AND user_id = ?
              AND status IN ('PENDING', 'CONFIRMED')
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int cancelExpiredPending(int minutes) {
        String sql = """
            UPDATE bookings
            SET status = 'CANCELLED'
            WHERE status = 'PENDING'
              AND created_at < (NOW() - INTERVAL ? MINUTE)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Map<String, Long> countBookingsByStatus(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        String sql = """
            SELECT status, COUNT(*) AS total
            FROM bookings
            WHERE user_id = ?
            GROUP BY status
        """;
        Map<String, Long> counts = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("status"), rs.getLong("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    @Override
    public long countUpcomingTrips(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String sql = """
            SELECT COUNT(*)
            FROM bookings b
            JOIN trips t ON t.trip_id = b.trip_id
            WHERE b.user_id = ?
              AND b.status = 'CONFIRMED'
              AND TIMESTAMP(t.travel_date, t.departure_time) > NOW()
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public long countCompletedTrips(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String sql = """
            SELECT COUNT(*)
            FROM bookings b
            JOIN trips t ON t.trip_id = b.trip_id
            WHERE b.user_id = ?
              AND b.status = 'CONFIRMED'
              AND TIMESTAMP(t.travel_date, t.departure_time) <= NOW()
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    @Override
    public UpcomingTripDTO findNextUpcomingTrip(Long userId) {
        if (userId == null) {
            return null;
        }
        String sql = """
            SELECT b.booking_id, b.ticket_code,
                   r.origin_city, r.destination_city,
                   t.travel_date, t.departure_time,
                   bu.bus_number,
                   GROUP_CONCAT(s.seat_number ORDER BY s.seat_number SEPARATOR ',') AS seat_numbers
            FROM bookings b
            JOIN trips t ON t.trip_id = b.trip_id
            JOIN routes r ON r.route_id = t.route_id
            JOIN buses bu ON bu.bus_id = t.bus_id
            LEFT JOIN booking_seat bs ON bs.booking_id = b.booking_id
            LEFT JOIN seats s ON s.seat_id = bs.seat_id
            WHERE b.user_id = ?
              AND b.status = 'CONFIRMED'
              AND TIMESTAMP(t.travel_date, t.departure_time) > NOW()
            GROUP BY b.booking_id, b.ticket_code, r.origin_city, r.destination_city,
                     t.travel_date, t.departure_time, bu.bus_number
            ORDER BY t.travel_date ASC, t.departure_time ASC
            LIMIT 1
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UpcomingTripDTO dto = new UpcomingTripDTO();
                dto.setBookingId(rs.getLong("booking_id"));
                dto.setBookingCode(rs.getString("ticket_code"));
                dto.setOriginCity(rs.getString("origin_city"));
                dto.setDestinationCity(rs.getString("destination_city"));
                Date travelDate = rs.getDate("travel_date");
                if (travelDate != null) {
                    dto.setTravelDate(travelDate.toLocalDate());
                }
                Time departureTime = rs.getTime("departure_time");
                if (departureTime != null) {
                    dto.setDepartureTime(departureTime.toLocalTime());
                }
                dto.setBusNumber(rs.getString("bus_number"));
                String seats = rs.getString("seat_numbers");
                if (seats == null || seats.isBlank()) {
                    dto.setSeatNumbers(List.of());
                } else {
                    dto.setSeatNumbers(Arrays.stream(seats.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList());
                }
                return dto;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<RecentBookingDTO> findRecentBookings(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        String sql = """
            SELECT b.booking_id, b.ticket_code, b.total_price, b.status,
                   r.origin_city, r.destination_city,
                   t.travel_date, t.departure_time,
                   GROUP_CONCAT(s.seat_number ORDER BY s.seat_number SEPARATOR ',') AS seat_numbers
            FROM bookings b
            JOIN trips t ON t.trip_id = b.trip_id
            JOIN routes r ON r.route_id = t.route_id
            LEFT JOIN booking_seat bs ON bs.booking_id = b.booking_id
            LEFT JOIN seats s ON s.seat_id = bs.seat_id
            WHERE b.user_id = ?
            GROUP BY b.booking_id, b.ticket_code, b.total_price, b.status,
                     r.origin_city, r.destination_city, t.travel_date, t.departure_time
            ORDER BY b.booking_date DESC
            LIMIT ?
        """;
        List<RecentBookingDTO> bookings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecentBookingDTO dto = new RecentBookingDTO();
                    dto.setBookingId(rs.getLong("booking_id"));
                    dto.setBookingCode(rs.getString("ticket_code"));
                    dto.setTotalPrice(rs.getDouble("total_price"));
                    dto.setStatus(rs.getString("status"));
                    dto.setOriginCity(rs.getString("origin_city"));
                    dto.setDestinationCity(rs.getString("destination_city"));
                    Date travelDate = rs.getDate("travel_date");
                    if (travelDate != null) {
                        dto.setTravelDate(travelDate.toLocalDate());
                    }
                    Time departureTime = rs.getTime("departure_time");
                    if (departureTime != null) {
                        dto.setDepartureTime(departureTime.toLocalTime());
                    }
                    String seats = rs.getString("seat_numbers");
                    if (seats == null || seats.isBlank()) {
                        dto.setSeatNumbers(List.of());
                    } else {
                        dto.setSeatNumbers(Arrays.stream(seats.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList());
                    }
                    bookings.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }
}
