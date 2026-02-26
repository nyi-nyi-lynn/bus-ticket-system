package com.busticket.controller.admin;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class EditUserDialogController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private VBox dialogRoot;
    @FXML private Label dialogTitleLabel;
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
    @FXML private Label formErrorLabel;
    @FXML private Button saveChangesButton;
    @FXML private ProgressIndicator progressIndicator;

    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty formChanged = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private UserRemote userRemote;
    private Consumer<UserDTO> onSaveSuccess;
    private UserDTO originalUser;
    private boolean submissionAttempted;

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(List.of("ADMIN", "STAFF", "PASSENGER")));
        statusCombo.setItems(FXCollections.observableArrayList(List.of("ACTIVE", "BLOCKED")));

        saveChangesButton.disableProperty().bind(formValid.not().or(formChanged.not()).or(saving));
        dialogRoot.disableProperty().bind(saving);
        progressIndicator.visibleProperty().bind(saving);
        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());

        addValidationListeners();
        hideFieldValidationErrors();
    }

    public void configure(UserRemote userRemote, UserDTO user, Consumer<UserDTO> onSaveSuccess) {
        this.userRemote = userRemote;
        this.onSaveSuccess = onSaveSuccess;
        this.originalUser = copyUser(user);
        populateForm(user);
        updateChangedState();
        validateForm(false);
    }

    @FXML
    private void onSaveChanges() {
        submissionAttempted = true;
        clearFormError();
        if (!validateForm(true)) {
            return;
        }
        if (!formChanged.get()) {
            setFormError("No changes to save.");
            return;
        }
        if (userRemote == null || originalUser == null || originalUser.getUserId() == null) {
            setFormError("User service is unavailable. Please reopen this dialog.");
            return;
        }

        UserDTO request = buildUpdatedUser();
        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return userRemote.updateUser(request);
            }
        };

        updateTask.setOnSucceeded(event -> {
            saving.set(false);
            Boolean updated = updateTask.getValue();
            if (Boolean.TRUE.equals(updated)) {
                if (onSaveSuccess != null) {
                    onSaveSuccess.accept(request);
                }
                closeDialog();
            } else {
                setFormError("Unable to save changes. Email may already exist or data is invalid.");
            }
        });
        updateTask.setOnFailed(event -> {
            saving.set(false);
            Throwable ex = updateTask.getException();
            if (ex instanceof RemoteException) {
                setFormError("Server connection failed while saving changes.");
            } else {
                setFormError("Failed to save changes. Please try again.");
            }
        });

        saving.set(true);
        startBackgroundTask(updateTask, "edit-user-task");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void addValidationListeners() {
        nameField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        emailField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        phoneField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        roleCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        statusCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
    }

    private void onInputChanged() {
        clearFormError();
        updateChangedState();
        validateForm(submissionAttempted);
    }

    private void populateForm(UserDTO user) {
        if (user == null) {
            return;
        }
        dialogTitleLabel.setText("Edit User");
        nameField.setText(safe(user.getName()));
        emailField.setText(safe(user.getEmail()));
        phoneField.setText(safe(user.getPhone()));
        roleCombo.setValue(normalizeUpper(user.getRole()));
        statusCombo.setValue(normalizeUpper(user.getStatus()));
    }

    private boolean validateForm(boolean showErrors) {
        String name = trimmed(nameField.getText());
        String email = trimmed(emailField.getText());
        String phone = trimmed(phoneField.getText());
        String role = roleCombo.getValue();
        String status = statusCombo.getValue();

        String nameError = name.isBlank() ? "Name is required." : "";
        String emailError = email.isBlank()
                ? "Email is required."
                : (EMAIL_PATTERN.matcher(email).matches() ? "" : "Invalid email format.");
        String phoneError = phone.isBlank() ? "Phone is required." : "";
        String roleError = role == null || role.isBlank() ? "Role is required." : "";
        String statusError = status == null || status.isBlank() ? "Status is required." : "";

        if (showErrors) {
            updateError(nameErrorLabel, nameError);
            updateError(emailErrorLabel, emailError);
            updateError(phoneErrorLabel, phoneError);
            updateError(roleErrorLabel, roleError);
            updateError(statusErrorLabel, statusError);
        } else {
            hideFieldValidationErrors();
        }

        boolean valid = nameError.isEmpty()
                && emailError.isEmpty()
                && phoneError.isEmpty()
                && roleError.isEmpty()
                && statusError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private UserDTO buildUpdatedUser() {
        UserDTO updated = copyUser(originalUser);
        updated.setName(trimmed(nameField.getText()));
        updated.setEmail(trimmed(emailField.getText()).toLowerCase(Locale.ROOT));
        updated.setPhone(trimmed(phoneField.getText()));
        updated.setRole(normalizeUpper(roleCombo.getValue()));
        updated.setStatus(normalizeUpper(statusCombo.getValue()));
        return updated;
    }

    private void updateChangedState() {
        if (originalUser == null) {
            formChanged.set(false);
            return;
        }
        boolean changed = !Objects.equals(trimmed(nameField.getText()), safe(originalUser.getName()).trim())
                || !Objects.equals(trimmed(emailField.getText()).toLowerCase(Locale.ROOT), safe(originalUser.getEmail()).trim().toLowerCase(Locale.ROOT))
                || !Objects.equals(trimmed(phoneField.getText()), safe(originalUser.getPhone()).trim())
                || !Objects.equals(normalizeUpper(roleCombo.getValue()), normalizeUpper(originalUser.getRole()))
                || !Objects.equals(normalizeUpper(statusCombo.getValue()), normalizeUpper(originalUser.getStatus()));
        formChanged.set(changed);
    }

    private UserDTO copyUser(UserDTO user) {
        if (user == null) {
            return null;
        }
        UserDTO copy = new UserDTO();
        copy.setUserId(user.getUserId());
        copy.setName(user.getName());
        copy.setEmail(user.getEmail());
        copy.setPhone(user.getPhone());
        copy.setRole(user.getRole());
        copy.setStatus(user.getStatus());
        copy.setCreatedAt(user.getCreatedAt());
        copy.setUpdatedAt(user.getUpdatedAt());
        return copy;
    }

    private void hideFieldValidationErrors() {
        updateError(nameErrorLabel, "");
        updateError(emailErrorLabel, "");
        updateError(phoneErrorLabel, "");
        updateError(roleErrorLabel, "");
        updateError(statusErrorLabel, "");
    }

    private void setFormError(String message) {
        updateError(formErrorLabel, message == null ? "" : message);
    }

    private void clearFormError() {
        updateError(formErrorLabel, "");
    }

    private void updateError(Label label, String message) {
        boolean hasError = message != null && !message.isBlank();
        label.setText(hasError ? message : "");
        label.setVisible(hasError);
        label.setManaged(hasError);
    }

    private void closeDialog() {
        if (saveChangesButton != null && saveChangesButton.getScene() != null) {
            Stage stage = (Stage) saveChangesButton.getScene().getWindow();
            stage.close();
        }
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
