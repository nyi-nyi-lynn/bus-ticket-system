package com.busticket.rmi;

import com.busticket.remote.PaymentRemote;
import com.busticket.service.PaymentService;
import com.busticket.service.impl.PaymentServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PaymentRemoteImpl extends UnicastRemoteObject implements PaymentRemote {
    private PaymentService paymentService;

    public PaymentRemoteImpl() throws RemoteException{
        paymentService = new PaymentServiceImpl();
    }
}
