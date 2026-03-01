package com.busticket.service;

import com.busticket.dto.BusDTO;
import com.busticket.dto.CreateBusRequest;
import com.busticket.dto.UpdateBusRequest;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;

import java.util.List;

public interface BusService {
    BusDTO createBus(CreateBusRequest request) throws DuplicateResourceException, ValidationException;

    BusDTO updateBus(UpdateBusRequest request) throws DuplicateResourceException, ValidationException;

    void deleteBus(Long busId) throws ValidationException;

    List<BusDTO> getAllBuses();
}
