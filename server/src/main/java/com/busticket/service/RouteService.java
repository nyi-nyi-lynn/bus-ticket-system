package com.busticket.service;

import com.busticket.dto.RouteDTO;

import java.util.List;

public interface RouteService {
    boolean save(RouteDTO dto);

    boolean update(RouteDTO dto);

    boolean delete(Long id);

    List<RouteDTO> getAll();
}
