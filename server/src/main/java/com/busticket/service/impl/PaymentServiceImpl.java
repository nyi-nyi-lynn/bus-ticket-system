package com.busticket.service.impl;

import com.busticket.dao.PaymentDAO;
import com.busticket.dao.impl.PaymentDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.PaymentStatus;
import com.busticket.exception.UnauthorizedException;
import com.busticket.model.Payment;
import com.busticket.service.PaymentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;

public class PaymentServiceImpl implements PaymentService {
    private static final int BOOKING_EXPIRY_MINUTES = 15;
    private static final double AMOUNT_EPSILON = 0.0001;

    private final PaymentDAO paymentDAO;
    private final Connection connection;

    public PaymentServiceImpl() {
        connection = DatabaseConnection.getConnection();
        paymentDAO = new PaymentDAOImpl(connection);
    }

    @Override
    public PaymentDTO createPayment(PaymentDTO dto) throws UnauthorizedException {
        return processPayment(toRequest(dto));
    }

    @Override
    public PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException {
        validateRequest(request);

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            BookingPaymentContext context = findBookingForPayment(request.getBookingId());
            if (context == null) {
                throw new IllegalStateException("Booking does not exist.");
            }
            if (!context.userId.equals(request.getUserId())) {
                throw new SecurityException("Only the booking owner can pay this booking.");
            }
            if (!"PENDING".equalsIgnoreCase(context.status)) {
                throw new IllegalStateException("Only PENDING bookings can be paid.");
            }
            if (isExpired(context.createdAt)) {
                throw new IllegalStateException("Booking has expired.");
            }
            if (hasExistingPayment(context.bookingId)) {
                throw new IllegalStateException("Booking has already been paid.");
            }
            if (Math.abs(request.getAmount() - context.totalPrice) > AMOUNT_EPSILON) {
                throw new IllegalArgumentException("Paid amount does not match booking total.");
            }

            Payment payment = new Payment();
            payment.setBookingId(context.bookingId);
            payment.setPaymentMethod(parseMethod(request.getPaymentMethod()));
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAmount(context.totalPrice);

            Long paymentId = paymentDAO.create(payment);
            if (paymentId == null) {
                throw new IllegalStateException("Failed to insert payment.");
            }

            int updated = markBookingPaid(context.bookingId);
            if (updated != 1) {
                throw new IllegalStateException("Failed to confirm booking after payment.");
            }

            connection.commit();

            PaymentDTO result = new PaymentDTO();
            result.setPaymentId(paymentId);
            result.setUserId(context.userId);
            result.setBookingId(context.bookingId);
            result.setPaymentMethod(payment.getPaymentMethod());
            result.setPaymentStatus(PaymentStatus.PAID.name());
            result.setPaidAmount(context.totalPrice);
            return result;
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
    public PaymentDTO processPayment(PaymentDTO dto) throws UnauthorizedException {
        return processPayment(toRequest(dto));
    }

    @Override
    public PaymentDTO getPaymentById(Long paymentId) {
        if (paymentId == null) {
            return null;
        }
        Payment payment = paymentDAO.findById(paymentId);
        return toDTO(payment);
    }

    @Override
    public PaymentDTO getPaymentByBookingId(Long bookingId) {
        if (bookingId == null) {
            return null;
        }
        Payment payment = paymentDAO.findByBookingId(bookingId);
        return toDTO(payment);
    }

    private void validateRequest(PaymentRequestDTO request) throws UnauthorizedException {
        if (request == null) {
            throw new IllegalArgumentException("Payment request is required.");
        }
        if (request.getUserId() == null) {
            throw new UnauthorizedException("Please login to continue booking");
        }
        if (request.getBookingId() == null) {
            throw new IllegalArgumentException("Booking id is required.");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Paid amount must be greater than zero.");
        }
        parseMethod(request.getPaymentMethod());
    }

    private BookingPaymentContext findBookingForPayment(Long bookingId) throws SQLException {
        String sql = """
            SELECT booking_id, user_id, total_price, status, created_at
            FROM bookings
            WHERE booking_id = ?
            FOR UPDATE
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                BookingPaymentContext context = new BookingPaymentContext();
                context.bookingId = rs.getLong("booking_id");
                context.userId = rs.getLong("user_id");
                context.totalPrice = rs.getDouble("total_price");
                context.status = rs.getString("status");
                context.createdAt = rs.getTimestamp("created_at");
                return context;
            }
        }
    }

    private boolean hasExistingPayment(Long bookingId) throws SQLException {
        String sql = "SELECT payment_id FROM payments WHERE booking_id = ? FOR UPDATE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int markBookingPaid(Long bookingId) throws SQLException {
        String sql = """
            UPDATE bookings
            SET status = 'CONFIRMED'
            WHERE booking_id = ?
              AND status = 'PENDING'
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            return ps.executeUpdate();
        }
    }

    private boolean isExpired(Timestamp createdAt) {
        if (createdAt == null) {
            return true;
        }
        LocalDateTime expiry = createdAt.toLocalDateTime().plusMinutes(BOOKING_EXPIRY_MINUTES);
        return LocalDateTime.now().isAfter(expiry);
    }

    private String parseMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported payment method.");
        }
    }

    private PaymentRequestDTO toRequest(PaymentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Payment payload is required.");
        }
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setUserId(dto.getUserId());
        request.setBookingId(dto.getBookingId());
        request.setAmount(dto.getPaidAmount());
        request.setPaymentMethod(dto.getPaymentMethod());
        return request;
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

    private static final class BookingPaymentContext {
        private Long bookingId;
        private Long userId;
        private double totalPrice;
        private String status;
        private Timestamp createdAt;
    }
}
