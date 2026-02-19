package com.busticket.service;

import com.busticket.dto.BookingDTO;

import java.util.List;

public interface BookingService {
    BookingDTO createBooking(BookingDTO dto);

    boolean confirmBooking(Long bookingId);

    List<Long> getBookedSeatIds(Long tripId);
}
