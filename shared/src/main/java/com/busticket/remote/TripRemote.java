package com.busticket.remote;

import com.busticket.dto.TripDTO;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface TripRemote {
    boolean saveTrip(TripDTO dto) throws RemoteException;

    boolean updateTrip(TripDTO dto) throws RemoteException;

    boolean deleteTrip(Long id) throws RemoteException;

    List<TripDTO> getAllTrips() throws RemoteException;

    List<TripDTO> searchTrips(String origin, String destination, LocalDate date)
            throws RemoteException;
}
