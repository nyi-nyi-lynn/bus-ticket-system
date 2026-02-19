package com.busticket.remote;

import com.busticket.dto.RouteDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RouteRemote extends Remote {

    boolean saveRoute(RouteDTO dto) throws RemoteException;

    boolean updateRoute(RouteDTO dto) throws RemoteException;

    boolean deactivateRoute(Long id) throws RemoteException;

    List<RouteDTO> getAllRoutes() throws RemoteException;
}
