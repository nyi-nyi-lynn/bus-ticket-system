package com.busticket.service;

import com.busticket.dto.BookingDTO;
import com.busticket.exception.UnauthorizedException;

import java.util.List;

public interface BookingService {
    BookingDTO createBooking(BookingDTO dto) throws UnauthorizedException;
    boolean cancelBooking(Long bookingId, Long userId);
    List<BookingDTO> getBookingsByUserId(Long userId);
    List<String> getAvailableSeatNumbers(Long tripId);
    int cancelExpiredPending(int minutes);
}
