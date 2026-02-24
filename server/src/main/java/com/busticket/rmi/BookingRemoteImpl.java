package com.busticket.rmi;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO; // ADDED
import com.busticket.dto.BookingResponseDTO; // ADDED
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
    public BookingDTO createBooking(BookingDTO dto) throws RemoteException {
        return bookingService().createBooking(dto);
    }

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO request) throws RemoteException {
        // ADDED
        return bookingService().createBooking(request);
    }

    @Override
    public boolean confirmBooking(Long bookingId) throws RemoteException {
        return bookingService().confirmBooking(bookingId);
    }

    @Override
    public boolean cancelBooking(Long bookingId, Long userId) throws RemoteException {
        // MODIFIED
        return bookingService().cancelBooking(bookingId, userId);
    }

    @Override
    public List<BookingDTO> getBookingsByUserId(Long userId) throws RemoteException {
        // ADDED
        return bookingService().getBookingsByUserId(userId);
    }

    @Override
    public List<Long> getBookedSeatIds(Long tripId) throws RemoteException {
        return bookingService().getBookedSeatIds(tripId);
    }

    @Override
    public List<Long> findBookedSeatIdsByTrip(Long tripId) throws RemoteException {
        // ADDED
        return bookingService().findBookedSeatIdsByTrip(tripId);
    }

    @Override
    public List<String> getAvailableSeatNumbers(Long tripId) throws RemoteException {
        // ADDED
        return bookingService().getAvailableSeatNumbers(tripId);
    }
}
