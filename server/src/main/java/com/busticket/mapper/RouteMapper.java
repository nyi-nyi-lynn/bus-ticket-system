package com.busticket.mapper;

import com.busticket.dto.RouteDTO;
import com.busticket.dto.UserDTO;
import com.busticket.model.Route;
import com.busticket.model.User;

public class RouteMapper {

    public static Route toModel(RouteDTO dto) {
        Route route = new Route();
        route.setRouteId(dto.getRouteId());
        route.setOriginCity(dto.getOriginCity());
        route.setDestinationCity(dto.getDestinationCity());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDuration(dto.getEstimatedDuration());
        return route;
    }
}
