package com.busticket.controller.passenger;

import com.busticket.dto.TripDTO;
import com.busticket.dto.SeatDTO; // ADDED
import com.busticket.exception.UnauthorizedException;
import com.busticket.remote.BookingRemote;
import com.busticket.remote.SeatRemote; // ADDED
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button; // ADDED
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip; // ADDED
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SeatSelectionController {
    @FXML private GridPane seatGrid;
    @FXML private Label totalPriceLabel;
    @FXML private Label tripLabel;

    private static final int COLS = 4;

    private final Set<String> selectedSeats = new HashSet<>();
    private List<String> availableSeatNumbers = Collections.emptyList();
    private List<SeatDTO> busSeats = Collections.emptyList(); // MODIFIED
    private TripDTO selectedTrip;
    private BookingRemote bookingRemote; // ADDED
    private SeatRemote seatRemote; // ADDED

    @FXML
    private void initialize() {
        Session.clearPendingSelection();
        try {
            bookingRemote = RMIClient.getBookingRemote(); // ADDED
            seatRemote = RMIClient.getSeatRemote(); // ADDED
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Connection Failed", "Unable to initialize booking service.", ex.getMessage());
        }
        updateTotal();
    }

    public void setTripData(TripDTO trip) {
        this.selectedTrip = trip;
        loadSeatsForTrip();
    }

    private void loadSeatsForTrip() {
        selectedSeats.clear();
        if (tripLabel != null && selectedTrip != null) {
            String label = selectedTrip.getOriginCity() + " -> " + selectedTrip.getDestinationCity();
            tripLabel.setText(label);
        }
        try {
            loadSeats(selectedTrip); // MODIFIED
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Seat Loading Failed", "Unable to load seat layout.", ex.getMessage());
            availableSeatNumbers = Collections.emptyList();
            busSeats = Collections.emptyList();
            renderSeatGrid(busSeats, availableSeatNumbers);
        }
        updateTotal();
    }

    // MODIFIED
    public void loadSeats(TripDTO trip) throws Exception {
        this.selectedTrip = trip;
        if (trip == null || trip.getTripId() == null || trip.getBusId() == null) {
            availableSeatNumbers = Collections.emptyList();
            busSeats = Collections.emptyList();
            renderSeatGrid(busSeats, availableSeatNumbers);
            return;
        }

        Long busId = trip.getBusId();
        Long tripId = trip.getTripId();

        List<SeatDTO> seatsByBus = seatRemote == null ? Collections.emptyList() : seatRemote.getSeatsByBusId(busId);
        List<String> availableSeats = bookingRemote == null ? Collections.emptyList() : bookingRemote.getAvailableSeatNumbers(tripId);

        busSeats = seatsByBus == null ? Collections.emptyList() : seatsByBus;
        availableSeatNumbers = availableSeats == null ? Collections.emptyList() : availableSeats;

        renderSeatGrid(busSeats, availableSeatNumbers);
    }

    // ADDED
    private void renderSeatGrid(List<SeatDTO> allSeats, List<String> availableSeats) {
        seatGrid.getChildren().clear();
        Set<String> availableSet = new HashSet<>(availableSeats == null ? Collections.emptyList() : availableSeats);
        List<SeatDTO> orderedSeats = allSeats == null ? Collections.emptyList() : allSeats;

        for (int i = 0; i < orderedSeats.size(); i++) {
            String seatNumber = orderedSeats.get(i).getSeatNumber();
            Button seatBtn = new Button(seatNumber);
            seatBtn.getStyleClass().add("seat-button");
            seatBtn.setUserData(seatNumber);
            seatBtn.setMaxWidth(Double.MAX_VALUE);

            if (availableSet.contains(seatNumber)) {
                seatBtn.getStyleClass().add("seat-available");
                seatBtn.setOnAction(e -> toggleSelection(seatBtn));
            } else {
                seatBtn.getStyleClass().add("seat-unavailable");
                seatBtn.setDisable(true);
                seatBtn.setTooltip(new Tooltip("Already booked"));
            }

            seatGrid.add(seatBtn, i % COLS, i / COLS);
        }
    }

    // ADDED
    private void toggleSelection(Button seatBtn) {
        String seatLabel = String.valueOf(seatBtn.getUserData());
        if (selectedSeats.contains(seatLabel)) {
            selectedSeats.remove(seatLabel);
            seatBtn.getStyleClass().remove("seat-selected");
        } else {
            selectedSeats.add(seatLabel);
            if (!seatBtn.getStyleClass().contains("seat-selected")) {
                seatBtn.getStyleClass().add("seat-selected");
            }
        }
        updateTotal();
    }

    private void updateTotal() {
        double price = selectedTrip == null ? 0.0 : selectedTrip.getPrice();
        double total = selectedSeats.size() * price;
        totalPriceLabel.setText("MMK " + String.format("%.2f", total));
    }

    @FXML
    private void onContinue() {
        if (selectedTrip == null || selectedTrip.getTripId() == null) {
            showAlert(Alert.AlertType.WARNING, "Trip Required", "Trip information is missing.", "Please return and select a trip again.");
            return;
        }
        if (selectedSeats.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Seat Required", "No seats selected.", "Please select at least one seat.");
            return;
        }

        List<String> seatNumbers = new ArrayList<>(selectedSeats);
        seatNumbers.sort(Comparator.naturalOrder());

        List<SeatDTO> selectedSeatDTOs = busSeats.stream()
                .filter(seat -> selectedSeats.contains(seat.getSeatNumber()))
                .sorted(Comparator.comparing(SeatDTO::getSeatNumber))
                .toList();

        Session.setPendingSelection(selectedTrip, selectedSeatDTOs, seatNumbers);
        try {
            selectSeat();
        } catch (UnauthorizedException ex) {
            showLoginRequiredAndRedirect(ex.getMessage());
            return;
        }
        SceneSwitcher.switchToBookingSummary();
    }

    // MODIFIED
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void selectSeat() throws UnauthorizedException {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            throw new UnauthorizedException("Please login to continue booking");
        }
    }

    private void showLoginRequiredAndRedirect(String message) {
        showAlert(
                Alert.AlertType.WARNING,
                "Login Required",
                "Please login to continue booking",
                message == null || message.isBlank() ? "Please login to continue booking" : message
        );
        Session.clearPendingSelection();
        Session.clearBookingContext();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
