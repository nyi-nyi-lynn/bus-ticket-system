package com.busticket.remote;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO; // ADDED
import com.busticket.dto.BookingResponseDTO; // ADDED
import com.busticket.exception.UnauthorizedException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BookingRemote extends Remote {
    BookingDTO createBooking(BookingDTO dto) throws UnauthorizedException, RemoteException;
    BookingResponseDTO createBooking(BookingRequestDTO request) throws UnauthorizedException, RemoteException; // ADDED

    boolean confirmBooking(Long bookingId) throws RemoteException;
    boolean cancelBooking(Long bookingId, Long userId) throws RemoteException; // MODIFIED
    List<BookingDTO> getBookingsByUserId(Long userId) throws RemoteException; // ADDED

    List<Long> getBookedSeatIds(Long tripId) throws RemoteException;
    List<Long> findBookedSeatIdsByTrip(Long tripId) throws RemoteException; // ADDED
    List<String> getAvailableSeatNumbers(Long tripId) throws RemoteException; // ADDED
}
