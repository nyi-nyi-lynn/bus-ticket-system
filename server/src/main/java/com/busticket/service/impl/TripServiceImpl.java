package com.busticket.service.impl;

import com.busticket.dao.TripDAO;
import com.busticket.dao.impl.TripDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.TripDTO;
import com.busticket.enums.TripStatus;
import com.busticket.model.Trip;
import com.busticket.service.TripService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripServiceImpl implements TripService {

   private final TripDAO tripDAO;

   public TripServiceImpl(){
       tripDAO = new TripDAOImpl(DatabaseConnection.getConnection());
   }

    @Override
    public boolean save(TripDTO dto) {
        return tripDAO.save(toModel(dto));
    }

    @Override
    public boolean update(TripDTO dto) {
        if (dto == null || dto.getTripId() == null) {
            return false;
        }
        return tripDAO.update(toModel(dto));
    }

    @Override
    public boolean delete(Long id) {
        return tripDAO.delete(id);
    }

    @Override
    public List<TripDTO> getAll() {
        List<Trip> trips = tripDAO.findAll();
        List<TripDTO> dtos = new ArrayList<>();
        for (Trip trip : trips) {
            dtos.add(toDTO(trip));
        }
        return dtos;
    }

    @Override
    public List<TripDTO> search(String origin, String destination, LocalDate date) {
        List<Trip> trips = tripDAO.search(origin, destination, date);
        List<TripDTO> dtos = new ArrayList<>();
        for (Trip trip : trips) {
            dtos.add(toDTO(trip));
        }
        return dtos;
    }

    private Trip toModel(TripDTO dto) {
        Trip trip = new Trip();
        trip.setTripId(dto.getTripId());
        trip.setBusId(dto.getBusId());
        trip.setRouteId(dto.getRouteId());
        trip.setTravelDate(dto.getTravelDate());
        trip.setDepartureTime(dto.getDepartureTime());
        trip.setArrivalTime(dto.getArrivalTime());
        trip.setPrice(dto.getPrice());
        trip.setStatus(parseStatus(dto.getStatus()));
        return trip;
    }

    private TripDTO toDTO(Trip trip) {
        TripDTO dto = new TripDTO();
        dto.setTripId(trip.getTripId());
        dto.setBusId(trip.getBusId());
        dto.setRouteId(trip.getRouteId());
        dto.setTravelDate(trip.getTravelDate());
        dto.setDepartureTime(trip.getDepartureTime());
        dto.setArrivalTime(trip.getArrivalTime());
        dto.setPrice(trip.getPrice());
        dto.setStatus(trip.getStatus() == null ? null : trip.getStatus().name());
        return dto;
    }

    private TripStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TripStatus.OPEN;
        }
        try {
            return TripStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TripStatus.OPEN;
        }
    }
}
