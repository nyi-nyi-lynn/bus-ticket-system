package com.busticket.controller.guest;

import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;

public class GuestDashboardController {
    @FXML
    private void onSearchTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
    }

    @FXML
    private void onExit() {
        Session.clear();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
