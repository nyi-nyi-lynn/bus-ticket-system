package com.busticket.dao;

import com.busticket.dto.SeatDTO;

import java.util.List;

public interface SeatDAO {
    // ADDED
    List<Long> findBookedSeatIdsByTrip(Long tripId);
    List<String> findAvailableSeatNumbersByTrip(Long tripId); // ADDED
    List<SeatDTO> findByBusId(Long busId); // ADDED
}
