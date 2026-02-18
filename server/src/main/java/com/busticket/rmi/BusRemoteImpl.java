package com.busticket.rmi;

import com.busticket.dto.BusDTO;
import com.busticket.remote.BusRemote;
import com.busticket.service.BusService;
import com.busticket.service.impl.BusServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BusRemoteImpl extends UnicastRemoteObject implements BusRemote {
    private BusService busService;

    public BusRemoteImpl() throws RemoteException {
        busService = new BusServiceImpl();
    }

    @Override
    public boolean saveBus(BusDTO dto) throws RemoteException {
        return busService.save(dto);
    }

    @Override
    public boolean updateBus(BusDTO dto) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteBus(Long id) throws RemoteException {
        return false;
    }

    @Override
    public List<BusDTO> getAllBuses() throws RemoteException {
        return List.of();
    }
}
