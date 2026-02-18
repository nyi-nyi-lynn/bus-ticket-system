package com.busticket.service.impl;

import com.busticket.dao.BookingDAO;
import com.busticket.dao.impl.BookingDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.service.BookingService;

import javax.xml.crypto.Data;

public class BookingServiceImpl implements BookingService {
    private final BookingDAO bookingDAO;

    public BookingServiceImpl(){
        bookingDAO = new BookingDAOImpl(DatabaseConnection.getConnection());
    }
}
