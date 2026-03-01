package com.busticket.service;

import com.busticket.dto.SeatDTO;

import java.util.List;

public interface SeatService {
    List<SeatDTO> getSeatsByBusId(Long busId);
}
