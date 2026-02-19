package com.busticket.dao.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
    public List<Long> findBookedSeats(Long tripId) {
        String sql = """
            SELECT bs.seat_id
            FROM booking_seat bs
            JOIN bookings b ON b.booking_id = bs.booking_id
            WHERE b.trip_id = ? AND b.status <> 'CANCELLED'
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
}
