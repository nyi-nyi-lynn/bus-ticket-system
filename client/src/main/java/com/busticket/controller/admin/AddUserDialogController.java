package com.busticket.controller.admin;

import com.busticket.dto.CreateUserRequest;
import com.busticket.dto.UserDTO;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.UserRemote;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AddUserDialogController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private VBox dialogRoot;

    @FXML private TextField nameField;
    @FXML private Label nameErrorLabel;

    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;

    @FXML private TextField phoneField;
    @FXML private Label phoneErrorLabel;

    @FXML private ComboBox<String> roleCombo;
    @FXML private Label roleErrorLabel;

    @FXML private ComboBox<String> statusCombo;
    @FXML private Label statusErrorLabel;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button passwordToggleButton;
    @FXML private Label passwordErrorLabel;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button confirmPasswordToggleButton;
    @FXML private Label confirmPasswordErrorLabel;

    @FXML private Label formErrorLabel;
    @FXML private Button createUserButton;
    @FXML private ProgressIndicator progressIndicator;

    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty creating = new SimpleBooleanProperty(false);

    private UserRemote userRemote;
    private Long currentAdminUserId;
    private Consumer<UserDTO> onCreateSuccess;
    private boolean submissionAttempted;

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(List.of("ADMIN", "PASSENGER")));
        statusCombo.setItems(FXCollections.observableArrayList(List.of("ACTIVE", "BLOCKED")));
        statusCombo.setValue("ACTIVE");

        createUserButton.disableProperty().bind(formValid.not().or(creating));
        dialogRoot.disableProperty().bind(creating);
        progressIndicator.visibleProperty().bind(creating);
        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());
        setupPasswordToggle(passwordField, passwordVisibleField, passwordToggleButton);
        setupPasswordToggle(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);

        addValidationListeners();
        hideFieldValidationErrors();
        validateForm(false);
    }

    public void configure(UserRemote userRemote, Long currentAdminUserId, Consumer<UserDTO> onCreateSuccess) {
        this.userRemote = userRemote;
        this.currentAdminUserId = currentAdminUserId;
        this.onCreateSuccess = onCreateSuccess;
    }

    @FXML
    private void onCreateUser() {
        submissionAttempted = true;
        clearFormError();
        if (!validateForm(true)) {
            return;
        }
        if (userRemote == null || currentAdminUserId == null) {
            setFormError("User service is not available. Please reopen this dialog.");
            return;
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setRequestedByUserId(currentAdminUserId);
        request.setName(nameField.getText().trim());
        request.setEmail(emailField.getText().trim().toLowerCase(Locale.ROOT));
        request.setPhone(phoneField.getText().trim());
        request.setRole(roleCombo.getValue());
        request.setStatus(statusCombo.getValue());
        request.setPassword(passwordField.getText());

        Task<UserDTO> createTask = new Task<>() {
            @Override
            protected UserDTO call() throws Exception {
                return userRemote.createUser(request);
            }
        };

        createTask.setOnSucceeded(event -> {
            creating.set(false);
            UserDTO created = createTask.getValue();
            if (onCreateSuccess != null) {
                onCreateSuccess.accept(created);
            }
            closeDialog();
        });
        createTask.setOnFailed(event -> {
            creating.set(false);
            handleCreateFailure(createTask.getException());
        });

        creating.set(true);
        startBackgroundTask(createTask, "create-user-task");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    @FXML
    private void onTogglePasswordVisibility() {
        togglePasswordField(passwordField, passwordVisibleField, passwordToggleButton);
    }

    @FXML
    private void onToggleConfirmPasswordVisibility() {
        togglePasswordField(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);
    }

    private void addValidationListeners() {
        nameField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        emailField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        phoneField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        roleCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        statusCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        confirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
    }

    private void onInputChanged() {
        clearFormError();
        validateForm(submissionAttempted);
    }

    private boolean validateForm(boolean showErrors) {
        String name = safeTrim(nameField.getText());
        String email = safeTrim(emailField.getText());
        String phone = safeTrim(phoneField.getText());
        String role = roleCombo.getValue();
        String status = statusCombo.getValue();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        String nameError = name.isBlank() ? "Name is required." : "";
        String emailError = email.isBlank()
                ? "Email is required."
                : (EMAIL_PATTERN.matcher(email).matches() ? "" : "Invalid email format.");
        String phoneError = phone.isBlank() ? "Phone is required." : "";
        String roleError = role == null || role.isBlank() ? "Role is required." : "";
        String statusError = status == null || status.isBlank() ? "Status is required." : "";
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

        if (showErrors) {
            updateError(nameErrorLabel, nameError);
            updateError(emailErrorLabel, emailError);
            updateError(phoneErrorLabel, phoneError);
            updateError(roleErrorLabel, roleError);
            updateError(statusErrorLabel, statusError);
            updateError(passwordErrorLabel, passwordError);
            updateError(confirmPasswordErrorLabel, confirmError);
        } else {
            hideFieldValidationErrors();
        }

        boolean valid = nameError.isEmpty()
                && emailError.isEmpty()
                && phoneError.isEmpty()
                && roleError.isEmpty()
                && statusError.isEmpty()
                && passwordError.isEmpty()
                && confirmError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private void hideFieldValidationErrors() {
        updateError(nameErrorLabel, "");
        updateError(emailErrorLabel, "");
        updateError(phoneErrorLabel, "");
        updateError(roleErrorLabel, "");
        updateError(statusErrorLabel, "");
        updateError(passwordErrorLabel, "");
        updateError(confirmPasswordErrorLabel, "");
    }

    private void handleCreateFailure(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof DuplicateResourceException) {
            updateError(emailErrorLabel, "Email already exists.");
            setFormError("Please use a different email.");
            return;
        }

        if (root instanceof ValidationException validationException) {
            String message = mapValidationMessage(validationException.getMessage());
            setFormError(message);
            return;
        }

        if (root instanceof RemoteException) {
            setFormError("Server connection failed while creating user.");
            return;
        }

        setFormError("Unable to create user right now. Please try again.");
    }

    private String mapValidationMessage(String code) {
        if (code == null || code.isBlank()) {
            return "Validation failed.";
        }
        return switch (code) {
            case "FORBIDDEN_ONLY_ADMIN" -> "Only ACTIVE ADMIN users can create accounts.";
            case "EMAIL_EXISTS" -> "Email already exists.";
            case "INVALID_EMAIL" -> "Email format is invalid.";
            case "PASSWORD_TOO_SHORT" -> "Password must be at least 8 characters.";
            case "NAME_TOO_LONG" -> "Name is too long.";
            case "NAME_REQUIRED" -> "Name is required.";
            case "EMAIL_REQUIRED" -> "Email is required.";
            case "PHONE_REQUIRED" -> "Phone is required.";
            case "PASSWORD_REQUIRED" -> "Password is required.";
            case "ROLE_REQUIRED" -> "Role is required.";
            case "INVALID_ROLE" -> "Role is invalid.";
            case "INVALID_STATUS" -> "Status is invalid.";
            case "MISSING_ADMIN_CONTEXT" -> "Admin session is missing. Please sign in again.";
            case "CREATE_USER_FAILED" -> "Could not create user due to a server error.";
            default -> "Validation failed. Please review the form.";
        };
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void updateError(Label label, String message) {
        boolean hasError = message != null && !message.isBlank();
        label.setText(hasError ? message : "");
        label.setVisible(hasError);
        label.setManaged(hasError);
    }

    private void setFormError(String message) {
        updateError(formErrorLabel, message == null ? "" : message);
    }

    private void clearFormError() {
        updateError(formErrorLabel, "");
    }

    private void closeDialog() {
        if (createUserButton != null && createUserButton.getScene() != null) {
            Stage stage = (Stage) createUserButton.getScene().getWindow();
            stage.close();
        }
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
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
