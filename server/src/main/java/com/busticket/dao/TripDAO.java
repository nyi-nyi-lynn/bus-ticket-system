package com.busticket.dao;

import com.busticket.model.Trip;

import java.time.LocalDate;
import java.util.List;

public interface TripDAO {
    boolean save(Trip trip);

    boolean update(Trip trip);

    boolean delete(Long id);

    List<Trip> findAll();

    List<Trip> search(String origin, String destination, LocalDate date);
}
