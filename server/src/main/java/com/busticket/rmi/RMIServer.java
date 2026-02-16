package com.busticket.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class RMIServer {
    public static void main(String[] args) throws Exception {

        LocateRegistry.createRegistry(1099);

        Naming.rebind("rmi://localhost/UserService", new UserRemoteImpl());

        System.out.println("RMI Server Running...");
    }
}
