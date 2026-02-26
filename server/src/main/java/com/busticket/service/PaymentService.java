package com.busticket.service;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;

public interface PaymentService {
    PaymentDTO createPayment(PaymentDTO dto);
    PaymentDTO processPayment(PaymentRequestDTO request);
    PaymentDTO processPayment(PaymentDTO dto); // ADDED

    PaymentDTO getPaymentById(Long paymentId);

    PaymentDTO getPaymentByBookingId(Long bookingId);
}
