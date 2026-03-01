package com.busticket.dao.impl;

import com.busticket.dao.BookingSeatDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class BookingSeatDAOImpl implements BookingSeatDAO {
    private final Connection connection;

    public BookingSeatDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void saveAll(Long bookingId, List<Long> seatIds) {
        if (bookingId == null || seatIds == null || seatIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO booking_seat(booking_id, seat_id) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Long seatId : seatIds) {
                ps.setLong(1, bookingId);
                ps.setLong(2, seatId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
