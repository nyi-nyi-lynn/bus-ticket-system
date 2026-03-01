package com.busticket.rmi;

import com.busticket.dto.BookingDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.remote.BookingRemote;
import com.busticket.service.BookingService;
import com.busticket.service.impl.BookingServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BookingRemoteImpl extends UnicastRemoteObject implements BookingRemote {

    public BookingRemoteImpl() throws RemoteException {
    }

    // FIXED
    private BookingService bookingService() {
        return new BookingServiceImpl();
    }

    @Override
    public BookingDTO createBooking(BookingDTO dto) throws UnauthorizedException, RemoteException {
        return bookingService().createBooking(dto);
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long userId) throws RemoteException {
        return bookingService().cancelBooking(bookingId, userId);
    }

    @Override
    public List<BookingDTO> getBookingsByUserId(Long userId) throws RemoteException {
        return bookingService().getBookingsByUserId(userId);
    }

    @Override
    public List<String> getAvailableSeatNumbers(Long tripId) throws RemoteException {
        return bookingService().getAvailableSeatNumbers(tripId);
    }
}
