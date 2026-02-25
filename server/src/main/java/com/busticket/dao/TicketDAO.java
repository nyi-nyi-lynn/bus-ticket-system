package com.busticket.dao;

import com.busticket.dto.TicketDetailsDTO;

public interface TicketDAO {
    TicketDetailsDTO findTicketDetailsByBookingId(Long bookingId);
}
