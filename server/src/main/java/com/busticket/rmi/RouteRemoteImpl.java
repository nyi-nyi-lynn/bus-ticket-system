package com.busticket.rmi;

import com.busticket.dto.RouteDTO;
import com.busticket.remote.RouteRemote;
import com.busticket.service.RouteService;
import com.busticket.service.impl.RouteServieImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RouteRemoteImpl extends UnicastRemoteObject implements RouteRemote {
    private RouteService routeService ;

   public RouteRemoteImpl() throws RemoteException{
       routeService = new RouteServieImpl();
   }

    @Override
    public boolean saveRoute(RouteDTO dto) throws RemoteException {
        return routeService.save(dto);
    }

    @Override
    public boolean updateRoute(RouteDTO dto) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteRoute(Long id) throws RemoteException {
        return false;
    }

    @Override
    public List<RouteDTO> getAllRoutes() throws RemoteException {
        return List.of();
    }
}
