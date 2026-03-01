package com.busticket.remote;

import com.busticket.dto.BusDTO;
import com.busticket.dto.CreateBusRequest;
import com.busticket.dto.UpdateBusRequest;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BusRemote extends Remote {
    List<BusDTO> getAllBuses() throws RemoteException;

    BusDTO createBus(CreateBusRequest request)
            throws DuplicateResourceException, ValidationException, RemoteException;

    BusDTO updateBus(UpdateBusRequest request)
            throws DuplicateResourceException, ValidationException, RemoteException;

    void deleteBus(Long busId) throws ValidationException, RemoteException;
}
