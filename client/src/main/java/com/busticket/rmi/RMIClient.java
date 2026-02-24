package com.busticket.rmi;

import com.busticket.remote.*;

import java.rmi.Naming;

public class RMIClient {
    public static UserRemote  getUserRemote() throws Exception {
        return (UserRemote) Naming.lookup("rmi://localhost/UserService");
    }

    public static RouteRemote getRouteRemote() throws Exception {
        return (RouteRemote) Naming.lookup("rmi://localhost/RouteService");
    }

    public static BusRemote getBusRemote() throws Exception {
        return (BusRemote) Naming.lookup("rmi://localhost/BusService");
    }

    public static TripRemote getTripRemote() throws Exception {
        return (TripRemote) Naming.lookup("rmi://localhost/TripService");
    }

    public static SeatRemote getSeatRemote() throws Exception { // ADDED
        return (SeatRemote) Naming.lookup("rmi://localhost/SeatService");
    }

    public static BookingRemote getBookingRemote() throws Exception{
        return (BookingRemote) Naming.lookup("rmi://localhost/BookingService");
    }

    public static PaymentRemote getPaymentRemote() throws Exception {
        return (PaymentRemote) Naming.lookup("rmi://localhost/PaymentService");
    }
}
