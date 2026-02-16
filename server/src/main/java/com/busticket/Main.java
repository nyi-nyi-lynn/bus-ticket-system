package com.busticket;

import com.busticket.rmi.UserRemoteImpl;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Starting RMI Server...");

            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://localhost/UserService", new UserRemoteImpl());

            System.out.println("RMI Server is running on port 1099...");

        } catch (Exception e) {
            System.err.println("Server Exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
