package com.busticket.remote;

import com.busticket.dto.TicketDetailsDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicketRemote extends Remote {
    TicketDetailsDTO getTicketDetailsByBookingId(Long bookingId) throws RemoteException;
}
