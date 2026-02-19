package com.busticket.service.impl;

import com.busticket.dao.BusDAO;
import com.busticket.dao.impl.BusDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BusDTO;
import com.busticket.enums.BusType;
import com.busticket.model.Bus;
import com.busticket.service.BusService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BusServiceImpl implements BusService {
    private final BusDAO busDAO;

    public BusServiceImpl(){
        busDAO = new BusDAOImpl(DatabaseConnection.getConnection());
    }
    @Override
    public boolean save(BusDTO dto) {
        // Validate required fields.
        return busDAO.save(toModel(dto));
    }

    @Override
    public boolean update(BusDTO dto) {
        // Validate required fields and identifiers.
        if (dto == null || dto.getBusId() == null) {
            return false;
        }
        return busDAO.update(toModel(dto));
    }

    @Override
    public boolean deactivate(Long id) {
        // Soft delete: mark bus inactive.
        return busDAO.deactivate(id);
    }

    @Override
    public List<BusDTO> getAll() {
        List<Bus> buses = busDAO.findAll();
        List<BusDTO> dtos = new ArrayList<>();
        for (Bus bus : buses) {
            dtos.add(toDTO(bus));
        }
        return dtos;
    }

    private Bus toModel(BusDTO dto) {
        Bus bus = new Bus();
        bus.setBusId(dto.getBusId());
        bus.setBusNumber(dto.getBusNumber());
        bus.setType(parseType(dto.getType()));
        bus.setTotalSeats(dto.getTotalSeats());
        return bus;
    }

    private BusDTO toDTO(Bus bus) {
        BusDTO dto = new BusDTO();
        dto.setBusId(bus.getBusId());
        dto.setBusNumber(bus.getBusNumber());
        dto.setType(bus.getType() == null ? null : bus.getType().name());
        dto.setTotalSeats(bus.getTotalSeats());
        return dto;
    }

    private BusType parseType(String type) {
        if (type == null || type.isBlank()) {
            return BusType.NORMAL;
        }
        try {
            return BusType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BusType.NORMAL;
        }
    }
}
