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
        String sql = "UPDATE routes SET origin_city = ?, destination_city = ?, distance_km = ?, estimated_duration = ? WHERE route_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, route.getOriginCity());
            ps.setString(2, route.getDestinationCity());
            ps.setDouble(3, route.getDistanceKm());
            ps.setString(4, route.getEstimatedDuration());
            ps.setLong(5, route.getRouteId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM routes WHERE route_id = ?";
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
        String sql = "SELECT route_id, origin_city, destination_city, distance_km, estimated_duration FROM routes ORDER BY route_id DESC";
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
                routes.add(route);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return routes;
    }
}
