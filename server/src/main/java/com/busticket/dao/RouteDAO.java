package com.busticket.dao;

import com.busticket.model.Route;

import java.util.List;

public interface RouteDAO {
    boolean save(Route route);

    boolean update(Route route);

    boolean deactivate(Long id);

    List<Route> findAll();
}
