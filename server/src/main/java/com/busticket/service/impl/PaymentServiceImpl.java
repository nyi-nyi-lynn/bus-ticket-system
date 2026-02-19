package com.busticket.service.impl;

import com.busticket.dao.PaymentDAO;
import com.busticket.dao.impl.PaymentDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.PaymentDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.PaymentStatus;
import com.busticket.model.Payment;
import com.busticket.service.PaymentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;
    private final Connection connection;

    public PaymentServiceImpl(){
        connection = DatabaseConnection.getConnection();
        paymentDAO = new PaymentDAOImpl(connection);
    }

    @Override
    public PaymentDTO createPayment(PaymentDTO dto) {
        // Validate required fields.
        if (dto == null || dto.getBookingId() == null) {
            return null;
        }

        boolean originalAutoCommit = true;
        try {
            // Transaction boundary: validate booking and create payment atomically.
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            String bookingSql = "SELECT status FROM bookings WHERE booking_id = ? FOR UPDATE";
            String existingPaymentSql = "SELECT payment_id FROM payments WHERE booking_id = ? FOR UPDATE";

            try (PreparedStatement ps = connection.prepareStatement(bookingSql)) {
                ps.setLong(1, dto.getBookingId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return null;
                    }
                    String status = rs.getString("status");
                    if (!"PENDING".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return null;
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(existingPaymentSql)) {
                ps.setLong(1, dto.getBookingId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Payment existing = paymentDAO.findByBookingId(dto.getBookingId());
                        connection.rollback();
                        return toDTO(existing);
                    }
                }
            }

            Payment payment = new Payment();
            payment.setBookingId(dto.getBookingId());
            payment.setPaymentMethod(parseMethod(dto.getPaymentMethod()));
            payment.setPaymentStatus(parseStatus(dto.getPaymentStatus()));
            payment.setPaidAmount(dto.getPaidAmount());

            Long id = paymentDAO.create(payment);
            if (id == null) {
                connection.rollback();
                return null;
            }

            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                String updateBookingSql = "UPDATE bookings SET status = 'CONFIRMED' WHERE booking_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(updateBookingSql)) {
                    ps.setLong(1, dto.getBookingId());
                    ps.executeUpdate();
                }
            }

            connection.commit();

            PaymentDTO result = new PaymentDTO();
            result.setPaymentId(id);
            result.setBookingId(payment.getBookingId());
            result.setPaymentMethod(payment.getPaymentMethod());
            result.setPaymentStatus(payment.getPaymentStatus().name());
            result.setPaidAmount(payment.getPaidAmount());
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PaymentDTO getPaymentById(Long paymentId) {
        // Validate required identifier.
        if (paymentId == null) {
            return null;
        }
        Payment payment = paymentDAO.findById(paymentId);
        return toDTO(payment);
    }

    @Override
    public PaymentDTO getPaymentByBookingId(Long bookingId) {
        // Validate required identifier.
        if (bookingId == null) {
            return null;
        }
        Payment payment = paymentDAO.findByBookingId(bookingId);
        return toDTO(payment);
    }

    private PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setBookingId(payment.getBookingId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentStatus(payment.getPaymentStatus() == null ? null : payment.getPaymentStatus().name());
        dto.setPaidAmount(payment.getPaidAmount());
        return dto;
    }

    private String parseMethod(String method) {
        if (method == null || method.isBlank()) {
            return PaymentMethod.CARD.name();
        }
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            return PaymentMethod.CARD.name();
        }
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return PaymentStatus.PENDING;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PaymentStatus.PENDING;
        }
    }
}
