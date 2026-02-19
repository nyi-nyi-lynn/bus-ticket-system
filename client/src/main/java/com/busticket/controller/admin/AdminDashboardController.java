package com.busticket.controller.admin;

import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;

public class AdminDashboardController {
    @FXML
    private void onManageUsers() {
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageUsersView.fxml");
    }

    @FXML
    private void onManageBuses() {
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageBusesView.fxml");
    }

    @FXML
    private void onManageRoutes() {
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageRoutesView.fxml");
    }

    @FXML
    private void onManageTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageTripsView.fxml");
    }

    @FXML
    private void onReports() {
        SceneSwitcher.switchContent("/com/busticket/view/admin/ReportsView.fxml");
    }
}
