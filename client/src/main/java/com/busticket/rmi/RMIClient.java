package com.busticket.rmi;

import com.busticket.remote.BusTicketRemote;
import com.busticket.remote.RouteRemote;
import com.busticket.remote.UserRemote;

import java.rmi.Naming;

public class RMIClient {
    public static UserRemote getUserRemote() throws Exception {
        return (UserRemote) Naming.lookup("rmi://localhost/UserService");
    }

    public static BusTicketRemote getBusTicketRemote() throws Exception {
        return (BusTicketRemote) Naming.lookup("rmi://localhost/BusTicketService");
    }

    public static RouteRemote getRouteRemote() throws Exception {
        return (RouteRemote) Naming.lookup("rmi://localhost/RouteService");
    }
}
