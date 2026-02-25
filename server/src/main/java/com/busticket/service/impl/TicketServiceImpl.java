package com.busticket.service.impl;

import com.busticket.dao.TicketDAO;
import com.busticket.dao.impl.TicketDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.TicketDetailsDTO;
import com.busticket.service.TicketService;

import java.sql.Connection;

public class TicketServiceImpl implements TicketService {

    private final TicketDAO ticketDAO;

    public TicketServiceImpl() {
        Connection connection = DatabaseConnection.getConnection();
        ticketDAO = new TicketDAOImpl(connection);
    }

    @Override
    public TicketDetailsDTO getTicketDetailsByBookingId(Long bookingId) {
        return ticketDAO.findTicketDetailsByBookingId(bookingId);
    }
}
