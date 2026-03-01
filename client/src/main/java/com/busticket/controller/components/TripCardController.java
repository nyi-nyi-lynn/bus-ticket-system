package com.busticket.controller.components;

import com.busticket.dto.TripDTO;
import com.busticket.util.SceneSwitcher; // ADDED
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class TripCardController {
    @FXML private StackPane cardRoot;
    @FXML private Label routeLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private Label departureTimeLabel;
    @FXML private Label arrowIconLabel;
    @FXML private Label arrivalTimeLabel;
    @FXML private Label durationLabel;
    @FXML private Label busNumberLabel;
    @FXML private Label busTypeLabel;
    @FXML private Label priceLabel;
    @FXML private Label seatsLeftLabel;
    @FXML private Button bookNowButton;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0");

    private final ScaleTransition hoverIn = new ScaleTransition(Duration.millis(150));
    private final ScaleTransition hoverOut = new ScaleTransition(Duration.millis(150));

    private TripDTO currentTrip;
    private Consumer<TripDTO> onBookNow;

    @FXML
    private void initialize() {
        hoverIn.setNode(cardRoot);
        hoverIn.setToX(1.02);
        hoverIn.setToY(1.02);

        hoverOut.setNode(cardRoot);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        cardRoot.setOnMouseEntered(event -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });
        cardRoot.setOnMouseExited(event -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
        // ADDED: Ensure button click triggers booking handler even if FXML lacks onAction.
        if (bookNowButton != null) {
            bookNowButton.setOnAction(event -> handleBookNow()); // MODIFIED
        }
    }

    public void setData(TripDTO trip) {
        currentTrip = trip;
        if (trip == null) {
            routeLabel.setText("- \u2192 -");
            departureTimeLabel.setText("--:--");
            arrivalTimeLabel.setText("--:--");
            durationLabel.setText("Duration: --");
            busNumberLabel.setText("Bus -");
            busTypeLabel.setManaged(false);
            busTypeLabel.setVisible(false);
            priceLabel.setText("MMK 0");
            seatsLeftLabel.setText("Seats left: 0");
            applyStatusState(true);
            return;
        }

        String origin = safeText(trip.getOriginCity(), "-");
        String destination = safeText(trip.getDestinationCity(), "-");
        routeLabel.setText(origin + " \u2192 " + destination);

        departureTimeLabel.setText(formatTime(trip.getDepartureTime()));
        arrivalTimeLabel.setText(formatTime(trip.getArrivalTime()));
        durationLabel.setText(formatDuration(trip.getDepartureTime(), trip.getArrivalTime()));
        arrowIconLabel.setText("\u2192");

        busNumberLabel.setText(safeText(trip.getBusNumber(), "Bus -"));
        applyBusType(trip);

        priceLabel.setText("MMK " + PRICE_FORMAT.format(trip.getPrice()));
        seatsLeftLabel.setText("Seats left: " + trip.getAvailableSeats());

        boolean isClosed = isClosedTrip(trip);
        applyStatusState(isClosed);
    }

    public void setOnBookNow(Consumer<TripDTO> onBookNow) {
        this.onBookNow = onBookNow;
    }

    @FXML
    private void handleBookNow() {
        navigateToBooking(currentTrip);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String formatTime(LocalTime time) {
        return time == null ? "--:--" : TIME_FORMAT.format(time).toLowerCase(Locale.ENGLISH);
    }

    private String formatDuration(LocalTime departure, LocalTime arrival) {
        if (departure == null || arrival == null) {
            return "Duration: --";
        }
        long minutes = ChronoUnit.MINUTES.between(departure, arrival);
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        long hoursPart = minutes / 60;
        long minutesPart = minutes % 60;
        return "Duration: " + hoursPart + "h " + minutesPart + "m";
    }

    private boolean isClosedTrip(TripDTO trip) {
        if (trip == null) {
            return true;
        }
        if (trip.getAvailableSeats() <= 0) {
            return true;
        }
        return "CLOSED".equalsIgnoreCase(trip.getStatus());
    }

    private void applyStatusState(boolean closed) {
        statusBadgeLabel.getStyleClass().removeAll("badge-open", "badge-closed");
        if (closed) {
            statusBadgeLabel.getStyleClass().add("badge-closed");
            statusBadgeLabel.setText("CLOSED");
            cardRoot.setOpacity(0.7);
            bookNowButton.setDisable(true);
            return;
        }

        statusBadgeLabel.getStyleClass().add("badge-open");
        statusBadgeLabel.setText("OPEN");
        cardRoot.setOpacity(1.0);
        bookNowButton.setDisable(false);
    }

    private void applyBusType(TripDTO trip) {
        String busType = readOptionalString(trip, "getBusType");
        if (isBlank(busType)) {
            busTypeLabel.setText("");
            busTypeLabel.setManaged(false);
            busTypeLabel.setVisible(false);
            return;
        }
        busTypeLabel.setManaged(true);
        busTypeLabel.setVisible(true);
        busTypeLabel.setText(busType.toUpperCase());
    }

    private String readOptionalString(TripDTO trip, String getterName) {
        try {
            Method getter = trip.getClass().getMethod(getterName);
            Object result = getter.invoke(trip);
            return result == null ? null : String.valueOf(result);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void navigateToBooking(TripDTO trip) {
        // MODIFIED
        if (trip == null) {
            return;
        }
        if (onBookNow != null) {
            onBookNow.accept(trip);
            return;
        }
        SceneSwitcher.switchToSeatSelection(trip); // ADDED
    }
}
