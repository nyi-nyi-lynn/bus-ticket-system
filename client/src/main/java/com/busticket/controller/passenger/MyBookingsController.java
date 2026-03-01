package com.busticket.controller.passenger;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.TripDTO;
import com.busticket.enums.PaymentStatus;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MyBookingsController {
    @FXML private VBox bookingsFlow;

    private BookingRemote bookingRemote;
    private TripRemote tripRemote;
    private final Map<Long, TripDTO> tripById = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH);

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
        card.getStyleClass().add("trip-card");
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);
        if (bookingsFlow != null) {
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
                : DATE_FORMAT.format(booking.getBookingDate()).toLowerCase(Locale.ENGLISH);
        String status = safeStatus(booking == null ? null : booking.getStatus());
        String ticketCode = safe(booking == null ? null : booking.getTicketCode());
        String total = booking == null || booking.getTotalPrice() == null
                ? "0.00"
                : String.format("%.2f", booking.getTotalPrice());

        Label routeLabel = new Label("Route: " + route);
        routeLabel.getStyleClass().add("trip-route");
        Label seatsLabel = new Label("Seats: " + seats);
        seatsLabel.getStyleClass().add("trip-seats");
        Label dateLabel = new Label("Booking Date: " + date);
        dateLabel.getStyleClass().add("trip-meta-title");
        Label statusLabel = new Label("Status: " + status);
        statusLabel.getStyleClass().add("trip-meta-title");
        Label ticketLabel = new Label("Ticket Code: " + ticketCode);
        ticketLabel.getStyleClass().add("trip-meta-title");
        Label totalLabel = new Label("Total Price: " + total);
        totalLabel.getStyleClass().add("trip-price");

        HBox actionRow = new HBox(10);
        Button payNowButton = new Button("Pay Now");
        payNowButton.getStyleClass().add("primary-button");
        boolean payable = isPayNowEnabled(booking);
        payNowButton.setDisable(!payable);
        if (!payable) {
            payNowButton.setOpacity(0.55);
        } else {
            payNowButton.setOnAction(event -> onPayNow(booking, payNowButton));
        }

        Button viewTicketButton = new Button("View Ticket");
        viewTicketButton.getStyleClass().add("ghost-button");
        viewTicketButton.setOnAction(event -> onViewTicket(booking));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actionRow.getChildren().addAll(payNowButton, viewTicketButton, spacer);

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

    private boolean isPayNowEnabled(BookingDTO booking) {
        if (booking == null) {
            return false;
        }
        String bookingStatus = booking.getStatus();
        PaymentStatus paymentStatus = booking.getPaymentStatus();
        boolean bookingPending = bookingStatus != null && "PENDING".equalsIgnoreCase(bookingStatus.trim());
        boolean paymentPending = paymentStatus == null || paymentStatus == PaymentStatus.PENDING;
        return bookingPending && paymentPending;
    }

    private void onPayNow(BookingDTO booking, Button payNowButton) {
        if (Session.isGuest() || Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You are not logged in.", "Please login to continue payment.");
            return;
        }
        if (booking == null || booking.getBookingId() == null) {
            showAlert(Alert.AlertType.WARNING, "Invalid Booking", "Booking is missing.", "Please refresh and try again.");
            return;
        }

        Long currentUserId = Session.getCurrentUser().getUserId();
        if (booking.getUserId() == null || !Objects.equals(booking.getUserId(), currentUserId)) {
            showAlert(Alert.AlertType.WARNING, "Unauthorized", "This booking does not belong to your account.", "Please refresh your bookings.");
            return;
        }
        if (!isPayNowEnabled(booking)) {
            showAlert(Alert.AlertType.INFORMATION, "Payment Unavailable", "This booking is not payable.", "Only pending and unpaid bookings can be paid.");
            return;
        }

        payNowButton.setDisable(true);
        payNowButton.setText("Loading...");
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(14, 14);
        payNowButton.setGraphic(loadingIndicator);

        TripDTO trip = tripById.get(booking.getTripId());
        Session.setPendingSelection(trip, booking.getSeatNumbers());
        Session.setCurrentBookingContext(
                booking.getBookingId(),
                currentUserId,
                booking.getTicketCode(),
                booking.getTotalPrice()
        );

        try {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
        } catch (Exception ex) {
            payNowButton.setDisable(false);
            payNowButton.setText("Pay Now");
            payNowButton.setGraphic(null);
            showAlert(Alert.AlertType.ERROR, "Navigation Failed", "Unable to open payment page.", ex.getMessage());
        }
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String safeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "-";
        }
        return status.trim().toUpperCase();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
