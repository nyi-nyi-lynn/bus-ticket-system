package com.busticket.rmi;

import com.busticket.remote.UserRemote;

import java.rmi.Naming;

public class RMIClient {
    public static UserRemote getUserRemote() throws Exception {
        return (UserRemote) Naming.lookup("rmi://localhost/UserService");
    }
}
