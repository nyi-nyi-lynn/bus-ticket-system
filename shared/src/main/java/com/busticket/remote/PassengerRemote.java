package com.busticket.remote;

import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PassengerRemote extends Remote {
    PassengerDashboardDTO getDashboard(Long userId)
            throws ValidationException, UnauthorizedException, RemoteException;
}
