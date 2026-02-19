package com.busticket.dao;

import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;

import java.util.List;

public interface BookingDAO {
    Long createBooking(Booking booking);

    void insertBookingSeats(Long bookingId, List<Long> seatIds);

    List<Long> findBookedSeats(Long tripId);

    boolean updateStatus(Long bookingId, BookingStatus status);

    int cancelExpiredPending(int minutes);
}
