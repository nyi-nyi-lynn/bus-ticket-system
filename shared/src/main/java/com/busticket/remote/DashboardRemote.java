package com.busticket.remote;

import com.busticket.dto.DashboardResponseDTO;
import com.busticket.exception.ValidationException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DashboardRemote extends Remote {
    DashboardResponseDTO getDashboardSummary(Long requestedByUserId)
            throws ValidationException, RemoteException;
}
