package com.busticket.dao;

import com.busticket.dto.RecentBookingDTO;
import com.busticket.dto.UpcomingTripDTO;
import com.busticket.model.Booking;

import java.util.List;
import java.util.Map;

public interface BookingDAO {
    Long save(Booking booking);

    List<Booking> findByUserId(Long userId);
    List<Long> findBookedSeatIdsByTrip(Long tripId);
    List<Long> findByTripAndSeatIds(Long tripId, List<Long> seatIds);
    Map<String, Long> countBookingsByStatus(Long userId);
    long countUpcomingTrips(Long userId);
    long countCompletedTrips(Long userId);
    UpcomingTripDTO findNextUpcomingTrip(Long userId);
    List<RecentBookingDTO> findRecentBookings(Long userId, int limit);

    boolean cancelBooking(Long bookingId, Long userId);
    int cancelExpiredPending(int minutes);
}
