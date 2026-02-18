package com.busticket.dao;

import com.busticket.model.Bus;

import java.util.List;

public interface BusDAO {
    boolean save(Bus bus);

    boolean update(Bus bus);

    boolean delete(Long id);

    List<Bus> findAll();
}
