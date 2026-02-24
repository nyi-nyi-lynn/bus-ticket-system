package com.busticket.service.impl;

import com.busticket.dao.SeatDAO;
import com.busticket.dao.impl.SeatDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.SeatDTO;
import com.busticket.service.SeatService;

import java.util.List;

public class SeatServiceImpl implements SeatService {
    private final SeatDAO seatDAO;

    public SeatServiceImpl() {
        this.seatDAO = new SeatDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public List<SeatDTO> getSeatsByBusId(Long busId) {
        // ADDED
        if (busId == null) {
            return List.of();
        }
        return seatDAO.findByBusId(busId);
    }
}
