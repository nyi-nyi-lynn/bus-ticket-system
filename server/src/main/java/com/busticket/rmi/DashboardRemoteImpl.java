package com.busticket.rmi;

import com.busticket.dto.DashboardResponseDTO;
import com.busticket.exception.ValidationException;
import com.busticket.remote.DashboardRemote;
import com.busticket.service.DashboardService;
import com.busticket.service.impl.DashboardServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class DashboardRemoteImpl extends UnicastRemoteObject implements DashboardRemote {
    public DashboardRemoteImpl() throws RemoteException {
    }

    private DashboardService dashboardService() {
        return new DashboardServiceImpl();
    }

    @Override
    public DashboardResponseDTO getDashboardSummary(Long requestedByUserId)
            throws ValidationException, RemoteException {
        return dashboardService().getDashboardSummary(requestedByUserId);
    }
}
