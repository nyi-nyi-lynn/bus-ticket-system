package com.busticket.rmi;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.exception.UnauthorizedException;
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
    public PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException, RemoteException {
        return paymentService().processPayment(request);
    }
}
