package com.busticket.controller.passenger;

import com.busticket.dto.PaymentDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.remote.PaymentRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
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
            throw new RuntimeException(ex);
        }
        paymentMethodCombo.getItems().setAll(PaymentMethod.values());
    }

    @FXML
    private void onConfirm() {
        try {
            PaymentDTO payment = new PaymentDTO();
            PaymentMethod method = paymentMethodCombo.getValue();
            if (method != null) {
                payment.setPaymentMethod(method.name());
            }
            paymentRemote.createPayment(payment);
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketSuccessView.fxml");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
