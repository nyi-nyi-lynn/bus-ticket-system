package com.busticket.controller.shell;

import com.busticket.enums.Role;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class AppShellController {
    @FXML private StackPane contentHost;

    @FXML private Button navDashboard;
    @FXML private Button navSearchTrips;
    @FXML private Button navMyBookings;
    @FXML private Button navProfile;
    @FXML private Button navManageUsers;
    @FXML private Button navManageBuses;
    @FXML private Button navManageRoutes;
    @FXML private Button navManageTrips;
    @FXML private Button navReports;
    @FXML private Button navStaffTrips;
    @FXML private Button navStaffBookings;
    @FXML private Button navValidateTicket;
    @FXML private Button navLogout;

    @FXML private Text roleBadge;

    @FXML
    private void initialize() {
        applyRoleNavigation();
    }

    public void setContent(Node node) {
        contentHost.getChildren().setAll(node);
    }

    private void applyRoleNavigation() {
        Role role = Session.getRole();
        boolean guest = Session.isGuest();

        setAllNavVisible(false);
        navLogout.setVisible(true);

        if (guest) {
            navDashboard.setVisible(true);
            navSearchTrips.setVisible(true);
            roleBadge.setText("Guest");
            return;
        }

        if (role == Role.PASSENGER) {
            navDashboard.setVisible(true);
            navSearchTrips.setVisible(true);
            navMyBookings.setVisible(true);
            navProfile.setVisible(true);
            roleBadge.setText("Passenger");
        } else if (role == Role.ADMIN) {
            navDashboard.setVisible(true);
            navManageUsers.setVisible(true);
            navManageBuses.setVisible(true);
            navManageRoutes.setVisible(true);
            navManageTrips.setVisible(true);
            navReports.setVisible(true);
            roleBadge.setText("Admin");
        } else if (role == Role.STAFF) {
            navDashboard.setVisible(true);
            navStaffTrips.setVisible(true);
            navStaffBookings.setVisible(true);
            navValidateTicket.setVisible(true);
            roleBadge.setText("Staff");
        }
    }

    private void setAllNavVisible(boolean visible) {
        navDashboard.setVisible(visible);
        navSearchTrips.setVisible(visible);
        navMyBookings.setVisible(visible);
        navProfile.setVisible(visible);
        navManageUsers.setVisible(visible);
        navManageBuses.setVisible(visible);
        navManageRoutes.setVisible(visible);
        navManageTrips.setVisible(visible);
        navReports.setVisible(visible);
        navStaffTrips.setVisible(visible);
        navStaffBookings.setVisible(visible);
        navValidateTicket.setVisible(visible);
        navLogout.setVisible(visible);
    }

    @FXML
    private void onDashboard() {
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/guest/GuestDashboardView.fxml");
            return;
        }
        Role role = Session.getRole();
        if (role == Role.ADMIN) {
            SceneSwitcher.switchContent("/com/busticket/view/admin/AdminDashboardView.fxml");
        } else if (role == Role.STAFF) {
            SceneSwitcher.switchContent("/com/busticket/view/staff/StaffDashboardView.fxml");
        } else {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/PassengerDashboardView.fxml");
        }
    }

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

    @FXML
    private void onStaffTrips() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffTripsView.fxml");
    }

    @FXML
    private void onStaffBookings() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffBookingsView.fxml");
    }

    @FXML
    private void onValidateTicket() {
        SceneSwitcher.switchContent("/com/busticket/view/staff/ValidateTicketView.fxml");
    }

    @FXML
    private void onLogout() {
        Session.clear();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
