package com.busticket.controller.admin;

import com.busticket.dto.CreateUserRequest;
import com.busticket.dto.UserDTO;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.UserRemote;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class AddUserDialogController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,15}$");

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private ComboBox<UserStatus> statusCombo;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button createButton;

    private UserRemote userRemote;
    private Long createdByUserId;
    private Consumer<UserDTO> onCreated;
    private final BooleanProperty submitting = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        roleCombo.getItems().setAll(Arrays.asList(Role.ADMIN, Role.STAFF, Role.PASSENGER));
        statusCombo.getItems().setAll(Arrays.asList(UserStatus.ACTIVE, UserStatus.BLOCKED));
        statusCombo.setValue(UserStatus.ACTIVE);

        roleCombo.setPromptText("Select role");

        setupValidation();
    }

    public void setup(UserRemote userRemote, Long createdByUserId, Consumer<UserDTO> onCreated) {
        this.userRemote = userRemote;
        this.createdByUserId = createdByUserId;
        this.onCreated = onCreated;
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    @FXML
    private void onCreateUser() {
        if (userRemote == null) {
            showError("Unable to create user. User service is unavailable.");
            return;
        }

        String validationError = getValidationError(true);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        CreateUserRequest req = new CreateUserRequest();
        req.setCreatedByUserId(createdByUserId);
        req.setName(nameField.getText().trim());
        req.setEmail(emailField.getText().trim().toLowerCase(Locale.ROOT));
        req.setPhone(phoneField.getText().trim());
        req.setRole(roleCombo.getValue());
        req.setStatus(statusCombo.getValue() == null ? UserStatus.ACTIVE : statusCombo.getValue());
        req.setPassword(passwordField.getText());

        setSubmitting(true);

        Task<UserDTO> createTask = new Task<>() {
            @Override
            protected UserDTO call() throws Exception {
                return userRemote.createUser(req);
            }
        };

        createTask.setOnSucceeded(event -> {
            setSubmitting(false);
            hideError();
            UserDTO created = createTask.getValue();
            closeDialog();
            if (onCreated != null && created != null) {
                onCreated.accept(created);
            }
        });

        createTask.setOnFailed(event -> {
            setSubmitting(false);
            Throwable ex = createTask.getException();
            showError(resolveCreateError(ex));
        });

        Thread worker = new Thread(createTask, "create-user-task");
        worker.setDaemon(true);
        worker.start();
    }

    private void setupValidation() {
        BooleanBinding formInvalidBinding = Bindings.createBooleanBinding(
                () -> getValidationError(false) != null,
                nameField.textProperty(),
                emailField.textProperty(),
                phoneField.textProperty(),
                roleCombo.valueProperty(),
                statusCombo.valueProperty(),
                passwordField.textProperty(),
                confirmPasswordField.textProperty()
        );

        createButton.disableProperty().bind(submitting.or(formInvalidBinding));

        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> updateValidationView());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updateValidationView());

        updateValidationView();
    }

    private void updateValidationView() {
        String validationError = getValidationError(false);
        if (validationError == null) {
            hideError();
        } else if (isAnyFieldEdited()) {
            showError(validationError);
        } else {
            hideError();
        }

        boolean showFieldErrors = isAnyFieldEdited();
        markInvalid(nameField, isBlank(nameField.getText()), showFieldErrors);
        markInvalid(emailField, !isEmailValid(emailField.getText()), showFieldErrors);
        markInvalid(phoneField, !isPhoneValid(phoneField.getText()), showFieldErrors);
        markInvalid(roleCombo, roleCombo.getValue() == null, showFieldErrors);
        markInvalid(statusCombo, statusCombo.getValue() == null, showFieldErrors);
        markInvalid(passwordField, !isPasswordValid(passwordField.getText()), showFieldErrors);
        markInvalid(confirmPasswordField, !isConfirmPasswordValid(), showFieldErrors);
    }

    private boolean isAnyFieldEdited() {
        return !isBlank(nameField.getText())
                || !isBlank(emailField.getText())
                || !isBlank(phoneField.getText())
                || roleCombo.getValue() != null
                || !isBlank(passwordField.getText())
                || !isBlank(confirmPasswordField.getText());
    }

    private String getValidationError(boolean includeAdminGuard) {
        if (includeAdminGuard && createdByUserId == null) {
            return "Only logged-in admins can create users.";
        }
        if (isBlank(nameField.getText())) {
            return "Name is required.";
        }
        if (!isEmailValid(emailField.getText())) {
            return "Please enter a valid email address.";
        }
        if (!isPhoneValid(phoneField.getText())) {
            return "Phone must be 7 to 15 digits.";
        }
        if (roleCombo.getValue() == null) {
            return "Please select a role.";
        }
        if (statusCombo.getValue() == null) {
            return "Please select a status.";
        }
        if (!isPasswordValid(passwordField.getText())) {
            return "Password must be at least 8 characters.";
        }
        if (!isConfirmPasswordValid()) {
            return "Password and confirm password do not match.";
        }
        return null;
    }

    private boolean isEmailValid(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    private boolean isPhoneValid(String value) {
        return value != null && PHONE_PATTERN.matcher(value.trim()).matches();
    }

    private boolean isPasswordValid(String value) {
        return value != null && value.length() >= 8;
    }

    private boolean isConfirmPasswordValid() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        if (password == null || confirm == null) {
            return false;
        }
        return !confirm.isBlank() && password.equals(confirm);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void markInvalid(Control control, boolean invalid, boolean show) {
        if (invalid && show) {
            if (!control.getStyleClass().contains("invalid-field")) {
                control.getStyleClass().add("invalid-field");
            }
            return;
        }
        control.getStyleClass().remove("invalid-field");
    }

    private void setSubmitting(boolean submitting) {
        this.submitting.set(submitting);
        cancelButton.setDisable(submitting);
    }

    private void showError(String message) {
        if (message == null || message.isBlank()) {
            hideError();
            return;
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private String resolveCreateError(Throwable throwable) {
        if (throwable == null) {
            return "Unable to create user. Please try again.";
        }
        if (throwable instanceof DuplicateResourceException) {
            if ("EMAIL_EXISTS".equalsIgnoreCase(throwable.getMessage())) {
                return "This email is already registered.";
            }
            return throwable.getMessage();
        }
        if (throwable instanceof ValidationException) {
            return mapValidationMessage(throwable.getMessage());
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return resolveCreateError(cause);
        }
        return "Unable to create user. Please try again.";
    }

    private String mapValidationMessage(String code) {
        if (code == null) {
            return "Invalid request.";
        }
        return switch (code) {
            case "ADMIN_ONLY" -> "Only ADMIN users can create accounts.";
            case "NAME_REQUIRED" -> "Name is required.";
            case "INVALID_EMAIL" -> "Please enter a valid email address.";
            case "INVALID_PHONE" -> "Phone must be 7 to 15 digits.";
            case "INVALID_ROLE" -> "Please select a valid role.";
            case "PASSWORD_TOO_SHORT" -> "Password must be at least 8 characters.";
            default -> "Invalid input. Please review the form.";
        };
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
