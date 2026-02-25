package com.busticket.controller.passenger;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BookingRemote;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsController {
    @FXML private VBox bookingsFlow; // MODIFIED

    private BookingRemote bookingRemote;
    private TripRemote tripRemote;
    private final Map<Long, TripDTO> tripById = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        try {
            bookingRemote = RMIClient.getBookingRemote();
            tripRemote = RMIClient.getTripRemote();
            loadTripCache();
            loadBookings();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Loading Failed", "Unable to initialize My Bookings.", ex.getMessage());
        }
    }

    private void loadTripCache() {
        try {
            List<TripDTO> trips = tripRemote.getAllTrips();
            tripById.clear();
            if (trips != null) {
                for (TripDTO trip : trips) {
                    if (trip != null && trip.getTripId() != null) {
                        tripById.put(trip.getTripId(), trip);
                    }
                }
            }
        } catch (Exception ex) {
            tripById.clear();
        }
    }

    private void loadBookings() {
        bookingsFlow.getChildren().clear();

        if (Session.isGuest() || Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You are not logged in.", "Please login to view your bookings.");
            return;
        }

        try {
            Long userId = Session.getCurrentUser().getUserId();
            List<BookingDTO> bookings = bookingRemote.getBookingsByUserId(userId);
            if (bookings == null || bookings.isEmpty()) {
                bookingsFlow.getChildren().add(new Label("No bookings found."));
                return;
            }

            for (BookingDTO booking : bookings) {
                bookingsFlow.getChildren().add(createBookingCard(booking));
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Loading Failed", "Unable to load bookings.", ex.getMessage());
        }
    }

    private VBox createBookingCard(BookingDTO booking) {
        VBox card = new VBox(8);
        card.getStyleClass().add("trip-card"); // MODIFIED
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE); // MODIFIED
        if (bookingsFlow != null) { // MODIFIED
            card.prefWidthProperty().bind(bookingsFlow.widthProperty().subtract(2));
        }

        TripDTO trip = booking == null ? null : tripById.get(booking.getTripId());
        String route = trip == null
                ? "Trip #" + safe(booking == null ? null : booking.getTripId())
                : safe(trip.getOriginCity()) + " -> " + safe(trip.getDestinationCity());
        String seats = booking == null || booking.getSeatNumbers() == null
                ? "-"
                : String.join(", ", booking.getSeatNumbers());
        String date = booking == null || booking.getBookingDate() == null
                ? "-"
                : DATE_FORMAT.format(booking.getBookingDate());
        String status = safe(booking == null ? null : booking.getStatus());
        String ticketCode = safe(booking == null ? null : booking.getTicketCode());
        String total = booking == null || booking.getTotalPrice() == null
                ? "0.00"
                : String.format("%.2f", booking.getTotalPrice());

        Label routeLabel = new Label("Route: " + route); // MODIFIED
        routeLabel.getStyleClass().add("trip-route");
        Label seatsLabel = new Label("Seats: " + seats); // MODIFIED
        seatsLabel.getStyleClass().add("trip-seats");
        Label dateLabel = new Label("Booking Date: " + date); // MODIFIED
        dateLabel.getStyleClass().add("trip-meta-title");
        Label statusLabel = new Label("Status: " + status); // MODIFIED
        statusLabel.getStyleClass().add("trip-meta-title");
        Label ticketLabel = new Label("Ticket Code: " + ticketCode); // MODIFIED
        ticketLabel.getStyleClass().add("trip-meta-title");
        Label totalLabel = new Label("Total Price: " + total); // MODIFIED
        totalLabel.getStyleClass().add("trip-price");

        HBox actionRow = new HBox(10);
        Button viewTicketButton = new Button("View Ticket");
        viewTicketButton.getStyleClass().add("ghost-button");
        viewTicketButton.setOnAction(event -> onViewTicket(booking));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actionRow.getChildren().addAll(viewTicketButton, spacer);

        card.getChildren().addAll(routeLabel, seatsLabel, dateLabel, statusLabel, ticketLabel, totalLabel, actionRow);
        return card;
    }

    private void onViewTicket(BookingDTO booking) {
        if (Session.isGuest() || Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You are not logged in.", "Please login to view tickets.");
            return;
        }
        if (booking == null || booking.getBookingId() == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Booking is missing.", "Please refresh and try again.");
            return;
        }
        Session.setCurrentBookingContext(booking.getBookingId(), booking.getTicketCode(), booking.getTotalPrice());
        SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketView.fxml");
    }

    private void onCancelBooking(BookingDTO booking) {
        if (Session.isGuest() || Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You are not logged in.", "Please login to cancel bookings.");
            return;
        }
        if (booking == null || booking.getBookingId() == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Selection", "Booking is missing.", "Please refresh and try again.");
            return;
        }

        try {
            boolean cancelled = bookingRemote.cancelBooking(booking.getBookingId(), Session.getCurrentUser().getUserId());
            if (!cancelled) {
                showAlert(Alert.AlertType.WARNING, "Cancel Failed", "Unable to cancel booking.", "Booking may already be cancelled.");
                return;
            }
            loadBookings();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Cancel Failed", "Unable to cancel booking.", ex.getMessage());
        }
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
