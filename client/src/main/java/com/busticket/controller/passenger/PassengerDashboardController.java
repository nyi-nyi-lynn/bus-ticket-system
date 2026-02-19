package com.busticket.controller.passenger;

import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;

public class PassengerDashboardController {
    @FXML
    private void onSearchTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
    }

    @FXML
    private void onMyBookings() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/MyBookingsView.fxml");
    }

    @FXML
    private void onProfile() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/ProfileView.fxml");
    }
}
