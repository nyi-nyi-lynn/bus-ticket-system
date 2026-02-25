package com.busticket.service;

import com.busticket.dto.TicketDetailsDTO;

public interface TicketService {
    TicketDetailsDTO getTicketDetailsByBookingId(Long bookingId);
}
