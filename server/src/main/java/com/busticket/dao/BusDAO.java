package com.busticket.dao;

import com.busticket.exception.DuplicateResourceException;
import com.busticket.model.Bus;

import java.util.List;

public interface BusDAO {
    boolean existsByBusNumber(String busNumber);

    boolean existsByBusNumberExceptId(String busNumber, Long busId);

    Bus findById(Long busId);

    boolean hasBookedSeats(Long busId);

    boolean hasTrips(Long busId);

    Bus insert(Bus bus) throws DuplicateResourceException;

    Bus updateRecord(Bus bus) throws DuplicateResourceException;

    boolean deleteById(Long id);

    boolean save(Bus bus);

    boolean update(Bus bus);

    boolean deactivate(Long id);

    List<Bus> findAll();
}
