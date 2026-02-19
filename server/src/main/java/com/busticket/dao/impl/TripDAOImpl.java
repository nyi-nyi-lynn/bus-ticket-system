package com.busticket.dao.impl;

import com.busticket.dao.TripDAO;
import com.busticket.enums.TripStatus;
import com.busticket.model.Trip;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripDAOImpl implements TripDAO {
    private final Connection connection;

    public TripDAOImpl(Connection connection) {
        this.connection = connection;
    }


    @Override
    public boolean save(Trip trip) {
        String sql = """
            INSERT INTO trips(bus_id, route_id, travel_date, departure_time, arrival_time, price, status)
            VALUES(?,?,?,?,?,?,?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trip.getBusId());
            ps.setLong(2, trip.getRouteId());
            ps.setDate(3, Date.valueOf(trip.getTravelDate()));
            ps.setTime(4, Time.valueOf(trip.getDepartureTime()));
            ps.setTime(5, Time.valueOf(trip.getArrivalTime()));
            ps.setDouble(6, trip.getPrice());
            ps.setString(7, trip.getStatus() == null ? TripStatus.OPEN.name() : trip.getStatus().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Trip trip) {
        String sql = """
            UPDATE trips
            SET bus_id = ?, route_id = ?, travel_date = ?, departure_time = ?, arrival_time = ?, price = ?, status = ?
            WHERE trip_id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, trip.getBusId());
            ps.setLong(2, trip.getRouteId());
            ps.setDate(3, Date.valueOf(trip.getTravelDate()));
            ps.setTime(4, Time.valueOf(trip.getDepartureTime()));
            ps.setTime(5, Time.valueOf(trip.getArrivalTime()));
            ps.setDouble(6, trip.getPrice());
            ps.setString(7, trip.getStatus() == null ? TripStatus.OPEN.name() : trip.getStatus().name());
            ps.setLong(8, trip.getTripId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE trips SET status = ? WHERE trip_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, TripStatus.CLOSED.name());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Trip> findAll() {
        String sql = "SELECT * FROM trips ORDER BY trip_id DESC";
        List<Trip> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Trip trip = new Trip();
                trip.setTripId(rs.getLong("trip_id"));
                trip.setBusId(rs.getLong("bus_id"));
                trip.setRouteId(rs.getLong("route_id"));
                trip.setTravelDate(rs.getDate("travel_date").toLocalDate());
                trip.setDepartureTime(rs.getTime("departure_time").toLocalTime());
                trip.setArrivalTime(rs.getTime("arrival_time").toLocalTime());
                trip.setPrice(rs.getDouble("price"));
                trip.setStatus(TripStatus.valueOf(rs.getString("status")));
                list.add(trip);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Trip> search(String origin, String destination, LocalDate date) {
        String sql = """
        SELECT t.* FROM trips t
        JOIN routes r ON t.route_id = r.route_id
        WHERE r.origin_city = ?
        AND r.destination_city = ?
        AND t.travel_date = ?
        AND t.status = 'OPEN'
    """;

        List<Trip> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, origin);
            ps.setString(2, destination);
            ps.setDate(3, Date.valueOf(date));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Trip trip = new Trip();
                trip.setTripId(rs.getLong("trip_id"));
                trip.setBusId(rs.getLong("bus_id"));
                trip.setRouteId(rs.getLong("route_id"));
                trip.setTravelDate(rs.getDate("travel_date").toLocalDate());
                trip.setDepartureTime(rs.getTime("departure_time").toLocalTime());
                trip.setArrivalTime(rs.getTime("arrival_time").toLocalTime());
                trip.setPrice(rs.getDouble("price"));
                trip.setStatus(TripStatus.valueOf(rs.getString("status")));
                list.add(trip);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}
