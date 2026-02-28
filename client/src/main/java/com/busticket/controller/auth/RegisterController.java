package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

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
        updateToggleIcon(toggleButton, false);
    }

    private void togglePasswordField(PasswordField hiddenField, TextField visibleField, Button toggleButton) {
        boolean isShowing = visibleField.isVisible();
        visibleField.setVisible(!isShowing);
        visibleField.setManaged(!isShowing);
        hiddenField.setVisible(isShowing);
        hiddenField.setManaged(isShowing);
        updateToggleIcon(toggleButton, !isShowing);
        if (isShowing) {
            hiddenField.requestFocus();
            hiddenField.positionCaret(hiddenField.getText() == null ? 0 : hiddenField.getText().length());
        } else {
            visibleField.requestFocus();
            visibleField.positionCaret(visibleField.getText() == null ? 0 : visibleField.getText().length());
        }
    }

    private void updateToggleIcon(Button toggleButton, boolean showing) {
        toggleButton.setText("");
        toggleButton.setGraphic(createEyeIcon(showing));
    }

    private Group createEyeIcon(boolean showing) {
        SVGPath eyeOutline = new SVGPath();
        eyeOutline.setContent("M2 12C4.6 7.5 8 5.2 12 5.2C16 5.2 19.4 7.5 22 12C19.4 16.5 16 18.8 12 18.8C8 18.8 4.6 16.5 2 12Z");
        eyeOutline.setFill(Color.TRANSPARENT);
        eyeOutline.setStroke(Color.web("#1e40af"));
        eyeOutline.setStrokeWidth(1.7);

        SVGPath pupil = new SVGPath();
        pupil.setContent("M12 8.8A3.2 3.2 0 1 1 11.99 8.8Z");
        pupil.setFill(Color.web("#1e40af"));

        Group icon = new Group(eyeOutline, pupil);

        if (showing) {
            SVGPath slash = new SVGPath();
            slash.setContent("M4 20L20 4");
            slash.setFill(Color.TRANSPARENT);
            slash.setStroke(Color.web("#1e40af"));
            slash.setStrokeWidth(2.0);
            icon.getChildren().add(slash);
        }

        icon.setScaleX(0.72);
        icon.setScaleY(0.72);
        return icon;
    }
}
