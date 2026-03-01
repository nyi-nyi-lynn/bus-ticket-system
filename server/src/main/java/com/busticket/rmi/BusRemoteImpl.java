package com.busticket.rmi;

import com.busticket.dto.BusDTO;
import com.busticket.dto.CreateBusRequest;
import com.busticket.dto.UpdateBusRequest;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.BusRemote;
import com.busticket.service.BusService;
import com.busticket.service.impl.BusServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BusRemoteImpl extends UnicastRemoteObject implements BusRemote {
    public BusRemoteImpl() throws RemoteException {
    }

    // FIXED
    private BusService busService() {
        return new BusServiceImpl();
    }

    @Override
    public List<BusDTO> getAllBuses() throws RemoteException {
        return busService().getAllBuses();
    }

    @Override
    public BusDTO createBus(CreateBusRequest request)
            throws DuplicateResourceException, ValidationException, RemoteException {
        return busService().createBus(request);
    }

    @Override
    public BusDTO updateBus(UpdateBusRequest request)
            throws DuplicateResourceException, ValidationException, RemoteException {
        return busService().updateBus(request);
    }

    @Override
    public void deleteBus(Long busId) throws ValidationException, RemoteException {
        busService().deleteBus(busId);
    }
}
