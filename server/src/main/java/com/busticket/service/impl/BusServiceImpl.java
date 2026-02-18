package com.busticket.service.impl;

import com.busticket.dao.BusDAO;
import com.busticket.dao.impl.BusDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BusDTO;
import com.busticket.mapper.BusMapper;
import com.busticket.model.Bus;
import com.busticket.service.BusService;

import java.util.List;

public class BusServiceImpl implements BusService {
    private final BusDAO busDAO;

    public BusServiceImpl(){
        busDAO = new BusDAOImpl(DatabaseConnection.getConnection());
    }
    @Override
    public boolean save(BusDTO dto) {
        Bus bus = BusMapper.toModel(dto);
        return busDAO.save(bus);
    }

    @Override
    public boolean update(BusDTO dto) {
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return false;
    }

    @Override
    public List<BusDTO> getAll() {
        return List.of();
    }
}
