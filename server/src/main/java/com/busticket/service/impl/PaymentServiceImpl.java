package com.busticket.service.impl;

import com.busticket.dao.PaymentDAO;
import com.busticket.dao.impl.PaymentDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.PaymentDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.PaymentStatus;
import com.busticket.model.Payment;
import com.busticket.service.PaymentService;

import java.util.Locale;

public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentServiceImpl(){
        paymentDAO = new PaymentDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public PaymentDTO createPayment(PaymentDTO dto) {
        if (dto == null || dto.getBookingId() == null) {
            return null;
        }

        Payment payment = new Payment();
        payment.setBookingId(dto.getBookingId());
        payment.setPaymentMethod(parseMethod(dto.getPaymentMethod()));
        payment.setPaymentStatus(parseStatus(dto.getPaymentStatus()));
        payment.setPaidAmount(dto.getPaidAmount());

        Long id = paymentDAO.create(payment);
        if (id == null) {
            return null;
        }

        PaymentDTO result = new PaymentDTO();
        result.setPaymentId(id);
        result.setBookingId(payment.getBookingId());
        result.setPaymentMethod(payment.getPaymentMethod());
        result.setPaymentStatus(payment.getPaymentStatus().name());
        result.setPaidAmount(payment.getPaidAmount());
        return result;
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
