package com.busticket.service.impl;

import com.busticket.dao.TicketingDAO;
import com.busticket.dao.impl.TicketingDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.dto.TripDTO;
import com.busticket.enums.BookingStatus;
import com.busticket.enums.BusType;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.TripStatus;
import com.busticket.mapper.TicketingMapper;
import com.busticket.model.Booking;
import com.busticket.model.Bus;
import com.busticket.model.Payment;
import com.busticket.model.Route;
import com.busticket.model.SalesReport;
import com.busticket.model.Ticket;
import com.busticket.model.Trip;
import com.busticket.service.TicketingService;

import java.time.LocalDate;
import java.util.List;

public class TicketingServiceImpl implements TicketingService {
    private final TicketingDAO ticketingDAO;

    public TicketingServiceImpl() {
        this.ticketingDAO = new TicketingDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public List<String> getOriginCities() {
        return ticketingDAO.getOriginCities();
    }

    @Override
    public List<String> getDestinationCities(String originCity) {
        return ticketingDAO.getDestinationCities(originCity);
    }

    @Override
    public List<TripDTO> getAdvertisedTrips() {
        return TicketingMapper.toTripDTOList(ticketingDAO.getAdvertisedTrips());
    }

    @Override
    public boolean addBus(BusDTO bus) {
        if (bus == null || isBlank(bus.getBusNumber()) || bus.getTotalSeats() <= 0) {
            return false;
        }
        bus.setType(normalizeBusType(bus.getType()).name());
        Bus model = TicketingMapper.toModel(bus);
        return ticketingDAO.addBus(model);
    }

    @Override
    public boolean addRoute(RouteDTO route) {
        if (route == null || isBlank(route.getOriginCity()) || isBlank(route.getDestinationCity())) {
            return false;
        }
        Route model = TicketingMapper.toModel(route);
        return ticketingDAO.addRoute(model);
    }

    @Override
    public boolean createTrip(TripDTO trip) {
        if (trip == null || trip.getBusId() == null || trip.getRouteId() == null
                || trip.getTravelDate() == null || trip.getDepartureTime() == null
                || trip.getArrivalTime() == null || trip.getPrice() <= 0) {
            return false;
        }
        if (trip.getStatus() == null || trip.getStatus().isBlank()) {
            trip.setStatus(TripStatus.OPEN.name());
        }
        Trip model = TicketingMapper.toModel(trip);
        return ticketingDAO.createTrip(model);
    }

    @Override
    public List<TripDTO> searchTrips(String originCity, String destinationCity, LocalDate travelDate) {
        if (isBlank(originCity) || isBlank(destinationCity) || travelDate == null) {
            return List.of();
        }
        List<Trip> trips = ticketingDAO.searchTrips(originCity, destinationCity, travelDate);
        return TicketingMapper.toTripDTOList(trips);
    }

    @Override
    public List<TripDTO> searchTripsClosest(String originCity, String destinationCity, LocalDate requestedDate) {
        if (isBlank(originCity) || isBlank(destinationCity) || requestedDate == null) {
            return List.of();
        }
        return TicketingMapper.toTripDTOList(ticketingDAO.searchTripsClosest(originCity, destinationCity, requestedDate));
    }

    @Override
    public List<SeatDTO> getAvailableSeats(Long tripId) {
        if (tripId == null) {
            return List.of();
        }
        return TicketingMapper.toSeatDTOList(ticketingDAO.getAvailableSeats(tripId));
    }

    @Override
    public BookingDTO createBooking(BookingRequestDTO request) {
        if (request == null || request.getUserId() == null || request.getTripId() == null
                || request.getSeatNumbers() == null || request.getSeatNumbers().isEmpty()) {
            return null;
        }
        Booking bookingModel = TicketingMapper.toModel(request);
        Booking booking = ticketingDAO.createBooking(bookingModel);
        if (booking == null) {
            return null;
        }
        booking.setStatus(BookingStatus.PENDING.name());
        return TicketingMapper.toDTO(booking);
    }

    @Override
    public List<BookingDTO> getBookingsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return TicketingMapper.toBookingDTOList(ticketingDAO.getBookingsByUser(userId));
    }

    @Override
    public PaymentDTO makePayment(Long bookingId, String paymentMethod, double paidAmount) {
        if (bookingId == null || paidAmount <= 0) {
            return null;
        }
        String method = normalizePaymentMethod(paymentMethod).name();
        Payment payment = ticketingDAO.makePayment(bookingId, method, paidAmount);
        return payment == null ? null : TicketingMapper.toDTO(payment);
    }

    @Override
    public List<PaymentDTO> getPaymentsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return TicketingMapper.toPaymentDTOList(ticketingDAO.getPaymentsByUser(userId));
    }

    @Override
    public TicketDTO viewTicket(String ticketCode) {
        if (isBlank(ticketCode)) {
            return null;
        }
        Ticket ticket = ticketingDAO.findTicketByCode(ticketCode);
        return ticket == null ? null : TicketingMapper.toDTO(ticket);
    }

    @Override
    public List<TicketDTO> getTicketsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return TicketingMapper.toTicketDTOList(ticketingDAO.getTicketsByUser(userId));
    }

    @Override
    public boolean validateTicket(String ticketCode, Long staffUserId) {
        if (isBlank(ticketCode) || staffUserId == null) {
            return false;
        }
        return ticketingDAO.validateTicket(ticketCode, staffUserId);
    }

    @Override
    public SalesReportDTO getSalesReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            return null;
        }
        SalesReport report = ticketingDAO.getSalesReport(fromDate, toDate);
        return report == null ? null : TicketingMapper.toDTO(report);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusType normalizeBusType(String type) {
        try {
            return BusType.valueOf(type == null ? "NORMAL" : type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BusType.NORMAL;
        }
    }

    private PaymentMethod normalizePaymentMethod(String method) {
        try {
            return PaymentMethod.valueOf(method == null ? "CARD" : method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CARD;
        }
    }
}
