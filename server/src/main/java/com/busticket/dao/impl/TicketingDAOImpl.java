package com.busticket.dao.impl;

import com.busticket.dao.TicketingDAO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.TripStatus;
import com.busticket.model.Booking;
import com.busticket.model.Bus;
import com.busticket.model.Payment;
import com.busticket.model.Route;
import com.busticket.model.SalesReport;
import com.busticket.model.Seat;
import com.busticket.model.Ticket;
import com.busticket.model.Trip;
import com.busticket.util.TicketCodeUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketingDAOImpl implements TicketingDAO {
    private final Connection connection;

    public TicketingDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<String> getOriginCities() {
        List<String> cities = new ArrayList<>();
        if (connection == null) {
            return cities;
        }
        String sql = "SELECT DISTINCT origin_city FROM routes ORDER BY origin_city";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cities.add(rs.getString("origin_city"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cities;
    }

    @Override
    public List<String> getDestinationCities(String originCity) {
        List<String> cities = new ArrayList<>();
        if (connection == null) {
            return cities;
        }
        String sql = """
                SELECT DISTINCT destination_city
                FROM routes
                WHERE (? IS NULL OR ? = '' OR origin_city = ?)
                ORDER BY destination_city
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, originCity);
            ps.setString(2, originCity);
            ps.setString(3, originCity);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cities.add(rs.getString("destination_city"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cities;
    }

    @Override
    public List<Trip> getAdvertisedTrips() {
        releaseExpiredPendingBookings();
        List<Trip> trips = new ArrayList<>();
        if (connection == null) {
            return trips;
        }
        String sql = """
                SELECT t.trip_id, t.bus_id, t.route_id, t.travel_date, t.departure_time, t.arrival_time, t.price, t.status,
                       b.bus_number, b.total_seats, r.origin_city, r.destination_city,
                       (b.total_seats - IFNULL(bs.booked_count, 0)) AS available_seats
                FROM trips t
                JOIN buses b ON t.bus_id = b.bus_id
                JOIN routes r ON t.route_id = r.route_id
                LEFT JOIN (
                    SELECT bk.trip_id, COUNT(*) AS booked_count
                    FROM bookings bk
                    JOIN booking_seat bks ON bk.booking_id = bks.booking_id
                    WHERE bk.status = 'CONFIRMED' OR (bk.status = 'PENDING' AND bk.created_at >= (NOW() - INTERVAL 15 MINUTE))
                    GROUP BY bk.trip_id
                ) bs ON bs.trip_id = t.trip_id
                WHERE t.status = 'OPEN' AND t.travel_date >= CURDATE()
                ORDER BY t.travel_date, t.departure_time
                LIMIT 60
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                trips.add(mapTrip(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trips;
    }

    @Override
    public boolean addBus(Bus bus) {
        if (connection == null) {
            return false;
        }
        String insertBus = "INSERT INTO buses(bus_number, type, total_seats) VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertBus, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getType().toUpperCase());
            ps.setInt(3, bus.getTotalSeats());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                return false;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long busId = rs.getLong(1);
                    bus.setBusId(busId);
                    createDefaultSeats(busId, bus.getTotalSeats());
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addRoute(Route route) {
        if (connection == null) {
            return false;
        }
        String sql = "INSERT INTO routes(origin_city, destination_city, distance_km, estimated_duration) VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, route.getOriginCity());
            ps.setString(2, route.getDestinationCity());
            ps.setDouble(3, route.getDistanceKm());
            ps.setString(4, route.getEstimatedDuration());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean createTrip(Trip trip) {
        if (connection == null) {
            return false;
        }
        String sql = "INSERT INTO trips(bus_id, route_id, travel_date, departure_time, arrival_time, price, status) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trip.getBusId());
            ps.setLong(2, trip.getRouteId());
            ps.setDate(3, Date.valueOf(trip.getTravelDate()));
            ps.setTime(4, Time.valueOf(trip.getDepartureTime()));
            ps.setTime(5, Time.valueOf(trip.getArrivalTime()));
            ps.setDouble(6, trip.getPrice());
            ps.setString(7, trip.getStatus() == null ? TripStatus.OPEN.name() : trip.getStatus().toUpperCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Trip> searchTrips(String originCity, String destinationCity, LocalDate travelDate) {
        releaseExpiredPendingBookings();
        List<Trip> trips = new ArrayList<>();
        if (connection == null) {
            return trips;
        }
        String sql = """
                SELECT t.trip_id, t.bus_id, t.route_id, t.travel_date, t.departure_time, t.arrival_time, t.price, t.status,
                       b.bus_number, b.total_seats, r.origin_city, r.destination_city,
                       (b.total_seats - IFNULL(bs.booked_count, 0)) AS available_seats
                FROM trips t
                JOIN buses b ON t.bus_id = b.bus_id
                JOIN routes r ON t.route_id = r.route_id
                LEFT JOIN (
                    SELECT bk.trip_id, COUNT(*) AS booked_count
                    FROM bookings bk
                    JOIN booking_seat bks ON bk.booking_id = bks.booking_id
                    WHERE bk.status = 'CONFIRMED' OR (bk.status = 'PENDING' AND bk.created_at >= (NOW() - INTERVAL 15 MINUTE))
                    GROUP BY bk.trip_id
                ) bs ON bs.trip_id = t.trip_id
                WHERE t.status = 'OPEN'
                  AND r.origin_city = ?
                  AND r.destination_city = ?
                  AND t.travel_date = ?
                ORDER BY t.departure_time
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, originCity);
            ps.setString(2, destinationCity);
            ps.setDate(3, Date.valueOf(travelDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapTrip(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trips;
    }

    @Override
    public List<Trip> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate) {
        releaseExpiredPendingBookings();
        List<Trip> trips = new ArrayList<>();
        if (connection == null) {
            return trips;
        }
        String sql = """
                SELECT t.trip_id, t.bus_id, t.route_id, t.travel_date, t.departure_time, t.arrival_time, t.price, t.status,
                       b.bus_number, b.total_seats, r.origin_city, r.destination_city,
                       (b.total_seats - IFNULL(bs.booked_count, 0)) AS available_seats
                FROM trips t
                JOIN buses b ON t.bus_id = b.bus_id
                JOIN routes r ON t.route_id = r.route_id
                LEFT JOIN (
                    SELECT bk.trip_id, COUNT(*) AS booked_count
                    FROM bookings bk
                    JOIN booking_seat bks ON bk.booking_id = bks.booking_id
                    WHERE bk.status = 'CONFIRMED' OR (bk.status = 'PENDING' AND bk.created_at >= (NOW() - INTERVAL 15 MINUTE))
                    GROUP BY bk.trip_id
                ) bs ON bs.trip_id = t.trip_id
                WHERE t.status = 'OPEN'
                  AND r.origin_city = ?
                  AND r.destination_city = ?
                  AND t.travel_date >= CURDATE()
                ORDER BY ABS(DATEDIFF(t.travel_date, ?)), t.travel_date, t.departure_time
                LIMIT 20
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, originCity);
            ps.setString(2, destinationCity);
            ps.setDate(3, Date.valueOf(requestedDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapTrip(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trips;
    }

    @Override
    public List<Seat> getAvailableSeats(Long tripId) {
        releaseExpiredPendingBookings();
        List<Seat> seats = new ArrayList<>();
        if (connection == null) {
            return seats;
        }
        String sql = """
                SELECT s.seat_id, s.bus_id, s.seat_number
                FROM seats s
                JOIN trips t ON t.bus_id = s.bus_id
                WHERE t.trip_id = ?
                  AND NOT EXISTS (
                    SELECT 1
                    FROM booking_seat bs
                    JOIN bookings b ON b.booking_id = bs.booking_id
                    WHERE bs.seat_id = s.seat_id
                      AND b.trip_id = t.trip_id
                      AND (b.status = 'CONFIRMED' OR (b.status = 'PENDING' AND b.created_at >= (NOW() - INTERVAL 15 MINUTE)))
                  )
                ORDER BY s.seat_number
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat seat = new Seat();
                    seat.setSeatId(rs.getLong("seat_id"));
                    seat.setBusId(rs.getLong("bus_id"));
                    seat.setSeatNumber(rs.getString("seat_number"));
                    seats.add(seat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return seats;
    }

    @Override
    public List<Booking> getBookingsByUser(Long userId) {
        releaseExpiredPendingBookings();
        List<Booking> bookings = new ArrayList<>();
        if (connection == null || userId == null) {
            return bookings;
        }
        String sql = """
                SELECT booking_id, user_id, trip_id, booking_date, total_price, ticket_code, status
                FROM bookings
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;
        String seatSql = """
                SELECT s.seat_number
                FROM booking_seat bs
                JOIN seats s ON s.seat_id = bs.seat_id
                WHERE bs.booking_id = ?
                ORDER BY s.seat_number
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setUserId(rs.getLong("user_id"));
                    booking.setTripId(rs.getLong("trip_id"));
                    booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());
                    booking.setTotalPrice(rs.getDouble("total_price"));
                    booking.setTicketCode(rs.getString("ticket_code"));
                    booking.setStatus(rs.getString("status"));
                    List<String> seats = new ArrayList<>();
                    try (PreparedStatement sps = connection.prepareStatement(seatSql)) {
                        sps.setLong(1, booking.getBookingId());
                        try (ResultSet srs = sps.executeQuery()) {
                            while (srs.next()) {
                                seats.add(srs.getString("seat_number"));
                            }
                        }
                    }
                    booking.setSeatNumbers(seats);
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    @Override
    public Booking createBooking(Booking request) {
        if (connection == null) {
            return null;
        }
        if (request == null || request.getUserId() == null || request.getTripId() == null
                || request.getSeatNumbers() == null || request.getSeatNumbers().isEmpty()) {
            return null;
        }

        try {
            connection.setAutoCommit(false);

            TripPriceData tripData = getTripPriceData(request.getTripId());
            if (tripData == null || !TripStatus.OPEN.name().equalsIgnoreCase(tripData.status)) {
                connection.rollback();
                return null;
            }

            List<Long> seatIds = resolveSeatIdsByNumbers(tripData.busId, request.getSeatNumbers());
            if (seatIds.size() != request.getSeatNumbers().size()) {
                connection.rollback();
                return null;
            }
            if (!areSeatsAvailableForTrip(request.getTripId(), seatIds)) {
                connection.rollback();
                return null;
            }

            double total = tripData.price * seatIds.size();
            String ticketCode = TicketCodeUtil.generate();
            LocalDateTime now = LocalDateTime.now();

            long bookingId = insertBooking(request.getUserId(), request.getTripId(), total, ticketCode, now);
            if (bookingId <= 0) {
                connection.rollback();
                return null;
            }

            for (Long seatId : seatIds) {
                insertBookingSeat(bookingId, seatId);
            }

            connection.commit();

            Booking booking = new Booking();
            booking.setBookingId(bookingId);
            booking.setUserId(request.getUserId());
            booking.setTripId(request.getTripId());
            booking.setBookingDate(now);
            booking.setTotalPrice(total);
            booking.setTicketCode(ticketCode);
            booking.setStatus(BookingStatus.PENDING.name());
            booking.setSeatNumbers(new ArrayList<>(request.getSeatNumbers()));
            return booking;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Payment makePayment(Long bookingId, String paymentMethod, double paidAmount) {
        releaseExpiredPendingBookings();
        if (connection == null) {
            return null;
        }
        String bookingSql = "SELECT total_price FROM bookings WHERE booking_id = ? AND status = 'PENDING'";
        String insertPaymentSql = "INSERT INTO payments(booking_id, payment_method, payment_status, paid_amount, paid_at) VALUES(?, ?, ?, ?, ?)";
        String confirmSql = "UPDATE bookings SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP WHERE booking_id = ?";
        String failSql = "UPDATE bookings SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE booking_id = ?";

        try {
            connection.setAutoCommit(false);

            Double totalPrice = null;
            try (PreparedStatement ps = connection.prepareStatement(bookingSql)) {
                ps.setLong(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalPrice = rs.getDouble("total_price");
                    }
                }
            }

            if (totalPrice == null) {
                connection.rollback();
                return null;
            }

            String normalizedMethod = paymentMethod == null ? "CARD" : paymentMethod.toUpperCase();
            boolean paid = paidAmount >= totalPrice;
            String paymentStatus = paid ? "PAID" : "FAILED";

            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setPaymentMethod(normalizedMethod);
            payment.setPaymentStatus(paymentStatus);
            payment.setPaidAmount(paidAmount);
            payment.setPaidAt(LocalDateTime.now());

            try (PreparedStatement ps = connection.prepareStatement(insertPaymentSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, bookingId);
                ps.setString(2, normalizedMethod);
                ps.setString(3, paymentStatus);
                ps.setDouble(4, paidAmount);
                ps.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setPaymentId(rs.getLong(1));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(paid ? confirmSql : failSql)) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }

            connection.commit();
            return payment;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public List<Payment> getPaymentsByUser(Long userId) {
        releaseExpiredPendingBookings();
        List<Payment> payments = new ArrayList<>();
        if (connection == null || userId == null) {
            return payments;
        }
        String sql = """
                SELECT p.payment_id, p.booking_id, p.payment_method, p.payment_status, p.paid_amount, p.paid_at
                FROM payments p
                JOIN bookings b ON b.booking_id = p.booking_id
                WHERE b.user_id = ?
                ORDER BY p.paid_at DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment payment = new Payment();
                    payment.setPaymentId(rs.getLong("payment_id"));
                    payment.setBookingId(rs.getLong("booking_id"));
                    payment.setPaymentMethod(rs.getString("payment_method"));
                    payment.setPaymentStatus(rs.getString("payment_status"));
                    payment.setPaidAmount(rs.getDouble("paid_amount"));
                    Timestamp paidAt = rs.getTimestamp("paid_at");
                    if (paidAt != null) {
                        payment.setPaidAt(paidAt.toLocalDateTime());
                    }
                    payments.add(payment);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public List<Ticket> getTicketsByUser(Long userId) {
        releaseExpiredPendingBookings();
        List<Ticket> tickets = new ArrayList<>();
        if (connection == null || userId == null) {
            return tickets;
        }
        String sql = """
                SELECT b.booking_id, b.ticket_code, b.status AS booking_status,
                       u.name AS passenger_name, tr.travel_date, tr.departure_time,
                       bu.bus_number, r.origin_city, r.destination_city
                FROM bookings b
                JOIN users u ON u.user_id = b.user_id
                JOIN trips tr ON tr.trip_id = b.trip_id
                JOIN buses bu ON bu.bus_id = tr.bus_id
                JOIN routes r ON r.route_id = tr.route_id
                WHERE b.user_id = ? AND b.status IN ('CONFIRMED', 'PENDING')
                ORDER BY b.created_at DESC
                """;
        String seatSql = """
                SELECT s.seat_number
                FROM booking_seat bs
                JOIN seats s ON s.seat_id = bs.seat_id
                WHERE bs.booking_id = ?
                ORDER BY s.seat_number
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket ticket = new Ticket();
                    ticket.setTicketCode(rs.getString("ticket_code"));
                    ticket.setPassengerName(rs.getString("passenger_name"));
                    ticket.setBookingStatus(rs.getString("booking_status"));
                    ticket.setTravelDate(rs.getDate("travel_date").toLocalDate());
                    ticket.setDepartureTime(rs.getTime("departure_time").toLocalTime());
                    ticket.setBusNumber(rs.getString("bus_number"));
                    ticket.setOriginCity(rs.getString("origin_city"));
                    ticket.setDestinationCity(rs.getString("destination_city"));
                    long bookingId = rs.getLong("booking_id");
                    List<String> seats = new ArrayList<>();
                    try (PreparedStatement sps = connection.prepareStatement(seatSql)) {
                        sps.setLong(1, bookingId);
                        try (ResultSet srs = sps.executeQuery()) {
                            while (srs.next()) {
                                seats.add(srs.getString("seat_number"));
                            }
                        }
                    }
                    ticket.setSeatNumbers(seats);
                    ticket.setQrPayload("TICKET:" + ticket.getTicketCode());
                    tickets.add(ticket);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    @Override
    public Ticket findTicketByCode(String ticketCode) {
        if (connection == null) {
            return null;
        }
        String ticketSql = """
                SELECT b.booking_id, b.ticket_code, b.status AS booking_status,
                       u.name AS passenger_name, tr.travel_date, tr.departure_time,
                       bu.bus_number, r.origin_city, r.destination_city
                FROM bookings b
                JOIN users u ON u.user_id = b.user_id
                JOIN trips tr ON tr.trip_id = b.trip_id
                JOIN buses bu ON bu.bus_id = tr.bus_id
                JOIN routes r ON r.route_id = tr.route_id
                WHERE b.ticket_code = ?
                """;
        String seatsSql = """
                SELECT s.seat_number
                FROM booking_seat bs
                JOIN seats s ON s.seat_id = bs.seat_id
                WHERE bs.booking_id = ?
                ORDER BY s.seat_number
                """;

        try (PreparedStatement ps = connection.prepareStatement(ticketSql)) {
            ps.setString(1, ticketCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Ticket ticket = new Ticket();
                ticket.setTicketCode(rs.getString("ticket_code"));
                ticket.setPassengerName(rs.getString("passenger_name"));
                ticket.setBookingStatus(rs.getString("booking_status"));
                ticket.setTravelDate(rs.getDate("travel_date").toLocalDate());
                ticket.setDepartureTime(rs.getTime("departure_time").toLocalTime());
                ticket.setBusNumber(rs.getString("bus_number"));
                ticket.setOriginCity(rs.getString("origin_city"));
                ticket.setDestinationCity(rs.getString("destination_city"));

                long bookingId = rs.getLong("booking_id");
                List<String> seats = new ArrayList<>();
                try (PreparedStatement seatPs = connection.prepareStatement(seatsSql)) {
                    seatPs.setLong(1, bookingId);
                    try (ResultSet seatRs = seatPs.executeQuery()) {
                        while (seatRs.next()) {
                            seats.add(seatRs.getString("seat_number"));
                        }
                    }
                }
                ticket.setSeatNumbers(seats);
                ticket.setQrPayload("TICKET:" + ticket.getTicketCode());
                return ticket;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean validateTicket(String ticketCode, Long staffUserId) {
        if (connection == null) {
            return false;
        }
        String staffSql = "SELECT role, status FROM users WHERE user_id = ?";
        String ticketSql = "SELECT booking_id, status FROM bookings WHERE ticket_code = ?";
        String checkValidated = "SELECT 1 FROM ticket_validations WHERE booking_id = ?";
        String insertValidation = "INSERT INTO ticket_validations(booking_id, staff_user_id, validated_at) VALUES(?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement staffPs = connection.prepareStatement(staffSql);
             PreparedStatement ticketPs = connection.prepareStatement(ticketSql)) {

            staffPs.setLong(1, staffUserId);
            try (ResultSet rs = staffPs.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String role = rs.getString("role");
                String status = rs.getString("status");
                boolean isAllowedRole = "STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
                if (!isAllowedRole || !"ACTIVE".equalsIgnoreCase(status)) {
                    return false;
                }
            }

            ticketPs.setString(1, ticketCode);
            long bookingId;
            try (ResultSet rs = ticketPs.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                bookingId = rs.getLong("booking_id");
                if (!"CONFIRMED".equalsIgnoreCase(rs.getString("status"))) {
                    return false;
                }
            }

            try (PreparedStatement checkPs = connection.prepareStatement(checkValidated)) {
                checkPs.setLong(1, bookingId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement insertPs = connection.prepareStatement(insertValidation)) {
                insertPs.setLong(1, bookingId);
                insertPs.setLong(2, staffUserId);
                return insertPs.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public SalesReport getSalesReport(LocalDate fromDate, LocalDate toDate) {
        SalesReport report = new SalesReport();
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        if (connection == null) {
            return report;
        }

        String sql = """
                SELECT COUNT(DISTINCT b.booking_id) AS booking_count,
                       IFNULL(SUM(p.paid_amount), 0) AS total_revenue
                FROM bookings b
                JOIN payments p ON p.booking_id = b.booking_id
                WHERE b.status = 'CONFIRMED'
                  AND p.payment_status = 'PAID'
                  AND DATE(b.created_at) BETWEEN ? AND ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    report.setTotalConfirmedBookings(rs.getLong("booking_count"));
                    report.setTotalRevenue(rs.getDouble("total_revenue"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report;
    }

    private void createDefaultSeats(long busId, int totalSeats) throws SQLException {
        String sql = "INSERT INTO seats(bus_id, seat_number) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 1; i <= totalSeats; i++) {
                ps.setLong(1, busId);
                ps.setString(2, "S" + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private TripPriceData getTripPriceData(Long tripId) throws SQLException {
        String sql = "SELECT bus_id, price, status FROM trips WHERE trip_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                TripPriceData data = new TripPriceData();
                data.busId = rs.getLong("bus_id");
                data.price = rs.getDouble("price");
                data.status = rs.getString("status");
                return data;
            }
        }
    }

    private List<Long> resolveSeatIdsByNumbers(Long busId, List<String> seatNumbers) throws SQLException {
        List<Long> seatIds = new ArrayList<>();
        String sql = "SELECT seat_id FROM seats WHERE bus_id = ? AND seat_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String seatNumber : seatNumbers) {
                ps.setLong(1, busId);
                ps.setString(2, seatNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        seatIds.add(rs.getLong("seat_id"));
                    }
                }
            }
        }
        return seatIds;
    }

    private boolean areSeatsAvailableForTrip(Long tripId, List<Long> seatIds) throws SQLException {
        String sql = """
                SELECT 1
                FROM booking_seat bs
                JOIN bookings b ON b.booking_id = bs.booking_id
                WHERE b.trip_id = ?
                  AND bs.seat_id = ?
                  AND (b.status = 'CONFIRMED' OR (b.status = 'PENDING' AND b.created_at >= (NOW() - INTERVAL 15 MINUTE)))
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Long seatId : seatIds) {
                ps.setLong(1, tripId);
                ps.setLong(2, seatId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private long insertBooking(Long userId, Long tripId, double totalPrice, String ticketCode, LocalDateTime bookingDate)
            throws SQLException {
        String sql = "INSERT INTO bookings(user_id, trip_id, booking_date, total_price, ticket_code, status, created_at, updated_at) VALUES(?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, tripId);
            ps.setTimestamp(3, Timestamp.valueOf(bookingDate));
            ps.setDouble(4, totalPrice);
            ps.setString(5, ticketCode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    private void insertBookingSeat(Long bookingId, Long seatId) throws SQLException {
        String sql = "INSERT INTO booking_seat(booking_id, seat_id) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, seatId);
            ps.executeUpdate();
        }
    }

    private static class TripPriceData {
        long busId;
        double price;
        String status;
    }

    private Trip mapTrip(ResultSet rs) throws SQLException {
        Trip trip = new Trip();
        trip.setTripId(rs.getLong("trip_id"));
        trip.setBusId(rs.getLong("bus_id"));
        trip.setRouteId(rs.getLong("route_id"));
        trip.setTravelDate(rs.getDate("travel_date").toLocalDate());
        trip.setDepartureTime(rs.getTime("departure_time").toLocalTime());
        trip.setArrivalTime(rs.getTime("arrival_time").toLocalTime());
        trip.setPrice(rs.getDouble("price"));
        trip.setStatus(rs.getString("status"));
        trip.setBusNumber(rs.getString("bus_number"));
        trip.setOriginCity(rs.getString("origin_city"));
        trip.setDestinationCity(rs.getString("destination_city"));
        trip.setTotalSeats(rs.getInt("total_seats"));
        trip.setAvailableSeats(rs.getInt("available_seats"));
        return trip;
    }

    private void releaseExpiredPendingBookings() {
        if (connection == null) {
            return;
        }
        String sql = """
                UPDATE bookings
                SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                WHERE status = 'PENDING'
                  AND created_at < (NOW() - INTERVAL 15 MINUTE)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
