package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Hyperlink guestButton;

    private UserRemote userRemote;

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onLogin() {
        try {
            UserDTO user = userRemote.login(emailField.getText(), passwordField.getText());
            if (user != null) {
                Session.login(user);
                SceneSwitcher.showAppShell(resolveDashboard());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onRegister() {
        SceneSwitcher.showAuth("/com/busticket/view/auth/RegisterView.fxml");
    }

    @FXML
    private void onGuest() {
        Session.loginAsGuest();
        SceneSwitcher.showAppShell("/com/busticket/view/guest/GuestDashboardView.fxml");
    }

    private String resolveDashboard() {
        switch (Session.getRole()) {
            case ADMIN:
                return "/com/busticket/view/admin/AdminDashboardView.fxml";
            case STAFF:
                return "/com/busticket/view/staff/StaffDashboardView.fxml";
            default:
                return "/com/busticket/view/passenger/PassengerDashboardView.fxml";
        }
    }
}
