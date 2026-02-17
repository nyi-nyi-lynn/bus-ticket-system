package com.busticket.dao;

import com.busticket.model.Booking;
import com.busticket.model.Bus;
import com.busticket.model.Payment;
import com.busticket.model.Route;
import com.busticket.model.SalesReport;
import com.busticket.model.Seat;
import com.busticket.model.Ticket;
import com.busticket.model.Trip;

import java.time.LocalDate;
import java.util.List;

public interface TicketingDAO {
    List<String> getOriginCities();

    List<String> getDestinationCities(String originCity);

    List<Trip> getAdvertisedTrips();

    boolean addBus(Bus bus);

    boolean addRoute(Route route);

    boolean createTrip(Trip trip);

    List<Trip> searchTrips(String originCity, String destinationCity, LocalDate travelDate);

    List<Trip> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate);

    List<Seat> getAvailableSeats(Long tripId);

    List<Booking> getBookingsByUser(Long userId);

    Booking createBooking(Booking bookingRequest);

    List<Payment> getPaymentsByUser(Long userId);

    Payment makePayment(Long bookingId, String paymentMethod, double paidAmount);

    List<Ticket> getTicketsByUser(Long userId);

    Ticket findTicketByCode(String ticketCode);

    boolean validateTicket(String ticketCode, Long staffUserId);

    SalesReport getSalesReport(LocalDate fromDate, LocalDate toDate);
}
