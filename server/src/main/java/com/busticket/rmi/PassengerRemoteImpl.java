package com.busticket.rmi;

import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.PassengerRemote;
import com.busticket.service.PassengerDashboardService;
import com.busticket.service.impl.PassengerDashboardServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PassengerRemoteImpl extends UnicastRemoteObject implements PassengerRemote {

    public PassengerRemoteImpl() throws RemoteException {
    }

    private PassengerDashboardService dashboardService() {
        return new PassengerDashboardServiceImpl();
    }

    @Override
    public PassengerDashboardDTO getDashboard(Long userId)
            throws ValidationException, UnauthorizedException, RemoteException {
        return dashboardService().getDashboard(userId);
    }
}
