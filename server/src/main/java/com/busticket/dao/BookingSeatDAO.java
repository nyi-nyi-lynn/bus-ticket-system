package com.busticket.dao;

import java.util.List;

public interface BookingSeatDAO {
    // ADDED
    void saveAll(Long bookingId, List<Long> seatIds);
}
