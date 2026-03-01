package com.busticket.remote;

import com.busticket.dto.SeatDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface SeatRemote extends Remote {
    List<SeatDTO> getSeatsByBusId(Long busId) throws RemoteException;
}
