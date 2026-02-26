package com.busticket.service.impl;

import com.busticket.dao.RouteDAO;
import com.busticket.dao.impl.RouteDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.RouteDTO;
import com.busticket.model.Route;
import com.busticket.service.RouteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteServiceImpl implements RouteService {

    private final RouteDAO routeDAO;

    public RouteServiceImpl() {
        this.routeDAO = new RouteDAOImpl(DatabaseConnection.getConnection());
    }


    @Override
    public boolean save(RouteDTO dto) {
        if (!isValid(dto, false)) {
            return false;
        }
        Route route = toModel(dto);
        if (route.getEstimatedDuration() == null || route.getEstimatedDuration().isBlank()) {
            route.setEstimatedDuration("N/A");
        }
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            route.setActive(true);
        }
        return routeDAO.save(route);
    }

    @Override
    public boolean update(RouteDTO dto) {
        if (!isValid(dto, true)) {
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
        route.setOriginCity(dto.getOriginCity() == null ? null : dto.getOriginCity().trim());
        route.setDestinationCity(dto.getDestinationCity() == null ? null : dto.getDestinationCity().trim());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDuration(dto.getEstimatedDuration() == null ? null : dto.getEstimatedDuration().trim());
        route.setActive(parseActive(dto.getStatus()));
        return route;
    }

    private RouteDTO toDTO(Route route) {
        RouteDTO dto = new RouteDTO();
        dto.setRouteId(route.getRouteId());
        dto.setOriginCity(route.getOriginCity());
        dto.setDestinationCity(route.getDestinationCity());
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDuration(route.getEstimatedDuration());
        dto.setStatus(route.isActive() ? "ACTIVE" : "INACTIVE");
        return dto;
    }

    private boolean isValid(RouteDTO dto, boolean requireId) {
        if (dto == null) {
            return false;
        }
        if (requireId && dto.getRouteId() == null) {
            return false;
        }
        String origin = dto.getOriginCity() == null ? "" : dto.getOriginCity().trim();
        String destination = dto.getDestinationCity() == null ? "" : dto.getDestinationCity().trim();
        String duration = dto.getEstimatedDuration() == null ? "" : dto.getEstimatedDuration().trim();

        if (origin.isBlank() || destination.isBlank() || duration.isBlank()) {
            return false;
        }
        if (origin.equalsIgnoreCase(destination)) {
            return false;
        }
        if (dto.getDistanceKm() < 0) {
            return false;
        }
        String status = dto.getStatus() == null ? "ACTIVE" : dto.getStatus().trim().toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(status) || "INACTIVE".equals(status) || "1".equals(status) || "0".equals(status);
    }

    private boolean parseActive(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(normalized) || "1".equals(normalized);
    }
}
