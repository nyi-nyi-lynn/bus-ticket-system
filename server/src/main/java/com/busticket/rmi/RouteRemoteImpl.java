package com.busticket.rmi;

import com.busticket.dto.RouteDTO;
import com.busticket.remote.RouteRemote;
import com.busticket.service.RouteService;
import com.busticket.service.impl.RouteServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RouteRemoteImpl extends UnicastRemoteObject implements RouteRemote {
    private final RouteService routeService ;

   public RouteRemoteImpl() throws RemoteException{
       routeService = new RouteServiceImpl();
   }

    @Override
    public boolean saveRoute(RouteDTO dto) throws RemoteException {
        return routeService.save(dto);
    }

    @Override
    public boolean updateRoute(RouteDTO dto) throws RemoteException {
        return routeService.update(dto);
    }

    @Override
    public boolean deleteRoute(Long id) throws RemoteException {
        return routeService.delete(id);
    }

    @Override
    public List<RouteDTO> getAllRoutes() throws RemoteException {
        return routeService.getAll();
    }
}
