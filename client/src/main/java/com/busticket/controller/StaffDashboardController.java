package com.busticket.controller;

import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.Navigator;
import com.busticket.util.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class StaffDashboardController {
    @FXML
    private TextField ticketCodeField;
    @FXML
    private Label resultLabel;

    private BusTicketRemote busTicketRemote;

    public void initialize() {
        try {
            busTicketRemote = RMIClient.getBusTicketRemote();
            resultLabel.setText("Ready to validate ticket.");
        } catch (Exception e) {
            resultLabel.setText("Cannot connect to server.");
        }
    }

    @FXML
    public void handleValidateTicket() {
        try {
            Long staffId = Session.getUser() == null ? null : Session.getUser().getUserId();
            boolean valid = busTicketRemote.validateTicket(ticketCodeField.getText().trim(), staffId);
            resultLabel.setText(valid ? "Ticket is valid." : "Ticket invalid / already used.");
            resultLabel.getStyleClass().removeAll("status-success", "status-error");
            resultLabel.getStyleClass().add(valid ? "status-success" : "status-error");
        } catch (Exception e) {
            resultLabel.setText("Validation error: " + e.getMessage());
            resultLabel.getStyleClass().removeAll("status-success", "status-error");
            resultLabel.getStyleClass().add("status-error");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Navigator.switchScene(getStage(event), "/com/busticket/view/auth/login.fxml");
    }

    private Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
