package com.busticket.dao.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class BookingDAOImpl implements BookingDAO {

    private final Connection connection ;

    public BookingDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long createBooking(Booking booking) {
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
        return null;
    }

    @Override
    public Long save(Booking booking) {
        // ADDED
        return createBooking(booking);
    }

    @Override
    public void insertBookingSeats(Long bookingId, List<Long> seatIds) {
        String sql = "INSERT INTO booking_seat(booking_id, seat_id) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Long seatId : seatIds) {
                ps.setLong(1, bookingId);
                ps.setLong(2, seatId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        // ADDED
        if (userId == null) {
            return List.of();
        }

        String sql = """
            SELECT b.booking_id, b.user_id, b.trip_id, b.booking_date, b.total_price, b.ticket_code, b.status,
                   GROUP_CONCAT(s.seat_number ORDER BY s.seat_number SEPARATOR ',') AS seat_numbers
            FROM bookings b
            LEFT JOIN booking_seat bs ON bs.booking_id = b.booking_id
            LEFT JOIN seats s ON s.seat_id = bs.seat_id
            WHERE b.user_id = ?
            GROUP BY b.booking_id, b.user_id, b.trip_id, b.booking_date, b.total_price, b.ticket_code, b.status
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

    @Override
    public List<Long> findBookedSeats(Long tripId) {
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
    public List<Long> findBookedSeatIdsByTrip(Long tripId) {
        // ADDED
        return findBookedSeats(tripId);
    }

    @Override
    public List<String> findAvailableSeatNumbers(Long tripId) {
        // ADDED
        if (tripId == null) {
            return new ArrayList<>();
        }

        String sql = """
            SELECT s.seat_number
            FROM seats s
            JOIN trips t ON t.bus_id = s.bus_id
            WHERE t.trip_id = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM booking_seat bs
                  JOIN bookings b ON b.booking_id = bs.booking_id
                  WHERE b.trip_id = t.trip_id
                    AND bs.seat_id = s.seat_id
                    AND b.status IN ('PENDING', 'CONFIRMED')
              )
            ORDER BY s.seat_number
        """;
        List<String> seatNumbers = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seatNumbers.add(rs.getString("seat_number"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return seatNumbers;
    }

    @Override
    public List<Long> findByTripAndSeatIds(Long tripId, List<Long> seatIds) {
        // ADDED
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
    public boolean updateStatus(Long bookingId, BookingStatus status) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long userId) {
        // MODIFIED
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
              AND booking_date < (NOW() - INTERVAL ? MINUTE)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
