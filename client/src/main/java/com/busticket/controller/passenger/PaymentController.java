package com.busticket.controller.passenger;

import com.busticket.dto.PaymentDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.enums.PaymentStatus;
import com.busticket.remote.PaymentRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PaymentController {
    @FXML private VBox summaryBox;
    @FXML private ComboBox<PaymentMethod> paymentMethodCombo;

    private PaymentRemote paymentRemote;

    @FXML
    private void initialize() {
        try {
            paymentRemote = RMIClient.getPaymentRemote();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Payment service unavailable.", ex.getMessage());
        }
        paymentMethodCombo.getItems().setAll(PaymentMethod.values());
        paymentMethodCombo.getSelectionModel().select(PaymentMethod.CARD);
        renderSummary();
    }

    @FXML
    private void onConfirm() {
        Long bookingId = Session.getCurrentBookingId();
        Double totalAmount = Session.getCurrentBookingAmount();
        if (bookingId == null || totalAmount == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Booking", "Booking context not found.", "Please create booking again.");
            return;
        }
        try {
            PaymentDTO payment = new PaymentDTO();
            payment.setBookingId(bookingId);
            payment.setPaidAmount(totalAmount);
            payment.setPaymentStatus(PaymentStatus.PAID.name());
            PaymentMethod method = paymentMethodCombo.getValue();
            if (method != null) {
                payment.setPaymentMethod(method.name());
            }
            PaymentDTO created = paymentRemote.processPayment(payment); // MODIFIED
            if (created == null || created.getPaymentId() == null) {
                showAlert(Alert.AlertType.ERROR, "Payment Failed", "Unable to confirm payment.", "Please try again.");
                return;
            }
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketSuccessView.fxml");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Payment Failed", "Unable to confirm payment.", ex.getMessage());
        }
    }

    // ADDED
    private void renderSummary() {
        if (summaryBox == null) {
            return;
        }
        summaryBox.getChildren().clear();
        String ticket = Session.getCurrentTicketCode();
        Double amount = Session.getCurrentBookingAmount();
        summaryBox.getChildren().add(new Label("Booking ID: " + Session.getCurrentBookingId()));
        summaryBox.getChildren().add(new Label("Ticket Code: " + (ticket == null ? "-" : ticket)));
        summaryBox.getChildren().add(new Label("Total: " + String.format("$%.2f", amount == null ? 0.0 : amount)));
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
