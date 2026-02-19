package com.busticket.controller.passenger;

import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

import java.util.HashSet;
import java.util.Set;

public class SeatSelectionController {
    @FXML private GridPane seatGrid;
    @FXML private Label totalPriceLabel;
    @FXML private Label tripLabel;

    private static final int ROWS = 10;
    private static final int COLS = 4;
    private static final double SEAT_PRICE = 12.50;

    private final Set<String> selectedSeats = new HashSet<>();
    private final Set<String> bookedSeats = Set.of("1A", "1B", "2C", "5D", "6A");

    @FXML
    private void initialize() {
        buildSeatGrid();
        updateTotal();
    }

    private void buildSeatGrid() {
        seatGrid.getChildren().clear();
        for (int row = 1; row <= ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                String seatLabel = row + String.valueOf((char) ('A' + col));
                ToggleButton seatButton = new ToggleButton(seatLabel);
                seatButton.getStyleClass().add("seat-button");
                seatButton.setUserData(seatLabel);

                if (bookedSeats.contains(seatLabel)) {
                    seatButton.setDisable(true);
                    seatButton.getStyleClass().add("seat-booked");
                } else {
                    seatButton.setOnAction(event -> toggleSeat(seatButton));
                }

                seatGrid.add(seatButton, col, row - 1);
            }
        }
    }

    private void toggleSeat(ToggleButton seatButton) {
        String seatLabel = String.valueOf(seatButton.getUserData());
        if (seatButton.isSelected()) {
            selectedSeats.add(seatLabel);
            if (!seatButton.getStyleClass().contains("seat-selected")) {
                seatButton.getStyleClass().add("seat-selected");
            }
        } else {
            selectedSeats.remove(seatLabel);
            seatButton.getStyleClass().remove("seat-selected");
        }
        updateTotal();
    }

    private void updateTotal() {
        double total = selectedSeats.size() * SEAT_PRICE;
        totalPriceLabel.setText(String.format("$%.2f", total));
    }

    @FXML
    private void onContinue() {
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/guest/GuestInfoView.fxml");
        } else {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
        }
    }
}
