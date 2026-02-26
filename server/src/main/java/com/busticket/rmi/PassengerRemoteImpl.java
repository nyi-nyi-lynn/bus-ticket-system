package com.busticket.rmi;

import com.busticket.dto.PassengerDashboardDTO;
import com.busticket.dto.PassengerProfileDTO;
import com.busticket.dto.PassengerProfileUpdateDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.PassengerRemote;
import com.busticket.service.PassengerDashboardService;
import com.busticket.service.PassengerProfileService;
import com.busticket.service.impl.PassengerDashboardServiceImpl;
import com.busticket.service.impl.PassengerProfileServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PassengerRemoteImpl extends UnicastRemoteObject implements PassengerRemote {

    public PassengerRemoteImpl() throws RemoteException {
    }

    private PassengerDashboardService dashboardService() {
        return new PassengerDashboardServiceImpl();
    }

    private PassengerProfileService profileService() {
        return new PassengerProfileServiceImpl();
    }

    @Override
    public PassengerDashboardDTO getDashboard(Long userId)
            throws ValidationException, UnauthorizedException, RemoteException {
        return dashboardService().getDashboard(userId);
    }

    @Override
    public PassengerProfileDTO getProfile(Long userId)
            throws ValidationException, UnauthorizedException, RemoteException {
        return profileService().getProfile(userId);
    }

    @Override
    public void updateProfile(PassengerProfileUpdateDTO dto)
            throws ValidationException, UnauthorizedException, RemoteException {
        profileService().updateProfile(dto);
    }
}
