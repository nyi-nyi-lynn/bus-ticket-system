package com.busticket.controller.auth;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.Locale;
import java.util.regex.Pattern;

public class RegisterController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private TextField nameField;
    @FXML private Label nameErrorLabel;
    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;
    @FXML private TextField phoneField;
    @FXML private Label phoneErrorLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button passwordToggleButton;
    @FXML private Label passwordErrorLabel;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button confirmPasswordToggleButton;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private Label formErrorLabel;
    @FXML private Button registerButton;

    private UserRemote userRemote;
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private boolean nameTouched;
    private boolean emailTouched;
    private boolean phoneTouched;
    private boolean passwordTouched;
    private boolean confirmPasswordTouched;

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        setupPasswordToggle(passwordField, passwordVisibleField, passwordToggleButton);
        setupPasswordToggle(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);
        registerButton.disableProperty().bind(formValid.not());

        addValidationListeners();
        addTouchListeners();
        hideFieldValidationErrors();
        validateForm(false);
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
        clearFormError();
        if (!validateForm(true)) {
            return;
        }
        try {
            UserDTO user = new UserDTO();
            user.setName(trimmed(nameField.getText()));
            user.setEmail(trimmed(emailField.getText()).toLowerCase(Locale.ROOT));
            user.setPhone(trimmed(phoneField.getText()));
            user.setPassword(passwordField.getText());
            userRemote.register(user);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Registration Successful");
            success.setHeaderText("Account created successfully.");
            success.setContentText("You can now login with your new account.");
            success.showAndWait();

            SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Register Failed", "Unable to create account.", ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        SceneSwitcher.showAuth("/com/busticket/view/auth/LoginView.fxml");
    }

    private void addValidationListeners() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        confirmPasswordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
    }

    private void addTouchListeners() {
        nameField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                nameTouched = true;
                validateForm(false);
            }
        });
        emailField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                emailTouched = true;
                validateForm(false);
            }
        });
        phoneField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                phoneTouched = true;
                validateForm(false);
            }
        });
        passwordField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                passwordTouched = true;
                validateForm(false);
            }
        });
        passwordVisibleField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                passwordTouched = true;
                validateForm(false);
            }
        });
        confirmPasswordField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                confirmPasswordTouched = true;
                validateForm(false);
            }
        });
        confirmPasswordVisibleField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                confirmPasswordTouched = true;
                validateForm(false);
            }
        });
    }

    private void onInputChanged() {
        clearFormError();
        validateForm(false);
    }

    private boolean validateForm(boolean showAllErrors) {
        String name = trimmed(nameField.getText());
        String email = trimmed(emailField.getText());
        String phone = trimmed(phoneField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        String nameError = name.isBlank() ? "Name is required." : "";
        String emailError = email.isBlank()
                ? "Email is required."
                : (EMAIL_PATTERN.matcher(email).matches() ? "" : "Invalid email format.");
        String phoneError = phone.isBlank() ? "Phone is required." : "";
        String passwordError = password.isBlank()
                ? "Password is required."
                : (password.length() >= 8 ? "" : "Password must be at least 8 characters.");

        String confirmError;
        if (confirmPassword.isBlank()) {
            confirmError = "Confirm password is required.";
        } else if (!password.equals(confirmPassword)) {
            confirmError = "Passwords do not match.";
        } else {
            confirmError = "";
        }

        updateError(nameErrorLabel, (showAllErrors || nameTouched) ? nameError : "");
        updateError(emailErrorLabel, (showAllErrors || emailTouched) ? emailError : "");
        updateError(phoneErrorLabel, (showAllErrors || phoneTouched) ? phoneError : "");
        updateError(passwordErrorLabel, (showAllErrors || passwordTouched) ? passwordError : "");
        updateError(confirmPasswordErrorLabel, (showAllErrors || confirmPasswordTouched) ? confirmError : "");

        boolean valid = nameError.isEmpty()
                && emailError.isEmpty()
                && phoneError.isEmpty()
                && passwordError.isEmpty()
                && confirmError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private void hideFieldValidationErrors() {
        updateError(nameErrorLabel, "");
        updateError(emailErrorLabel, "");
        updateError(phoneErrorLabel, "");
        updateError(passwordErrorLabel, "");
        updateError(confirmPasswordErrorLabel, "");
    }

    private void updateError(Label label, String message) {
        boolean hasError = message != null && !message.isBlank();
        label.setText(hasError ? message : "");
        label.setVisible(hasError);
        label.setManaged(hasError);
    }

    private void clearFormError() {
        updateError(formErrorLabel, "");
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
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
