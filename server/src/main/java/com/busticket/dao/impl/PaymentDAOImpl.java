package com.busticket.dao.impl;

import com.busticket.dao.PaymentDAO;
import com.busticket.enums.PaymentStatus;
import com.busticket.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PaymentDAOImpl implements PaymentDAO {

    private final Connection connection ;

    public PaymentDAOImpl(Connection connection){
        this.connection = connection;
    }

    @Override
    public Long create(Payment payment) {
        String sql = """
            INSERT INTO payments(booking_id, payment_method, payment_status, paid_amount, paid_at)
            VALUES(?,?,?,?,?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.getBookingId());
            ps.setString(2, payment.getPaymentMethod());
            ps.setString(3, payment.getPaymentStatus().name());
            ps.setDouble(4, payment.getPaidAmount() == null ? 0.0 : payment.getPaidAmount());
            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            } else {
                ps.setTimestamp(5, null);
            }

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
    public Payment findById(Long paymentId) {
        String sql = "SELECT payment_id, booking_id, payment_method, payment_status, paid_amount FROM payments WHERE payment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Payment findByBookingId(Long bookingId) {
        String sql = "SELECT payment_id, booking_id, payment_method, payment_status, paid_amount FROM payments WHERE booking_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getLong("payment_id"));
        payment.setBookingId(rs.getLong("booking_id"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        payment.setPaidAmount(rs.getDouble("paid_amount"));
        return payment;
    }
}
