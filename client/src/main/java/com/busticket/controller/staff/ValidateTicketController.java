package com.busticket.controller.staff;

import com.busticket.remote.BookingRemote;
import com.busticket.rmi.RMIClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ValidateTicketController {
    @FXML private TextField ticketCodeField;
    @FXML private Label validationResultLabel;

    private BookingRemote bookingRemote;

    @FXML
    private void initialize() {
        try {
            bookingRemote = RMIClient.getBookingRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onValidate() {
        validationResultLabel.setText("Validated");
    }
}
