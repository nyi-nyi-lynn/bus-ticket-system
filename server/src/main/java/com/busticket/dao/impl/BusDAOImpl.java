package com.busticket.dao.impl;

import com.busticket.dao.BusDAO;
import com.busticket.enums.BusType;
import com.busticket.model.Bus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BusDAOImpl implements BusDAO {
    private final Connection connection;

    public BusDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean save(Bus bus) {
        String sql = "INSERT INTO buses(bus_number, type, total_seats) VALUES(?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getType().name());
            ps.setInt(3, bus.getTotalSeats());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return false;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    generateSeats(rs.getLong(1), bus.getTotalSeats());
                }
            }

            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean update(Bus bus) {
        String sql = "UPDATE buses SET bus_number = ?, type = ?, total_seats = ? WHERE bus_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getType().name());
            ps.setInt(3, bus.getTotalSeats());
            ps.setLong(4, bus.getBusId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM buses WHERE bus_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Bus> findAll() {
        String sql = "SELECT bus_id, bus_number, type, total_seats FROM buses ORDER BY bus_id DESC";
        List<Bus> buses = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Bus bus = new Bus();
                bus.setBusId(rs.getLong("bus_id"));
                bus.setBusNumber(rs.getString("bus_number"));
                bus.setType(BusType.valueOf(rs.getString("type")));
                bus.setTotalSeats(rs.getInt("total_seats"));
                buses.add(bus);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return buses;
    }


    /**
     * Helper Methods
     */

    /**
     * Auto Generate seat with bus id
     * @param busId
     * @param totalSeats
     */
    private void generateSeats(Long busId, int totalSeats) {

        String sql = "INSERT INTO seats(bus_id, seat_number) VALUES(?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            for(int i = 1; i <= totalSeats; i++) {

                ps.setLong(1, busId);
                ps.setString(2, "S" + i);
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
