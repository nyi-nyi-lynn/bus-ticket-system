package com.busticket.controller.passenger;

import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TicketSuccessController {
    @FXML private Label ticketCodeLabel;

    @FXML
    private void onDownload() {
        // UI hook for download action
    }

    @FXML
    private void onBack() {
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/guest/GuestDashboardView.fxml");
        } else {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PassengerDashboardView.fxml");
        }
    }
}
