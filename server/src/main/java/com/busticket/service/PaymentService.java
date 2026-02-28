package com.busticket.service;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.exception.UnauthorizedException;

public interface PaymentService {
    PaymentDTO createPayment(PaymentDTO dto) throws UnauthorizedException;
    PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException;
    PaymentDTO processPayment(PaymentDTO dto) throws UnauthorizedException; // ADDED

    PaymentDTO getPaymentById(Long paymentId);

    PaymentDTO getPaymentByBookingId(Long bookingId);
}
