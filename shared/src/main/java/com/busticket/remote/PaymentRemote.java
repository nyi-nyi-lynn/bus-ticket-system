package com.busticket.remote;

import com.busticket.dto.PaymentDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PaymentRemote extends Remote {
    PaymentDTO createPayment(PaymentDTO dto) throws RemoteException;
    PaymentDTO processPayment(PaymentDTO dto) throws RemoteException; // ADDED

    PaymentDTO getPaymentById(Long paymentId) throws RemoteException;

    PaymentDTO getPaymentByBookingId(Long bookingId) throws RemoteException;
}
