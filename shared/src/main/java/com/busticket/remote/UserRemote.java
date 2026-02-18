package com.busticket.remote;

import com.busticket.dto.UserDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UserRemote extends Remote {
    UserDTO login(String email,String password) throws RemoteException;

    boolean register(UserDTO user) throws RemoteException;

    UserDTO getUserById(Long userId) throws RemoteException;

    boolean updateUser(UserDTO user) throws RemoteException;

    boolean deleteUser(Long userId) throws RemoteException;

    List<UserDTO> getAllUsers() throws RemoteException;
}
