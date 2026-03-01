package com.busticket.remote;

import com.busticket.dto.PaymentDTO;
import com.busticket.dto.PaymentRequestDTO;
import com.busticket.exception.UnauthorizedException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PaymentRemote extends Remote {
    PaymentDTO processPayment(PaymentRequestDTO request) throws UnauthorizedException, RemoteException;
}
