package com.busticket.controller.passenger;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BookingRemote;
import com.busticket.remote.BusRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BookingSummaryController {
    @FXML private Label routeLabel;
    @FXML private Label travelDateLabel;
    @FXML private Label departureTimeLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label busNumberLabel;
    @FXML private Label busTypeLabel;
    @FXML private ListView<String> selectedSeatsListView;
    @FXML private Label seatCountLabel;
    @FXML private Label seatNumbersLabel;
    @FXML private Label perSeatPriceLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Button confirmPayButton;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private TripDTO trip;
    private List<SeatDTO> selectedSeats = List.of();

    @FXML
    private void initialize() {
        trip = Session.getPendingTrip();
        selectedSeats = normalizeSelectedSeats();
        selectedSeatsListView.setFocusTraversable(false);

        renderSummary();
    }

    @FXML
    private void onBack() {
        if (trip != null) {
            SceneSwitcher.switchToSeatSelection(trip);
        }
    }

    @FXML
    private void onConfirmAndPay() {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "User session missing.", "Please login and try again.");
            return;
        }
        if (trip == null || trip.getTripId() == null || selectedSeats.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Summary", "Trip or seats missing.", "Please reselect your seats.");
            return;
        }

        List<String> seatNumbers = selectedSeats.stream()
                .map(SeatDTO::getSeatNumber)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
        if (seatNumbers.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Seats", "Selected seats are invalid.", "Please reselect your seats.");
            return;
        }

        BookingDTO booking;
        try {
            confirmPayButton.setDisable(true);

            BookingRemote bookingRemote = RMIClient.getBookingRemote();
            BookingDTO request = new BookingDTO();
            request.setUserId(Session.getCurrentUser().getUserId());
            request.setTripId(trip.getTripId());
            request.setSeatNumbers(seatNumbers);
            booking = bookingRemote.createBooking(request);
            if (booking == null || booking.getBookingId() == null) {
                showAlert(Alert.AlertType.ERROR, "Booking Failed", "Unable to create booking.", "Please try again.");
                return;
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Booking Failed", "Unable to create pending booking.", ex.getMessage());
            confirmPayButton.setDisable(false);
            return;
        }

        Session.setCurrentBookingContext(
                booking.getBookingId(),
                Session.getCurrentUser().getUserId(),
                booking.getTicketCode(),
                booking.getTotalPrice()
        );

        try {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
        } catch (Exception ex) {
            try {
                BookingRemote bookingRemote = RMIClient.getBookingRemote();
                bookingRemote.cancelBooking(booking.getBookingId(), Session.getCurrentUser().getUserId());
            } catch (Exception ignored) {
                // Best-effort cleanup to avoid leaving seats locked if payment view cannot open.
            }
            Session.clearBookingContext();
            showAlert(Alert.AlertType.ERROR, "Navigation Failed", "Booking was created but payment screen failed to open.", ex.getMessage());
        } finally {
            confirmPayButton.setDisable(false);
        }
    }

    private List<SeatDTO> normalizeSelectedSeats() {
        List<SeatDTO> seats = Session.getPendingSeats();
        if (seats != null && !seats.isEmpty()) {
            return seats;
        }

        List<String> seatNumbers = Session.getPendingSeatNumbers();
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            return List.of();
        }

        List<SeatDTO> mapped = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            SeatDTO seat = new SeatDTO();
            seat.setSeatNumber(seatNumber);
            mapped.add(seat);
        }
        return mapped;
    }

    private void renderSummary() {
        if (trip == null) {
            setEmptyView();
            return;
        }

        routeLabel.setText(safe(trip.getOriginCity()) + " \u2192 " + safe(trip.getDestinationCity()));
        travelDateLabel.setText(trip.getTravelDate() == null ? "-" : trip.getTravelDate().format(DATE_FMT));
        departureTimeLabel.setText(trip.getDepartureTime() == null ? "-" : trip.getDepartureTime().format(TIME_FMT));
        arrivalTimeLabel.setText(trip.getArrivalTime() == null ? "-" : trip.getArrivalTime().format(TIME_FMT));
        busNumberLabel.setText(safe(trip.getBusNumber()));
        busTypeLabel.setText(resolveBusType(trip));

        List<String> seatNumbers = selectedSeats.stream()
                .map(SeatDTO::getSeatNumber)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        selectedSeatsListView.setItems(FXCollections.observableArrayList(seatNumbers));

        int seatCount = seatNumbers.size();
        double pricePerSeat = trip.getPrice();
        double totalPrice = seatCount * pricePerSeat;

        seatCountLabel.setText(String.valueOf(seatCount));
        seatNumbersLabel.setText(seatNumbers.isEmpty() ? "-" : String.join(", ", seatNumbers));
        perSeatPriceLabel.setText(String.format("$%.2f", pricePerSeat));
        totalPriceLabel.setText(String.format("$%.2f", totalPrice));
    }

    private String resolveBusType(TripDTO tripDto) {
        try {
            if (tripDto == null || tripDto.getBusId() == null) {
                return "-";
            }
            BusRemote busRemote = RMIClient.getBusRemote();
            return busRemote.getAllBuses().stream()
                    .filter(bus -> Objects.equals(bus.getBusId(), tripDto.getBusId()))
                    .map(bus -> bus.getType() == null || bus.getType().isBlank() ? "-" : bus.getType())
                    .findFirst()
                    .orElse("-");
        } catch (Exception ex) {
            return "-";
        }
    }

    private void setEmptyView() {
        routeLabel.setText("-");
        travelDateLabel.setText("-");
        departureTimeLabel.setText("-");
        arrivalTimeLabel.setText("-");
        busNumberLabel.setText("-");
        busTypeLabel.setText("-");
        selectedSeatsListView.setItems(FXCollections.observableArrayList());
        seatCountLabel.setText("0");
        seatNumbersLabel.setText("-");
        perSeatPriceLabel.setText("$0.00");
        totalPriceLabel.setText("$0.00");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
