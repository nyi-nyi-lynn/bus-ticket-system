package com.busticket.rmi;

import com.busticket.dto.TicketDetailsDTO;
import com.busticket.remote.TicketRemote;
import com.busticket.service.TicketService;
import com.busticket.service.impl.TicketServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class TicketRemoteImpl extends UnicastRemoteObject implements TicketRemote {

    public TicketRemoteImpl() throws RemoteException {
    }

    private TicketService ticketService() {
        return new TicketServiceImpl();
    }

    @Override
    public TicketDetailsDTO getTicketDetailsByBookingId(Long bookingId) throws RemoteException {
        return ticketService().getTicketDetailsByBookingId(bookingId);
    }
}
