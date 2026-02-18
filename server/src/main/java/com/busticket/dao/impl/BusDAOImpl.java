package com.busticket.dao.impl;

import com.busticket.dao.BusDAO;
import com.busticket.model.Bus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class BusDAOImpl implements BusDAO {
    private final Connection connection;

    public BusDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean save(Bus bus) {
        String sql = "INSERT INTO buses(bus_number, type, total_seats) VALUES(?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, bus.getBusNumber());
            ps.setString(2, bus.getType().name());
            ps.setInt(3, bus.getTotalSeats());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean update(Bus bus) {
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return false;
    }

    @Override
    public List<Bus> findAll() {
        return List.of();
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
