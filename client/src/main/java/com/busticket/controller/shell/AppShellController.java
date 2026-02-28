package com.busticket.controller.shell;

import com.busticket.enums.Role;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.util.List;

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
    @FXML private Button navBackToLogin;

    @FXML private Text roleBadge;
    private List<Button> navButtons;

    @FXML
    private void initialize() {
        navButtons = List.of(
                navDashboard, navSearchTrips, navMyBookings, navProfile,
                navManageUsers, navManageBuses, navManageRoutes, navManageTrips,
                navReports, navStaffTrips, navStaffBookings, navValidateTicket, navLogout,navBackToLogin
        );
        applyRoleNavigation();
        setDefaultActiveNav();
    }

    public void setContent(Node node) {
        contentHost.getChildren().setAll(node);
    }

    private void applyRoleNavigation() {
        Role role = Session.getRole();
        boolean guest = Session.isGuest();

        setAllNavVisible(false);


        if (guest) {
            setNavVisibility(navSearchTrips, true);
            setNavVisibility(navBackToLogin, true);
            roleBadge.setText("Guest");
            return;
        }

        if (role == Role.PASSENGER) {
            setNavVisibility(navDashboard, true);
            setNavVisibility(navSearchTrips, true);
            setNavVisibility(navMyBookings, true);
            setNavVisibility(navProfile, true);
            setNavVisibility(navLogout, true);
            roleBadge.setText("Passenger");
        } else if (role == Role.ADMIN) {
            setNavVisibility(navDashboard, true);
            setNavVisibility(navManageUsers, true);
            setNavVisibility(navManageBuses, true);
            setNavVisibility(navManageRoutes, true);
            setNavVisibility(navManageTrips, true);
            setNavVisibility(navReports, true);
            setNavVisibility(navLogout, true);
            roleBadge.setText("Admin");
        } else if (role == Role.STAFF) {
            setNavVisibility(navDashboard, true);
            setNavVisibility(navStaffTrips, true);
            setNavVisibility(navStaffBookings, true);
            setNavVisibility(navValidateTicket, true);
            roleBadge.setText("Staff");
        }
    }

    private void setAllNavVisible(boolean visible) {
        for (Button navButton : navButtons) {
            setNavVisibility(navButton, visible);
        }
    }

    private void setNavVisibility(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }

    private void setDefaultActiveNav() {
        for (Button navButton : navButtons) {
            if (navButton.isVisible() && navButton != navLogout) {
                setActiveNav(navButton);
                return;
            }
        }
    }

    private void setActiveNav(Button activeButton) {
        for (Button navButton : navButtons) {
            navButton.getStyleClass().remove("active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onDashboard() {
        setActiveNav(navDashboard);
        if (Session.isGuest()) {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
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
        setActiveNav(navSearchTrips);
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
    }

    @FXML
    private void onMyBookings() {
        setActiveNav(navMyBookings);
        SceneSwitcher.switchContent("/com/busticket/view/passenger/MyBookingsView.fxml");
    }

    @FXML
    private void onProfile() {
        setActiveNav(navProfile);
        SceneSwitcher.switchContent("/com/busticket/view/passenger/PassengerProfile.fxml");
    }

    @FXML
    private void onManageUsers() {
        setActiveNav(navManageUsers);
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageUsersView.fxml");
    }

    @FXML
    private void onManageBuses() {
        setActiveNav(navManageBuses);
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageBusesView.fxml");
    }

    @FXML
    private void onManageRoutes() {
        setActiveNav(navManageRoutes);
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageRoutesView.fxml");
    }

    @FXML
    private void onManageTrips() {
        setActiveNav(navManageTrips);
        SceneSwitcher.switchContent("/com/busticket/view/admin/ManageTripsView.fxml");
    }

    @FXML
    private void onReports() {
        setActiveNav(navReports);
        SceneSwitcher.switchContent("/com/busticket/view/admin/ReportsView.fxml");
    }

    @FXML
    private void onStaffTrips() {
        setActiveNav(navStaffTrips);
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffTripsView.fxml");
    }

    @FXML
    private void onStaffBookings() {
        setActiveNav(navStaffBookings);
        SceneSwitcher.switchContent("/com/busticket/view/staff/StaffBookingsView.fxml");
    }

    @FXML
    private void onValidateTicket() {
        setActiveNav(navValidateTicket);
        SceneSwitcher.switchContent("/com/busticket/view/staff/ValidateTicketView.fxml");
    }

    @FXML
    private void onLogout() {
        Session.clear();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
