package com.busticket.controller.passenger;

import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.BookingResponseDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BookingRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.util.List;

public class BookingSummaryController {
    @FXML private Label routeLabel;
    @FXML private Label seatsLabel;
    @FXML private Label perSeatPriceLabel;
    @FXML private Label totalPriceLabel;

    private TripDTO trip;
    private List<String> selectedSeats;

    @FXML
    private void initialize() {
        trip = Session.getPendingTrip();
        selectedSeats = Session.getPendingSeatNumbers();
        renderSummary();
    }

    @FXML
    private void onBack() {
        SceneSwitcher.switchToSeatSelection(trip);
    }

    @FXML
    private void onConfirmBooking() {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "User session missing.", "Please login and try again.");
            return;
        }
        if (trip == null || trip.getTripId() == null || selectedSeats == null || selectedSeats.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Summary", "Trip or seats missing.", "Please reselect your seats.");
            return;
        }

        try {
            BookingRemote bookingRemote = RMIClient.getBookingRemote();
            BookingRequestDTO request = new BookingRequestDTO();
            request.setUserId(Session.getCurrentUser().getUserId());
            request.setTripId(trip.getTripId());
            request.setSeatNumbers(selectedSeats);

            BookingResponseDTO response = bookingRemote.createBooking(request);
            if (response == null || response.getBookingId() == null) {
                showAlert(Alert.AlertType.ERROR, "Booking Failed", "Unable to create booking.", "Please try again.");
                return;
            }

            Session.setCurrentBookingContext(response.getBookingId(), response.getTicketCode(), response.getTotalAmount());
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Booking Failed", "Unable to create booking.", ex.getMessage());
        }
    }

    private void renderSummary() {
        if (trip == null || selectedSeats == null) {
            routeLabel.setText("-");
            seatsLabel.setText("-");
            perSeatPriceLabel.setText("$0.00");
            totalPriceLabel.setText("$0.00");
            return;
        }
        double perSeat = trip.getPrice();
        double total = perSeat * selectedSeats.size();
        routeLabel.setText(trip.getOriginCity() + " -> " + trip.getDestinationCity());
        seatsLabel.setText(String.join(", ", selectedSeats));
        perSeatPriceLabel.setText(String.format("$%.2f", perSeat));
        totalPriceLabel.setText(String.format("$%.2f", total));
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
