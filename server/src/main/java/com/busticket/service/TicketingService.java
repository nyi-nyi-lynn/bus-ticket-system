package com.busticket.service;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.dto.TripDTO;

import java.time.LocalDate;
import java.util.List;

public interface TicketingService {
    List<String> getOriginCities();

    List<String> getDestinationCities(String originCity);

    List<TripDTO> getAdvertisedTrips();

    boolean addBus(BusDTO bus);

    boolean addRoute(RouteDTO route);

    boolean createTrip(TripDTO trip);

    List<TripDTO> searchTrips(String originCity, String destinationCity, LocalDate travelDate);

    List<TripDTO> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate);

    List<SeatDTO> getAvailableSeats(Long tripId);

    List<BookingDTO> getBookingsByUser(Long userId);

    BookingDTO createBooking(BookingRequestDTO request);

    List<PaymentDTO> getPaymentsByUser(Long userId);

    PaymentDTO makePayment(Long bookingId, String paymentMethod, double paidAmount);

    List<TicketDTO> getTicketsByUser(Long userId);

    TicketDTO viewTicket(String ticketCode);

    boolean validateTicket(String ticketCode, Long staffUserId);

    SalesReportDTO getSalesReport(LocalDate fromDate, LocalDate toDate);
}
