package com.busticket.remote;

import com.busticket.dto.BusDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BusRemote extends Remote {
    boolean saveBus(BusDTO dto) throws RemoteException;

    boolean updateBus(BusDTO dto) throws RemoteException;

    boolean deactivateBus(Long id) throws RemoteException;

    List<BusDTO> getAllBuses() throws RemoteException;
}
