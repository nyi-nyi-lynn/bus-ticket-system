package com.busticket.service;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO; // ADDED
import com.busticket.dto.BookingResponseDTO; // ADDED
import com.busticket.exception.UnauthorizedException;

import java.util.List;

public interface BookingService {
    BookingDTO createBooking(BookingDTO dto) throws UnauthorizedException;
    BookingResponseDTO createBooking(BookingRequestDTO request) throws UnauthorizedException; // ADDED

    boolean confirmBooking(Long bookingId);
    boolean cancelBooking(Long bookingId, Long userId); // MODIFIED
    List<BookingDTO> getBookingsByUserId(Long userId); // ADDED

    List<Long> getBookedSeatIds(Long tripId);
    List<Long> findBookedSeatIdsByTrip(Long tripId); // ADDED
    List<String> getAvailableSeatNumbers(Long tripId); // ADDED

    int cancelExpiredPending(int minutes);
}
