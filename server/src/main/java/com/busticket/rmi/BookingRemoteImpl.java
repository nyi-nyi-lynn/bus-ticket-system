package com.busticket.rmi;

import com.busticket.dto.BookingDTO;
import com.busticket.remote.BookingRemote;
import com.busticket.service.BookingService;
import com.busticket.service.impl.BookingServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BookingRemoteImpl extends UnicastRemoteObject implements BookingRemote {

    private final BookingService bookingService;

    public BookingRemoteImpl() throws RemoteException {
        bookingService = new BookingServiceImpl();
    }

    @Override
    public BookingDTO createBooking(BookingDTO dto) throws RemoteException {
        return null;
    }

    @Override
    public boolean confirmBooking(Long bookingId) throws RemoteException {
        return false;
    }

    @Override
    public List<Long> getBookedSeatIds(Long tripId) throws RemoteException {
        return List.of();
    }
}
