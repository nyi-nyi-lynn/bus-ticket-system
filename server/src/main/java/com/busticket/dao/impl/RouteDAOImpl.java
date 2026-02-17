package com.busticket.dao.impl;

import com.busticket.dao.RouteDAO;
import com.busticket.model.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class RouteDAOImpl implements RouteDAO {

    private final Connection connection;

    public RouteDAOImpl(Connection connection) {
        this.connection = connection;
    }
    @Override
    public boolean save(Route route) {
        String sql = "INSERT INTO routes(origin_city, destination_city, distance_km, estimated_duration) VALUES (?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, route.getOriginCity());
            ps.setString(2, route.getDestinationCity());
            ps.setDouble(3, route.getDistanceKm());
            ps.setString(4, route.getEstimatedDuration());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean update(Route route) {
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return false;
    }

    @Override
    public List<Route> findAll() {
        return List.of();
    }
}
