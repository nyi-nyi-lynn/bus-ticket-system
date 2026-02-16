package com.busticket.remote;

import com.busticket.dto.UserDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UserRemote extends Remote {
    UserDTO login(String email,String password) throws RemoteException;

    boolean register(UserDTO user) throws RemoteException;

    List<UserDTO> getAllUsers() throws RemoteException;
}
