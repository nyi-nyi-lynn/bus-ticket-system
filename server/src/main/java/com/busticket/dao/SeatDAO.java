package com.busticket.dao;

import com.busticket.dto.SeatDTO;

import java.util.List;

public interface SeatDAO {
    List<String> findAvailableSeatNumbersByTrip(Long tripId);
    List<SeatDTO> findByBusId(Long busId);
}
