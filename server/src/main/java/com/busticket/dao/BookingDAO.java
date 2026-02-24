package com.busticket.dao;

import com.busticket.enums.BookingStatus;
import com.busticket.model.Booking;

import java.util.List;

public interface BookingDAO {
    Long createBooking(Booking booking);
    Long save(Booking booking); // ADDED

    void insertBookingSeats(Long bookingId, List<Long> seatIds);

    List<Booking> findByUserId(Long userId); // ADDED
    List<Long> findBookedSeats(Long tripId);
    List<Long> findBookedSeatIdsByTrip(Long tripId); // ADDED
    List<String> findAvailableSeatNumbers(Long tripId); // ADDED
    List<Long> findByTripAndSeatIds(Long tripId, List<Long> seatIds); // ADDED

    boolean updateStatus(Long bookingId, BookingStatus status);
    boolean cancelBooking(Long bookingId, Long userId); // MODIFIED

    int cancelExpiredPending(int minutes);
}
