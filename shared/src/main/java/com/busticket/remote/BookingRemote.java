package com.busticket.remote;

import com.busticket.dto.BookingDTO;
import com.busticket.exception.UnauthorizedException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BookingRemote extends Remote {
    BookingDTO createBooking(BookingDTO dto) throws UnauthorizedException, RemoteException;
    boolean cancelBooking(Long bookingId, Long userId) throws RemoteException;
    List<BookingDTO> getBookingsByUserId(Long userId) throws RemoteException;

    List<String> getAvailableSeatNumbers(Long tripId) throws RemoteException;
}
