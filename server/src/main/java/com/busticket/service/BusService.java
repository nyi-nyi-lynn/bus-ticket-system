package com.busticket.service;

import com.busticket.dto.BusDTO;

import java.util.List;

public interface BusService {
    boolean save(BusDTO dto);

    boolean update(BusDTO dto);

    boolean deactivate(Long id);

    List<BusDTO> getAll();
}
