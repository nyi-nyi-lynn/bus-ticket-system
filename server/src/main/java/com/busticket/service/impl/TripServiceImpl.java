package com.busticket.service.impl;

import com.busticket.dao.TripDAO;
import com.busticket.dao.impl.TripDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.TripDTO;
import com.busticket.service.TripService;
import java.time.LocalDate;
import java.util.List;

public class TripServiceImpl implements TripService {

   private final TripDAO tripDAO;

   public TripServiceImpl(){
       tripDAO = new TripDAOImpl(DatabaseConnection.getConnection());
   }

    @Override
    public boolean save(TripDTO dto) {
        return false;
    }

    @Override
    public boolean update(TripDTO dto) {
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return false;
    }

    @Override
    public List<TripDTO> getAll() {
        return List.of();
    }

    @Override
    public List<TripDTO> search(String origin, String destination, LocalDate date) {
        return List.of();
    }
}
