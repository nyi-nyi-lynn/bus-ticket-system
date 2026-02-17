package com.busticket.mapper;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BusDTO;
import com.busticket.dto.PaymentDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.SalesReportDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TicketDTO;
import com.busticket.dto.TripDTO;
import com.busticket.model.Booking;
import com.busticket.model.Bus;
import com.busticket.model.Payment;
import com.busticket.model.Route;
import com.busticket.model.SalesReport;
import com.busticket.model.Seat;
import com.busticket.model.Ticket;
import com.busticket.model.Trip;

import java.util.ArrayList;
import java.util.List;

public final class TicketingMapper {
    private TicketingMapper() {
    }

    public static Bus toModel(BusDTO dto) {
        Bus bus = new Bus();
        bus.setBusId(dto.getBusId());
        bus.setBusNumber(dto.getBusNumber());
        bus.setType(dto.getType());
        bus.setTotalSeats(dto.getTotalSeats());
        return bus;
    }

    public static Route toModel(RouteDTO dto) {
        Route route = new Route();
        route.setRouteId(dto.getRouteId());
        route.setOriginCity(dto.getOriginCity());
        route.setDestinationCity(dto.getDestinationCity());
        route.setDistanceKm(dto.getDistanceKm());
        route.setEstimatedDuration(dto.getEstimatedDuration());
        return route;
    }

    public static Trip toModel(TripDTO dto) {
        Trip trip = new Trip();
        trip.setTripId(dto.getTripId());
        trip.setBusId(dto.getBusId());
        trip.setRouteId(dto.getRouteId());
        trip.setTravelDate(dto.getTravelDate());
        trip.setDepartureTime(dto.getDepartureTime());
        trip.setArrivalTime(dto.getArrivalTime());
        trip.setPrice(dto.getPrice());
        trip.setStatus(dto.getStatus());
        trip.setBusNumber(dto.getBusNumber());
        trip.setOriginCity(dto.getOriginCity());
        trip.setDestinationCity(dto.getDestinationCity());
        trip.setTotalSeats(dto.getTotalSeats());
        trip.setAvailableSeats(dto.getAvailableSeats());
        return trip;
    }

    public static Booking toModel(BookingRequestDTO request) {
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setTripId(request.getTripId());
        booking.setSeatNumbers(new ArrayList<>(request.getSeatNumbers()));
        return booking;
    }

    public static BusDTO toDTO(Bus model) {
        return new BusDTO(model.getBusId(), model.getBusNumber(), model.getType(), model.getTotalSeats());
    }

    public static RouteDTO toDTO(Route model) {
        return new RouteDTO(
                model.getRouteId(),
                model.getOriginCity(),
                model.getDestinationCity(),
                model.getDistanceKm(),
                model.getEstimatedDuration()
        );
    }

    public static TripDTO toDTO(Trip model) {
        TripDTO dto = new TripDTO();
        dto.setTripId(model.getTripId());
        dto.setBusId(model.getBusId());
        dto.setRouteId(model.getRouteId());
        dto.setTravelDate(model.getTravelDate());
        dto.setDepartureTime(model.getDepartureTime());
        dto.setArrivalTime(model.getArrivalTime());
        dto.setPrice(model.getPrice());
        dto.setStatus(model.getStatus());
        dto.setBusNumber(model.getBusNumber());
        dto.setOriginCity(model.getOriginCity());
        dto.setDestinationCity(model.getDestinationCity());
        dto.setTotalSeats(model.getTotalSeats());
        dto.setAvailableSeats(model.getAvailableSeats());
        return dto;
    }

    public static List<TripDTO> toTripDTOList(List<Trip> models) {
        List<TripDTO> list = new ArrayList<>();
        for (Trip model : models) {
            list.add(toDTO(model));
        }
        return list;
    }

    public static SeatDTO toDTO(Seat model) {
        return new SeatDTO(model.getSeatId(), model.getBusId(), model.getSeatNumber());
    }

    public static List<SeatDTO> toSeatDTOList(List<Seat> models) {
        List<SeatDTO> list = new ArrayList<>();
        for (Seat model : models) {
            list.add(toDTO(model));
        }
        return list;
    }

    public static BookingDTO toDTO(Booking model) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(model.getBookingId());
        dto.setUserId(model.getUserId());
        dto.setTripId(model.getTripId());
        dto.setBookingDate(model.getBookingDate());
        dto.setTotalPrice(model.getTotalPrice());
        dto.setTicketCode(model.getTicketCode());
        dto.setStatus(model.getStatus());
        dto.setSeatNumbers(model.getSeatNumbers());
        return dto;
    }

    public static List<BookingDTO> toBookingDTOList(List<Booking> models) {
        List<BookingDTO> list = new ArrayList<>();
        for (Booking model : models) {
            list.add(toDTO(model));
        }
        return list;
    }

    public static PaymentDTO toDTO(Payment model) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(model.getPaymentId());
        dto.setBookingId(model.getBookingId());
        dto.setPaymentMethod(model.getPaymentMethod());
        dto.setPaymentStatus(model.getPaymentStatus());
        dto.setPaidAmount(model.getPaidAmount());
        dto.setPaidAt(model.getPaidAt());
        return dto;
    }

    public static List<PaymentDTO> toPaymentDTOList(List<Payment> models) {
        List<PaymentDTO> list = new ArrayList<>();
        for (Payment model : models) {
            list.add(toDTO(model));
        }
        return list;
    }

    public static TicketDTO toDTO(Ticket model) {
        TicketDTO dto = new TicketDTO();
        dto.setTicketCode(model.getTicketCode());
        dto.setPassengerName(model.getPassengerName());
        dto.setBusNumber(model.getBusNumber());
        dto.setOriginCity(model.getOriginCity());
        dto.setDestinationCity(model.getDestinationCity());
        dto.setTravelDate(model.getTravelDate());
        dto.setDepartureTime(model.getDepartureTime());
        dto.setSeatNumbers(model.getSeatNumbers());
        dto.setBookingStatus(model.getBookingStatus());
        dto.setQrPayload(model.getQrPayload());
        return dto;
    }

    public static List<TicketDTO> toTicketDTOList(List<Ticket> models) {
        List<TicketDTO> list = new ArrayList<>();
        for (Ticket model : models) {
            list.add(toDTO(model));
        }
        return list;
    }

    public static SalesReportDTO toDTO(SalesReport model) {
        SalesReportDTO dto = new SalesReportDTO();
        dto.setFromDate(model.getFromDate());
        dto.setToDate(model.getToDate());
        dto.setTotalConfirmedBookings(model.getTotalConfirmedBookings());
        dto.setTotalRevenue(model.getTotalRevenue());
        return dto;
    }
}
