package com.busticket.rmi;

import com.busticket.dto.TripDTO;
import com.busticket.remote.TripRemote;
import com.busticket.service.TripService;
import com.busticket.service.impl.TripServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.List;

public class TripRemoteImpl extends UnicastRemoteObject implements TripRemote {

    public TripRemoteImpl() throws RemoteException {
    }

    // FIXED
    private TripService tripService() {
        return new TripServiceImpl();
    }

    @Override
    public boolean saveTrip(TripDTO dto) throws RemoteException {
        return tripService().save(dto);
    }

    @Override
    public boolean updateTrip(TripDTO dto) throws RemoteException {
        return tripService().update(dto);
    }

    @Override
    public boolean deleteTrip(Long id) throws RemoteException {
        return tripService().delete(id);
    }

    @Override
    public List<TripDTO> getAllTrips() throws RemoteException {
        return tripService().getAll();
    }

    @Override
    public List<TripDTO> searchTrips(String origin, String destination, LocalDate date) throws RemoteException {
        return tripService().search(origin, destination, date);
    }
}
