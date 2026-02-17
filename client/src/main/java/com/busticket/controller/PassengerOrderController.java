package com.busticket.controller;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.BookingRequestDTO;
import com.busticket.dto.SeatDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.PassengerFlowContext;
import com.busticket.util.PassengerViewRouter;
import com.busticket.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PassengerOrderController {
    @FXML
    private Label tripSummaryLabel;
    @FXML
    private Label availableSeatsLabel;
    @FXML
    private TextField seatNumbersField;
    @FXML
    private Label orderStatusLabel;
    @FXML
    private GridPane seatGridPane;
    @FXML
    private TableView<BookingDTO> bookingTable;
    @FXML
    private TableColumn<BookingDTO, Long> bookingIdColumn;
    @FXML
    private TableColumn<BookingDTO, String> bookingSeatsColumn;
    @FXML
    private TableColumn<BookingDTO, Double> bookingTotalColumn;
    @FXML
    private TableColumn<BookingDTO, String> bookingStatusColumn;

    private BusTicketRemote busTicketRemote;
    private TripDTO selectedTrip;
    private final Set<String> selectedSeats = new HashSet<>();

    public void initialize() {
        setupBookingTable();
        try {
            busTicketRemote = RMIClient.getBusTicketRemote();
            loadBookingHistory();
        } catch (Exception e) {
            orderStatusLabel.setText("Cannot connect to server.");
            return;
        }

        selectedTrip = PassengerFlowContext.getSelectedTrip();
        if (selectedTrip == null) {
            orderStatusLabel.setText("No trip selected. Go to Routes page.");
            tripSummaryLabel.setText("No trip selected");
            availableSeatsLabel.setText("-");
            return;
        }
        tripSummaryLabel.setText(
                selectedTrip.getOriginCity() + " -> " + selectedTrip.getDestinationCity()
                        + " | " + selectedTrip.getTravelDate()
                        + " | " + selectedTrip.getDepartureTime()
                        + " | $" + selectedTrip.getPrice()
        );
        loadAvailableSeats();
    }

    @FXML
    public void handleCreateOrder() {
        try {
            if (selectedTrip == null) {
                orderStatusLabel.setText("No trip selected.");
                return;
            }
            if (Session.getUser() == null) {
                orderStatusLabel.setText("Please login again.");
                return;
            }
            if (busTicketRemote == null) {
                orderStatusLabel.setText("Server not connected.");
                return;
            }
            List<String> seats = parseSeatNumbers(seatNumbersField.getText());
            if (seats.isEmpty() && !selectedSeats.isEmpty()) {
                seats = selectedSeats.stream().sorted().collect(Collectors.toList());
            }
            if (seats.isEmpty()) {
                orderStatusLabel.setText("Select at least one seat.");
                return;
            }

            BookingRequestDTO request = new BookingRequestDTO();
            request.setUserId(Session.getUser().getUserId());
            request.setTripId(selectedTrip.getTripId());
            request.setSeatNumbers(seats);

            BookingDTO booking = busTicketRemote.createBooking(request);
            if (booking == null) {
                orderStatusLabel.setText("Order creation failed.");
                return;
            }
            PassengerFlowContext.setCurrentBooking(booking);
            orderStatusLabel.setText("Order created: #" + booking.getBookingId());
            loadBookingHistory();
            PassengerViewRouter.open("payment");
        } catch (Exception e) {
            orderStatusLabel.setText("Order error: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToRoutes() {
        PassengerViewRouter.open("routes");
    }

    private void loadAvailableSeats() {
        try {
            List<SeatDTO> seats = busTicketRemote.getAvailableSeats(selectedTrip.getTripId());
            String seatText = seats.stream().map(SeatDTO::getSeatNumber).collect(Collectors.joining(", "));
            availableSeatsLabel.setText(seatText.isBlank() ? "No available seats." : seatText);
            renderSeatGrid(seats, selectedTrip.getTotalSeats());
        } catch (Exception e) {
            availableSeatsLabel.setText("Cannot load seats.");
        }
    }

    private List<String> parseSeatNumbers(String text) {
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private void setupBookingTable() {
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        bookingSeatsColumn.setCellValueFactory(new PropertyValueFactory<>("ticketCode"));
        bookingTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        bookingStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadBookingHistory() {
        try {
            if (busTicketRemote == null || Session.getUser() == null) {
                return;
            }
            List<BookingDTO> bookings = busTicketRemote.getBookingsByUser(Session.getUser().getUserId());
            bookingTable.setItems(FXCollections.observableArrayList(bookings));
        } catch (Exception e) {
            orderStatusLabel.setText("Cannot load booking history.");
        }
    }

    private void renderSeatGrid(List<SeatDTO> availableSeats, int totalSeats) {
        seatGridPane.getChildren().clear();
        selectedSeats.clear();
        seatGridPane.getStyleClass().add("seat-grid");
        Set<String> availableSet = availableSeats.stream().map(SeatDTO::getSeatNumber).collect(Collectors.toSet());
        int seatCount = totalSeats > 0 ? totalSeats : inferSeatCount(availableSet);
        for (int i = 1; i <= seatCount; i++) {
            String seatNumber = "S" + i;
            Button seatButton = new Button(seatNumber);
            seatButton.setPrefWidth(60);
            seatButton.getStyleClass().add("seat-button");
            int col = (i - 1) % 4;
            int row = (i - 1) / 4;
            if (!availableSet.contains(seatNumber)) {
                seatButton.getStyleClass().add("seat-booked");
                seatButton.setDisable(true);
            } else {
                seatButton.setOnAction(event -> toggleSeatSelection(seatButton, seatNumber));
            }
            seatGridPane.add(seatButton, col, row);
        }
    }

    private void toggleSeatSelection(Button seatButton, String seatNumber) {
        if (selectedSeats.contains(seatNumber)) {
            selectedSeats.remove(seatNumber);
            seatButton.getStyleClass().remove("seat-selected");
        } else {
            selectedSeats.add(seatNumber);
            seatButton.getStyleClass().add("seat-selected");
        }
        seatNumbersField.setText(selectedSeats.stream().sorted().collect(Collectors.joining(",")));
    }

    private int inferSeatCount(Set<String> availableSet) {
        int max = 0;
        for (String seat : availableSet) {
            if (seat.startsWith("S")) {
                try {
                    max = Math.max(max, Integer.parseInt(seat.substring(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }
}
