package com.busticket.service;

import com.busticket.dto.PaymentDTO;

public interface PaymentService {
    PaymentDTO createPayment(PaymentDTO dto);

    PaymentDTO getPaymentById(Long paymentId);

    PaymentDTO getPaymentByBookingId(Long bookingId);
}
