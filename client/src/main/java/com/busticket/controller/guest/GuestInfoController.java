package com.busticket.controller.guest;

import com.busticket.dto.BookingDTO;
import com.busticket.dto.TripDTO;
import com.busticket.dto.UserDTO;
import com.busticket.remote.BookingRemote;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.UUID;

public class GuestInfoController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML
    private void onContinue() {
        String name = trim(fullNameField.getText());
        String email = trim(emailField.getText());
        String phone = trim(phoneField.getText());

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Guest details are required.", "Name and email are mandatory.");
            return;
        }

        TripDTO trip = Session.getPendingTrip();
        List<String> seats = Session.getPendingSeatNumbers();
        if (trip == null || trip.getTripId() == null || seats.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Booking Data Missing", "Trip or seat selection is missing.", "Please select trip and seats again.");
            return;
        }

        String generatedPassword = "G-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            UserRemote userRemote = RMIClient.getUserRemote();
            BookingRemote bookingRemote = RMIClient.getBookingRemote();

            UserDTO user = new UserDTO();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(generatedPassword);
            user.setRole("PASSENGER");
            user.setStatus("ACTIVE");

            boolean registered = userRemote.register(user);
            if (!registered) {
                showAlert(Alert.AlertType.WARNING, "Guest Registration Failed", "Email already exists or data is invalid.", "Please use another email or login.");
                return;
            }

            UserDTO createdUser = userRemote.login(email, generatedPassword);
            if (createdUser == null || createdUser.getUserId() == null) {
                showAlert(Alert.AlertType.ERROR, "Guest Account Error", "Unable to authenticate guest account.", "Please try again.");
                return;
            }

            BookingDTO req = new BookingDTO();
            req.setUserId(createdUser.getUserId());
            req.setTripId(trip.getTripId());
            req.setSeatNumbers(seats);
            req.setTotalPrice(trip.getPrice() * seats.size());

            BookingDTO booking = bookingRemote.createBooking(req);
            if (booking == null || booking.getBookingId() == null) {
                showAlert(Alert.AlertType.ERROR, "Booking Failed", "Unable to create booking for guest.", "Please try again.");
                return;
            }

            Session.setCurrentBookingContext(booking.getBookingId(), booking.getTicketCode(), booking.getTotalPrice());
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Guest Booking Failed", "Unable to continue booking.", ex.getMessage());
        }
    }

    // ADDED
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    // ADDED
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
