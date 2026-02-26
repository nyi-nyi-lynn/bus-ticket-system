package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button passwordToggleButton;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button confirmPasswordToggleButton;

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
        setupPasswordToggle(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);
    }

    @FXML
    private void onTogglePasswordVisibility() {
        togglePasswordField(passwordField, passwordVisibleField, passwordToggleButton);
    }

    @FXML
    private void onToggleConfirmPasswordVisibility() {
        togglePasswordField(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);
    }

    @FXML
    private void onRegister() {
        try {
            UserDTO user = new UserDTO();
            user.setName(nameField.getText());
            user.setEmail(emailField.getText());
            user.setPhone(phoneField.getText());
            user.setPassword(passwordField.getText());
            userRemote.register(user);
            SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void onBack() {
        SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
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
