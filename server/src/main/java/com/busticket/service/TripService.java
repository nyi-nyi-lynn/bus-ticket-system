package com.busticket.service;

import com.busticket.dto.TripDTO;

import java.time.LocalDate;
import java.util.List;

public interface TripService {
    boolean save(TripDTO dto);

    boolean update(TripDTO dto);

    boolean delete(Long id);

    List<TripDTO> getAll();

    List<TripDTO> search(String origin, String destination, LocalDate date);
}
