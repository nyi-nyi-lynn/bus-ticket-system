package com.busticket.service.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dao.BookingSeatDAO; // ADDED
import com.busticket.dao.SeatDAO; // ADDED
import com.busticket.dao.impl.BookingDAOImpl;
import com.busticket.dao.impl.BookingSeatDAOImpl; // ADDED
import com.busticket.dao.impl.SeatDAOImpl; // ADDED
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO; // ADDED
import com.busticket.dto.BookingResponseDTO; // ADDED
import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;
import com.busticket.service.BookingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {
    private final BookingDAO bookingDAO;
    private final BookingSeatDAO bookingSeatDAO; // ADDED
    private final SeatDAO seatDAO; // ADDED
    private final Connection connection;

    public BookingServiceImpl(){
        connection = DatabaseConnection.getConnection();
        bookingDAO = new BookingDAOImpl(connection);
        bookingSeatDAO = new BookingSeatDAOImpl(connection); // ADDED
        seatDAO = new SeatDAOImpl(connection); // ADDED
    }

    @Override
    public BookingDTO createBooking(BookingDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getTripId() == null || dto.getSeatNumbers() == null || dto.getSeatNumbers().isEmpty()) {
            return null;
        }

        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(dto.getUserId());
        request.setTripId(dto.getTripId());
        request.setSeatNumbers(dto.getSeatNumbers());

        BookingResponseDTO response = createBooking(request);
        if (response == null || response.getBookingId() == null) {
            return null;
        }

        BookingDTO result = new BookingDTO();
        result.setBookingId(response.getBookingId());
        result.setUserId(dto.getUserId());
        result.setTripId(dto.getTripId());
        result.setSeatNumbers(dto.getSeatNumbers());
        result.setTotalPrice(response.getTotalAmount());
        result.setTicketCode(response.getTicketCode());
        result.setStatus(response.getStatus());
        return result;
    }

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        // MODIFIED
        if (request == null || request.getUserId() == null || request.getTripId() == null) {
            throw new IllegalArgumentException("Invalid booking request.");
        }
        boolean hasSeatNumbers = request.getSeatNumbers() != null && !request.getSeatNumbers().isEmpty();
        boolean hasSeatIds = request.getSeatIds() != null && !request.getSeatIds().isEmpty();
        if (!hasSeatNumbers && !hasSeatIds) {
            throw new IllegalArgumentException("At least one seat is required.");
        }

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            TripSnapshot trip = findTripForUpdate(request.getTripId());
            if (trip == null) {
                throw new IllegalStateException("Trip not found.");
            }
            if (!"OPEN".equalsIgnoreCase(trip.status)) {
                throw new IllegalStateException("Trip is not OPEN.");
            }

            List<Long> requestedSeatIds;
            if (hasSeatIds) {
                requestedSeatIds = request.getSeatIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (requestedSeatIds.size() != request.getSeatIds().size()) {
                    throw new IllegalArgumentException("Duplicate or invalid seat ids.");
                }
                List<Long> seatsInBus = findSeatIdsForBusForUpdate(trip.busId, requestedSeatIds);
                if (seatsInBus.size() != requestedSeatIds.size()) {
                    throw new IllegalStateException("One or more seat ids do not belong to this trip.");
                }
            } else {
                List<String> requestedSeatNumbers = request.getSeatNumbers().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();
                if (requestedSeatNumbers.size() != request.getSeatNumbers().size()) {
                    throw new IllegalArgumentException("Duplicate or invalid seat numbers.");
                }

                requestedSeatIds = findSeatIds(request.getTripId(), requestedSeatNumbers);
                if (requestedSeatIds.size() != requestedSeatNumbers.size()) {
                    throw new IllegalStateException("One or more seats do not belong to this trip.");
                }
            }

            List<Long> bookedSeatIds = bookingDAO.findBookedSeatIdsByTrip(request.getTripId());
            Set<Long> bookedSet = new HashSet<>(bookedSeatIds);
            List<Long> conflicts = requestedSeatIds.stream().filter(bookedSet::contains).toList();
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("SeatAlreadyBookedException: " + conflicts);
            }

            // Extra guard for selected seat ids under the same lock scope.
            List<Long> directConflicts = bookingDAO.findByTripAndSeatIds(request.getTripId(), requestedSeatIds);
            if (!directConflicts.isEmpty()) {
                throw new IllegalStateException("SeatAlreadyBookedException: " + directConflicts);
            }

            double totalPrice = trip.price * requestedSeatIds.size();
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());
            booking.setTripId(request.getTripId());
            booking.setTotalPrice(totalPrice);
            booking.setTicketCode(generateTicketCode());
            booking.setStatus(BookingStatus.PENDING);

            Long bookingId = bookingDAO.save(booking);
            if (bookingId == null) {
                throw new IllegalStateException("Failed to create booking.");
            }
            bookingSeatDAO.saveAll(bookingId, requestedSeatIds);

            connection.commit();

            BookingResponseDTO response = new BookingResponseDTO();
            response.setBookingId(bookingId);
            response.setTicketCode(booking.getTicketCode());
            response.setTotalAmount(totalPrice);
            response.setStatus(BookingStatus.PENDING.name());
            response.setPaymentStatus("PENDING");
            return response;
        } catch (Exception ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(ex);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean confirmBooking(Long bookingId) {
        if (bookingId == null) {
            return false;
        }
        String sql = """
            UPDATE bookings b
            SET b.status = 'CONFIRMED'
            WHERE b.booking_id = ?
              AND b.status = 'PENDING'
              AND EXISTS (
                  SELECT 1
                  FROM payments p
                  WHERE p.booking_id = b.booking_id
                    AND p.payment_status = 'PAID'
              )
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long userId) {
        // MODIFIED
        if (bookingId == null || userId == null) {
            return false;
        }
        return bookingDAO.cancelBooking(bookingId, userId);
    }

    @Override
    public List<BookingDTO> getBookingsByUserId(Long userId) {
        // ADDED
        if (userId == null) {
            return List.of();
        }
        List<Booking> bookings = bookingDAO.findByUserId(userId);
        List<BookingDTO> result = new ArrayList<>();
        for (Booking booking : bookings) {
            BookingDTO dto = new BookingDTO();
            dto.setBookingId(booking.getBookingId());
            dto.setUserId(booking.getUserId());
            dto.setTripId(booking.getTripId());
            dto.setSeatNumbers(booking.getSeatNumbers());
            dto.setTotalPrice(booking.getTotalPrice());
            dto.setTicketCode(booking.getTicketCode());
            dto.setStatus(booking.getStatus() == null ? null : booking.getStatus().name());
            dto.setPaymentStatus(booking.getPaymentStatus());
            dto.setBookingDate(booking.getBookingDate());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<Long> getBookedSeatIds(Long tripId) {
        // Validate required identifier.
        if (tripId == null) {
            return List.of();
        }
        return findBookedSeatIdsByTrip(tripId);
    }

    @Override
    public List<Long> findBookedSeatIdsByTrip(Long tripId) {
        // ADDED
        if (tripId == null) {
            return List.of();
        }
        return bookingDAO.findBookedSeatIdsByTrip(tripId);
    }

    @Override
    public List<String> getAvailableSeatNumbers(Long tripId) {
        // MODIFIED
        if (tripId == null) {
            return List.of();
        }
        return seatDAO.findAvailableSeatNumbersByTrip(tripId);
    }

    @Override
    public int cancelExpiredPending(int minutes) {
        // Validate required parameter.
        if (minutes <= 0) {
            return 0;
        }
        return bookingDAO.cancelExpiredPending(minutes);
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

    private void lockSeatsForTrip(Long tripId) throws SQLException {
        String sql = """
            SELECT s.seat_id
            FROM seats s
            JOIN trips t ON t.bus_id = s.bus_id
            WHERE t.trip_id = ?
            FOR UPDATE
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            ps.executeQuery();
        }
    }

    // ADDED
    private TripSnapshot findTripForUpdate(Long tripId) throws SQLException {
        String sql = """
            SELECT trip_id, bus_id, price, status
            FROM trips
            WHERE trip_id = ?
            FOR UPDATE
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                TripSnapshot snapshot = new TripSnapshot();
                snapshot.tripId = rs.getLong("trip_id");
                snapshot.busId = rs.getLong("bus_id");
                snapshot.price = rs.getDouble("price");
                snapshot.status = rs.getString("status");
                return snapshot;
            }
        }
    }

    // ADDED
    private List<Long> findSeatIdsForBusForUpdate(Long busId, List<Long> seatIds) throws SQLException {
        if (busId == null || seatIds == null || seatIds.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < seatIds.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }

        String sql = """
            SELECT seat_id
            FROM seats
            WHERE bus_id = ?
              AND seat_id IN (%s)
            FOR UPDATE
        """.formatted(placeholders);

        List<Long> matched = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, busId);
            for (int i = 0; i < seatIds.size(); i++) {
                ps.setLong(i + 2, seatIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    matched.add(rs.getLong("seat_id"));
                }
            }
        }
        return matched;
    }

    private String generateTicketCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "TCK" + raw.substring(0, 12);
    }

    // ADDED
    private static final class TripSnapshot {
        private Long tripId;
        private Long busId;
        private double price;
        private String status;
    }
}
