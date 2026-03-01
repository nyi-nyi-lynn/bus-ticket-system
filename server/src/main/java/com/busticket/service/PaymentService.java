package com.busticket.service;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.exception.UnauthorizedException;

public interface PaymentService {
    PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException;
}
