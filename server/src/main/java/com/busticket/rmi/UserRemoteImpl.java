package com.busticket.rmi;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.service.UserService;
import com.busticket.service.impl.UserServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class UserRemoteImpl extends UnicastRemoteObject implements UserRemote {
    private final UserService userService;

    public  UserRemoteImpl() throws RemoteException{
        userService = new UserServiceImpl();
    }

    @Override
    public UserDTO login(String email, String password) throws RemoteException {
        return userService.login(email, password);
    }

    @Override
    public boolean register(UserDTO user) throws RemoteException {
        return userService.register(user);
    }

    @Override
    public UserDTO getUserById(Long userId) throws RemoteException {
        return userService.getUserById(userId);
    }

    @Override
    public boolean updateUser(UserDTO user) throws RemoteException {
        return userService.updateUser(user);
    }

    @Override
    public boolean deleteUser(Long userId) throws RemoteException {
        return userService.deleteUser(userId);
    }

    @Override
    public List<UserDTO> getAllUsers() throws RemoteException {
        return userService.getAllUsers();
    }
}
