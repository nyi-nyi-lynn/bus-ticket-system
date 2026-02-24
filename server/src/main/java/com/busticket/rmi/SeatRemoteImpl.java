package com.busticket.rmi;

import com.busticket.dto.SeatDTO;
import com.busticket.remote.SeatRemote;
import com.busticket.service.SeatService;
import com.busticket.service.impl.SeatServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class SeatRemoteImpl extends UnicastRemoteObject implements SeatRemote {
    public SeatRemoteImpl() throws RemoteException {
    }

    // ADDED
    private SeatService seatService() {
        return new SeatServiceImpl();
    }

    @Override
    public List<SeatDTO> getSeatsByBusId(Long busId) throws RemoteException {
        return seatService().getSeatsByBusId(busId);
    }
}
