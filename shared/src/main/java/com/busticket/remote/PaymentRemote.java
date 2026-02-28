package com.busticket.remote;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.exception.UnauthorizedException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PaymentRemote extends Remote {
    PaymentDTO createPayment(PaymentDTO dto) throws UnauthorizedException, RemoteException;
    PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException, RemoteException;
    PaymentDTO processPayment(PaymentDTO dto) throws UnauthorizedException, RemoteException; // ADDED

    PaymentDTO getPaymentById(Long paymentId) throws RemoteException;

    PaymentDTO getPaymentByBookingId(Long bookingId) throws RemoteException;
}
