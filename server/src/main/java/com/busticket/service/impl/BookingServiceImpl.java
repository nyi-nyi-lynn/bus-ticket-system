package com.busticket.service.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dao.impl.BookingDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;
import com.busticket.service.BookingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {
    private final BookingDAO bookingDAO;
    private final Connection connection;

    public BookingServiceImpl(){
        connection = DatabaseConnection.getConnection();
        bookingDAO = new BookingDAOImpl(connection);
    }

    @Override
    public BookingDTO createBooking(BookingDTO dto) {
        // Validate required fields and requested seats.
        if (dto == null || dto.getUserId() == null || dto.getTripId() == null) {
            return null;
        }
        if (dto.getSeatNumbers() == null || dto.getSeatNumbers().isEmpty()) {
            return null;
        }

        List<Long> seatIds = findSeatIds(dto.getTripId(), dto.getSeatNumbers());
        if (seatIds.size() != dto.getSeatNumbers().size()) {
            return null;
        }

        // Prevent double-booking by rejecting already reserved seats.
        List<Long> booked = bookingDAO.findBookedSeats(dto.getTripId());
        Set<Long> bookedSet = new HashSet<>(booked);
        for (Long seatId : seatIds) {
            if (bookedSet.contains(seatId)) {
                return null;
            }
        }

        Booking booking = new Booking();
        booking.setUserId(dto.getUserId());
        booking.setTripId(dto.getTripId());
        booking.setTotalPrice(dto.getTotalPrice() == null ? 0.0 : dto.getTotalPrice());
        booking.setTicketCode(generateTicketCode());
        booking.setStatus(BookingStatus.PENDING);

        Long bookingId = bookingDAO.createBooking(booking);
        if (bookingId == null) {
            return null;
        }

        bookingDAO.insertBookingSeats(bookingId, seatIds);

        BookingDTO result = new BookingDTO();
        result.setBookingId(bookingId);
        result.setUserId(dto.getUserId());
        result.setTripId(dto.getTripId());
        result.setSeatNumbers(dto.getSeatNumbers());
        result.setTotalPrice(booking.getTotalPrice());
        result.setTicketCode(booking.getTicketCode());
        result.setStatus(booking.getStatus().name());
        return result;
    }

    @Override
    public boolean confirmBooking(Long bookingId) {
        // Validate required identifier.
        if (bookingId == null) {
            return false;
        }
        return bookingDAO.updateStatus(bookingId, BookingStatus.CONFIRMED);
    }

    @Override
    public List<Long> getBookedSeatIds(Long tripId) {
        // Validate required identifier.
        if (tripId == null) {
            return List.of();
        }
        return bookingDAO.findBookedSeats(tripId);
    }

    private List<Long> findSeatIds(Long tripId, List<String> seatNumbers) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < seatNumbers.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }

        String sql = """
            SELECT s.seat_id
            FROM seats s
            JOIN trips t ON t.bus_id = s.bus_id
            WHERE t.trip_id = ? AND s.seat_number IN (%s)
        """.formatted(placeholders);

        List<Long> seatIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            for (int i = 0; i < seatNumbers.size(); i++) {
                ps.setString(i + 2, seatNumbers.get(i));
            }
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

    private String generateTicketCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "TCK" + raw.substring(0, 12);
    }
}
