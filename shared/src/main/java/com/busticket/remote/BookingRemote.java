package com.busticket.remote;

import com.busticket.dto.BookingDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BookingRemote extends Remote {
    BookingDTO createBooking(BookingDTO dto) throws RemoteException;

    boolean confirmBooking(Long bookingId) throws RemoteException;

    List<Long> getBookedSeatIds(Long tripId) throws RemoteException;
}
