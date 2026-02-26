package com.busticket.rmi;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.remote.PaymentRemote;
import com.busticket.service.PaymentService;
import com.busticket.service.impl.PaymentServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PaymentRemoteImpl extends UnicastRemoteObject implements PaymentRemote {
    public PaymentRemoteImpl() throws RemoteException{
    }


    private PaymentService paymentService() {
        return new PaymentServiceImpl();
    }

    @Override
    public PaymentDTO createPayment(PaymentDTO dto) throws RemoteException {
        return paymentService().createPayment(dto);
    }

    @Override
    public PaymentDTO processPayment(PaymentRequestDTO request) throws RemoteException {
        return paymentService().processPayment(request);
    }

    @Override
    public PaymentDTO processPayment(PaymentDTO dto) throws RemoteException {
        // ADDED
        return paymentService().processPayment(dto);
    }

    @Override
    public PaymentDTO getPaymentById(Long paymentId) throws RemoteException {
        return paymentService().getPaymentById(paymentId);
    }

    @Override
    public PaymentDTO getPaymentByBookingId(Long bookingId) throws RemoteException {
        return paymentService().getPaymentByBookingId(bookingId);
    }
}
