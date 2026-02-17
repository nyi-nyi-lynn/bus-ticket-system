package com.busticket.service.impl;

import com.busticket.dao.RouteDAO;
import com.busticket.dto.RouteDTO;
import com.busticket.mapper.RouteMapper;
import com.busticket.model.Route;
import com.busticket.service.RouteService;

import java.util.List;

public class RouteServieImpl implements RouteService {

    private final RouteDAO routeDAO;

    public  RouteServieImpl(RouteDAO routeDAO) {
        this.routeDAO = routeDAO;
    }

    @Override
    public boolean save(RouteDTO dto) {
        Route route = RouteMapper.toModel(dto);
        return routeDAO.save(route);
    }

    @Override
    public boolean update(RouteDTO dto) {
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return false;
    }

    @Override
    public List<RouteDTO> getAll() {
        return List.of();
    }
}
