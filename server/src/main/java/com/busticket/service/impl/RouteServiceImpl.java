package com.busticket.service.impl;

import com.busticket.dao.RouteDAO;
import com.busticket.dao.impl.RouteDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.RouteDTO;
import com.busticket.model.Route;
import com.busticket.service.RouteService;

import java.util.ArrayList;
import java.util.List;

public class RouteServiceImpl implements RouteService {

    private final RouteDAO routeDAO;

    public RouteServiceImpl() {
        this.routeDAO = new RouteDAOImpl(DatabaseConnection.getConnection());
    }


    @Override
    public boolean save(RouteDTO dto) {
        // Validate required fields.
        return routeDAO.save(toModel(dto));
    }

    @Override
    public boolean update(RouteDTO dto) {
        // Validate required fields and identifiers.
        if (dto == null || dto.getRouteId() == null) {
            return false;
        }
        return routeDAO.update(toModel(dto));
    }

    @Override
    public boolean deactivate(Long id) {
        // Soft delete: mark route inactive.
        return routeDAO.deactivate(id);
    }

    @Override
    public List<RouteDTO> getAll() {
        List<Route> routes = routeDAO.findAll();
        List<RouteDTO> dtos = new ArrayList<>();
        for (Route route : routes) {
            dtos.add(toDTO(route));
        }
        return dtos;
    }

    private Route toModel(RouteDTO dto) {
        Route route = new Route();
        route.setRouteId(dto.getRouteId());
        route.setOriginCity(dto.getOriginCity());
        route.setDestinationCity(dto.getDestinationCity());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDuration(dto.getEstimatedDuration());
        return route;
    }

    private RouteDTO toDTO(Route route) {
        RouteDTO dto = new RouteDTO();
        dto.setRouteId(route.getRouteId());
        dto.setOriginCity(route.getOriginCity());
        dto.setDestinationCity(route.getDestinationCity());
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDuration(route.getEstimatedDuration());
        return dto;
    }
}
