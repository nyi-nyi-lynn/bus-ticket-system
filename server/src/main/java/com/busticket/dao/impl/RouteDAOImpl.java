package com.busticket.dao.impl;

import com.busticket.dao.RouteDAO;
import com.busticket.model.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RouteDAOImpl implements RouteDAO {

    private final Connection connection;

    public RouteDAOImpl(Connection connection) {
        this.connection = connection;
    }
    @Override
    public boolean save(Route route) {
        String sql = "INSERT INTO routes(origin_city, destination_city, distance_km, estimated_duration, is_active) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, route.getOriginCity());
            ps.setString(2, route.getDestinationCity());
            ps.setDouble(3, route.getDistanceKm());
            ps.setString(4, route.getEstimatedDuration());
            ps.setInt(5, route.isActive() ? 1 : 0);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean update(Route route) {
        String sql = "UPDATE routes SET origin_city = ?, destination_city = ?, distance_km = ?, estimated_duration = ?, is_active = ? WHERE route_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, route.getOriginCity());
            ps.setString(2, route.getDestinationCity());
            ps.setDouble(3, route.getDistanceKm());
            ps.setString(4, route.getEstimatedDuration());
            ps.setInt(5, route.isActive() ? 1 : 0);
            ps.setLong(6, route.getRouteId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deactivate(Long id) {
        String sql = "UPDATE routes SET is_active = 0 WHERE route_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Route> findAll() {
        String sql = "SELECT route_id, origin_city, destination_city, distance_km, estimated_duration, is_active FROM routes ORDER BY route_id DESC";
        List<Route> routes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Route route = new Route();
                route.setRouteId(rs.getLong("route_id"));
                route.setOriginCity(rs.getString("origin_city"));
                route.setDestinationCity(rs.getString("destination_city"));
                route.setDistanceKm(rs.getDouble("distance_km"));
                route.setEstimatedDuration(rs.getString("estimated_duration"));
                route.setActive(rs.getInt("is_active") == 1);
                routes.add(route);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return routes;
    }
}
