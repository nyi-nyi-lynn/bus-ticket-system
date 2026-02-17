package com.busticket.remote;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.dto.TripDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

public interface BusTicketRemote extends Remote {
    List<String> getOriginCities() throws RemoteException;

    List<String> getDestinationCities(String originCity) throws RemoteException;

    List<TripDTO> getAdvertisedTrips() throws RemoteException;

    List<TripDTO> searchTrips(String originCity, String destinationCity, LocalDate travelDate) throws RemoteException;

    List<TripDTO> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate) throws RemoteException;

    List<SeatDTO> getAvailableSeats(Long tripId) throws RemoteException;

    List<BookingDTO> getBookingsByUser(Long userId) throws RemoteException;

    BookingDTO createBooking(BookingRequestDTO request) throws RemoteException;

    List<PaymentDTO> getPaymentsByUser(Long userId) throws RemoteException;

    PaymentDTO makePayment(Long bookingId, String paymentMethod, double paidAmount) throws RemoteException;

    List<TicketDTO> getTicketsByUser(Long userId) throws RemoteException;

    TicketDTO viewTicket(String ticketCode) throws RemoteException;

    boolean validateTicket(String ticketCode, Long staffUserId) throws RemoteException;

    boolean addBus(BusDTO bus) throws RemoteException;

    boolean addRoute(RouteDTO route) throws RemoteException;

    boolean createTrip(TripDTO trip) throws RemoteException;

    SalesReportDTO getSalesReport(LocalDate fromDate, LocalDate toDate) throws RemoteException;
}
