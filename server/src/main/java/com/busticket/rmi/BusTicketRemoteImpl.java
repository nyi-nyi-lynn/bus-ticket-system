package com.busticket.rmi;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.service.TicketingService;
import com.busticket.service.impl.TicketingServiceImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.List;

public class BusTicketRemoteImpl extends UnicastRemoteObject implements BusTicketRemote {
    private final TicketingService ticketingService;

    public BusTicketRemoteImpl() throws RemoteException {
        this.ticketingService = new TicketingServiceImpl();
    }

    @Override
    public List<String> getOriginCities() throws RemoteException {
        return ticketingService.getOriginCities();
    }

    @Override
    public List<String> getDestinationCities(String originCity) throws RemoteException {
        return ticketingService.getDestinationCities(originCity);
    }

    @Override
    public List<TripDTO> getAdvertisedTrips() throws RemoteException {
        return ticketingService.getAdvertisedTrips();
    }

    @Override
    public List<TripDTO> searchTrips(String originCity, String destinationCity, LocalDate travelDate) throws RemoteException {
        return ticketingService.searchTrips(originCity, destinationCity, travelDate);
    }

    @Override
    public List<TripDTO> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate) throws RemoteException {
        return ticketingService.searchTripsClosest(originCity, destinationCity, requestedDate);
    }

    @Override
    public List<SeatDTO> getAvailableSeats(Long tripId) throws RemoteException {
        return ticketingService.getAvailableSeats(tripId);
    }

    @Override
    public List<BookingDTO> getBookingsByUser(Long userId) throws RemoteException {
        return ticketingService.getBookingsByUser(userId);
    }

    @Override
    public BookingDTO createBooking(BookingRequestDTO request) throws RemoteException {
        return ticketingService.createBooking(request);
    }

    @Override
    public PaymentDTO makePayment(Long bookingId, String paymentMethod, double paidAmount) throws RemoteException {
        return ticketingService.makePayment(bookingId, paymentMethod, paidAmount);
    }

    @Override
    public List<PaymentDTO> getPaymentsByUser(Long userId) throws RemoteException {
        return ticketingService.getPaymentsByUser(userId);
    }

    @Override
    public List<TicketDTO> getTicketsByUser(Long userId) throws RemoteException {
        return ticketingService.getTicketsByUser(userId);
    }

    @Override
    public TicketDTO viewTicket(String ticketCode) throws RemoteException {
        return ticketingService.viewTicket(ticketCode);
    }

    @Override
    public boolean validateTicket(String ticketCode, Long staffUserId) throws RemoteException {
        return ticketingService.validateTicket(ticketCode, staffUserId);
    }

    @Override
    public boolean addBus(BusDTO bus) throws RemoteException {
        return ticketingService.addBus(bus);
    }

    @Override
    public boolean addRoute(RouteDTO route) throws RemoteException {
        return ticketingService.addRoute(route);
    }

    @Override
    public boolean createTrip(TripDTO trip) throws RemoteException {
        return ticketingService.createTrip(trip);
    }

    @Override
    public SalesReportDTO getSalesReport(LocalDate fromDate, LocalDate toDate) throws RemoteException {
        return ticketingService.getSalesReport(fromDate, toDate);
    }
}
