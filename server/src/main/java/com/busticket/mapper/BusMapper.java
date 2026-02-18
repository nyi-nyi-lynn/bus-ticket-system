package com.busticket.mapper;

import com.busticket.dto.BusDTO;
import com.busticket.model.Bus;

public class BusMapper {
    public static Bus toModel(BusDTO dto) {
        Bus bus = new Bus();
        bus.setBusId(dto.getBusId());
        bus.setBusNumber(dto.getBusNumber());
        bus.setType(dto.getType());
        bus.setTotalSeats(dto.getTotalSeats());

        return bus;
    }
}
