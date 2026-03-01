package com.busticket.dao;

import java.util.List;

public interface BookingSeatDAO {
    void saveAll(Long bookingId, List<Long> seatIds);
}
