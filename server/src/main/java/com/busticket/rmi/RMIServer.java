package com.busticket.rmi;

import com.busticket.service.impl.BookingExpiryJob;
import com.busticket.service.impl.BookingServiceImpl;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class RMIServer {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099);
        Naming.rebind("rmi://localhost/UserService", new UserRemoteImpl());
        Naming.rebind("rmi://localhost/RouteService", new RouteRemoteImpl());
        Naming.rebind("rmi://localhost/BusService", new BusRemoteImpl());
        Naming.rebind("rmi://localhost/TripService", new TripRemoteImpl());
        Naming.rebind("rmi://localhost/SeatService", new SeatRemoteImpl()); // ADDED
        Naming.rebind("rmi://localhost/BookingService", new BookingRemoteImpl());
        Naming.rebind("rmi://localhost/PaymentService", new PaymentRemoteImpl());
        Naming.rebind("rmi://localhost/TicketService", new TicketRemoteImpl());
        Naming.rebind("rmi://localhost/ReportService", new ReportRemoteImpl());

        new BookingExpiryJob(new BookingServiceImpl(), 20).start();

        System.out.println("RMI Server Running on port 1099...");
    }
}
