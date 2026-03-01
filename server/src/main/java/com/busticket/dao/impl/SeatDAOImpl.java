package com.busticket.dao.impl;

import com.busticket.dao.SeatDAO;
import com.busticket.dto.SeatDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeatDAOImpl implements SeatDAO {
    private final Connection connection;

    public SeatDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<String> findAvailableSeatNumbersByTrip(Long tripId) {
        if (tripId == null) {
            return List.of();
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
            throw new RuntimeException(e);
        }
        return seatNumbers;
    }

    @Override
    public List<SeatDTO> findByBusId(Long busId) {
        if (busId == null) {
            return List.of();
        }

        String sql = """
            SELECT seat_id, bus_id, seat_number
            FROM seats
            WHERE bus_id = ?
            ORDER BY seat_number
        """;
        List<SeatDTO> seats = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SeatDTO seat = new SeatDTO();
                    seat.setSeatId(rs.getLong("seat_id"));
                    seat.setBusId(rs.getLong("bus_id"));
                    seat.setSeatNumber(rs.getString("seat_number"));
                    seats.add(seat);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return seats;
    }
}
