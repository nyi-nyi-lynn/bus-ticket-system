package com.busticket.dao.impl;

import com.busticket.dao.TicketDAO;
import com.busticket.dto.TicketDetailsDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.util.Arrays;
import java.util.List;

public class TicketDAOImpl implements TicketDAO {

    private final Connection connection;

    public TicketDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public TicketDetailsDTO findTicketDetailsByBookingId(Long bookingId) {
        if (connection == null || bookingId == null) {
            return null;
        }

        String sql = """
            SELECT b.booking_id, b.ticket_code, b.total_price, b.status AS booking_status,
                   u.name AS passenger_name,
                   r.origin_city, r.destination_city,
                   t.travel_date, t.departure_time, t.arrival_time,
                   bu.bus_number, bu.type AS bus_type,
                   GROUP_CONCAT(s.seat_number ORDER BY s.seat_number SEPARATOR ',') AS seat_numbers,
                   (
                       SELECT p.payment_status
                       FROM payments p
                       WHERE p.booking_id = b.booking_id
                       ORDER BY p.payment_id DESC
                       LIMIT 1
                   ) AS payment_status
            FROM bookings b
            JOIN users u ON u.user_id = b.user_id
            JOIN trips t ON t.trip_id = b.trip_id
            JOIN routes r ON r.route_id = t.route_id
            JOIN buses bu ON bu.bus_id = t.bus_id
            LEFT JOIN booking_seat bs ON bs.booking_id = b.booking_id
            LEFT JOIN seats s ON s.seat_id = bs.seat_id
            WHERE b.booking_id = ?
            GROUP BY b.booking_id, b.ticket_code, b.total_price, b.status, u.name,
                     r.origin_city, r.destination_city, t.travel_date, t.departure_time, t.arrival_time,
                     bu.bus_number, bu.type
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                TicketDetailsDTO dto = new TicketDetailsDTO();
                dto.setTicketCode(rs.getString("ticket_code"));
                dto.setPassengerName(rs.getString("passenger_name"));
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
                Time arrivalTime = rs.getTime("arrival_time");
                if (arrivalTime != null) {
                    dto.setArrivalTime(arrivalTime.toLocalTime());
                }

                dto.setBusNumber(rs.getString("bus_number"));
                dto.setBusType(rs.getString("bus_type"));

                String seatNumbers = rs.getString("seat_numbers");
                if (seatNumbers == null || seatNumbers.isBlank()) {
                    dto.setSeatNumbers(List.of());
                } else {
                    dto.setSeatNumbers(Arrays.stream(seatNumbers.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList());
                }

                dto.setTotalPrice(rs.getDouble("total_price"));

                String bookingStatus = rs.getString("booking_status");
                if (bookingStatus != null && !bookingStatus.isBlank()) {
                    try {
                        dto.setBookingStatus(BookingStatus.valueOf(bookingStatus.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        dto.setBookingStatus(null);
                    }
                }

                String paymentStatus = rs.getString("payment_status");
                if (paymentStatus != null && !paymentStatus.isBlank()) {
                    try {
                        dto.setPaymentStatus(PaymentStatus.valueOf(paymentStatus.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        dto.setPaymentStatus(null);
                    }
                }

                return dto;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
