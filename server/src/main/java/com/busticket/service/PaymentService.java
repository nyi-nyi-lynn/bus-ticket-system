package com.busticket.service;

import com.busticket.dto.PaymentDTO;

public interface PaymentService {
    PaymentDTO createPayment(PaymentDTO dto);
    PaymentDTO processPayment(PaymentDTO dto); // ADDED

    PaymentDTO getPaymentById(Long paymentId);

    PaymentDTO getPaymentByBookingId(Long bookingId);
}
