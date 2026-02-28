package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button passwordToggleButton;
    @FXML private Hyperlink guestButton;

    private UserRemote userRemote;
    private static final String EYE_ICON = "\uD83D\uDC41";
    private static final String EYE_SLASH_ICON = "\uD83D\uDC41\u0336";

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        setupPasswordToggle(passwordField, passwordVisibleField, passwordToggleButton);
    }

    @FXML
    private void onTogglePasswordVisibility() {
        togglePasswordField(passwordField, passwordVisibleField, passwordToggleButton);
    }

    @FXML
    private void onLogin() {
        try {
            UserDTO user = userRemote.login(emailField.getText(), passwordField.getText());
            if (user == null) {
                showAlert(Alert.AlertType.WARNING, "Login Failed", "Invalid credentials", "Please check your email and password.");
                return;
            }

            String status = user.getStatus() == null ? "" : user.getStatus().trim();
            if ("BLOCKED".equalsIgnoreCase(status)) {
                showAlert(Alert.AlertType.ERROR, "Account Blocked", "Your account has been blocked.", "Please contact support for assistance.");
                return;
            }

            if ("INVALID_PASSWORD".equalsIgnoreCase(status)) {
                showAlert(Alert.AlertType.WARNING, "Login Failed", "Invalid password", "Please check your password and try again.");
                return;
            }

            Session.login(user);
            SceneSwitcher.showAppShell(resolveDashboard());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Unable to sign in.", ex.getMessage());
        }
    }

    @FXML
    private void onRegister() {
        SceneSwitcher.showAuth("/com/busticket/view/auth/RegisterView.fxml");
    }

    @FXML
    private void onGuest() {
        Session.loginAsGuest();
        SceneSwitcher.showAppShell("/com/busticket/view/passenger/SearchTripsView.fxml");
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

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupPasswordToggle(PasswordField hiddenField, TextField visibleField, Button toggleButton) {
        visibleField.textProperty().bindBidirectional(hiddenField.textProperty());
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        hiddenField.setVisible(true);
        hiddenField.setManaged(true);
        toggleButton.setText("Show " + EYE_ICON);
    }

    private void togglePasswordField(PasswordField hiddenField, TextField visibleField, Button toggleButton) {
        boolean isShowing = visibleField.isVisible();
        visibleField.setVisible(!isShowing);
        visibleField.setManaged(!isShowing);
        hiddenField.setVisible(isShowing);
        hiddenField.setManaged(isShowing);
        toggleButton.setText(isShowing ? "Show " + EYE_ICON : "Hide " + EYE_SLASH_ICON);
        if (isShowing) {
            hiddenField.requestFocus();
            hiddenField.positionCaret(hiddenField.getText() == null ? 0 : hiddenField.getText().length());
        } else {
            visibleField.requestFocus();
            visibleField.positionCaret(visibleField.getText() == null ? 0 : visibleField.getText().length());
        }
    }
}
