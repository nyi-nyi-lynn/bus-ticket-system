package com.busticket.rmi;

import com.busticket.dto.BusDTO;
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
    public boolean saveBus(BusDTO dto) throws RemoteException {
        return busService().save(dto);
    }

    @Override
    public boolean updateBus(BusDTO dto) throws RemoteException {
        return busService().update(dto);
    }

    @Override
    public boolean deactivateBus(Long id) throws RemoteException {
        return busService().deactivate(id);
    }

    @Override
    public List<BusDTO> getAllBuses() throws RemoteException {
        return busService().getAll();
    }
}
