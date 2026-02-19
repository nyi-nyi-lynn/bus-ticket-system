package com.busticket.controller.staff;

import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;

public class StaffDashboardController {
    @FXML
    private void onViewTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffTripsView.fxml");
    }

    @FXML
    private void onManageBookings() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffBookingsView.fxml");
    }

    @FXML
    private void onValidateTicket() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/ValidateTicketView.fxml");
    }
}
