package com.busticket.controller.passenger;

import com.busticket.dto.PassengerProfileDTO;
import com.busticket.dto.PassengerProfileUpdateDTO;
import com.busticket.exception.UnauthorizedException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.PassengerRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PassengerProfileController {
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label avatarLetterLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField personalEmailField;

    @FXML private Label totalBookingsLabel;
    @FXML private Label completedTripsLabel;
    @FXML private Label cancelledTripsLabel;
    @FXML private Label memberSinceLabel;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private PassengerRemote passengerRemote;
    private Long currentUserId;
    private PassengerProfileDTO cachedProfile;
    private boolean loading;

    @FXML
    private void initialize() {
        try {
            passengerRemote = RMIClient.getPassengerRemote();
        } catch (Exception ex) {
            passengerRemote = null;
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Service Unavailable",
                            "Passenger profile service is not available.",
                            ex.getMessage()));
        }

        currentUserId = Session.getCurrentUser() == null ? null : Session.getCurrentUser().getUserId();
        setupReadonlyFields();
        if (passengerRemote != null) {
            loadProfile();
        }
    }

    @FXML
    private void onSave() {
        if (loading) {
            return;
        }
        if (passengerRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Passenger profile is unavailable.",
                    "Please reconnect and try again.");
            return;
        }
        if (Session.isGuest() || currentUserId == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You must be logged in as PASSENGER.",
                    "Please sign in again.");
            return;
        }

        PassengerProfileUpdateDTO updateDTO = new PassengerProfileUpdateDTO();
        updateDTO.setUserId(currentUserId);
        updateDTO.setFirstName(text(firstNameField));
        updateDTO.setLastName(text(lastNameField));
        updateDTO.setPhone(text(phoneField));

        setLoading(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                passengerRemote.updateProfile(updateDTO);
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoading(false);
            showAlert(Alert.AlertType.INFORMATION, "Profile Updated",
                    "Your profile has been updated.", "Changes saved successfully.");
            loadProfile();
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (ex instanceof ValidationException validation) {
                showAlert(Alert.AlertType.WARNING, "Update Failed",
                        "Unable to update profile.", validation.getMessage());
            } else if (ex instanceof UnauthorizedException unauthorized) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized",
                        "You are not authorized to update this profile.", unauthorized.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed",
                        "Unable to update profile.", ex == null ? "Unexpected error." : ex.getMessage());
            }
            setLoading(false);
        }));

        startBackgroundTask(task, "passenger-profile-update-task");
    }

    @FXML
    private void onCancel() {
        applyProfile(cachedProfile);
    }

    private void loadProfile() {
        if (passengerRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Passenger profile is unavailable.",
                    "Please reconnect and try again.");
            return;
        }
        if (Session.isGuest() || currentUserId == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "You must be logged in as PASSENGER.",
                    "Please sign in again.");
            return;
        }

        setLoading(true);
        Task<PassengerProfileDTO> task = new Task<>() {
            @Override
            protected PassengerProfileDTO call() throws Exception {
                return passengerRemote.getProfile(currentUserId);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            cachedProfile = task.getValue();
            applyProfile(cachedProfile);
            setLoading(false);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            if (ex instanceof ValidationException validation) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized",
                        "You are not authorized to view this profile.", validation.getMessage());
            } else if (ex instanceof UnauthorizedException unauthorized) {
                showAlert(Alert.AlertType.WARNING, "Unauthorized",
                        "You are not authorized to view this profile.", unauthorized.getMessage());
            } else {
                showAlert(Alert.AlertType.ERROR, "Load Failed",
                        "Unable to load profile.", ex == null ? "Unexpected error." : ex.getMessage());
            }
            setLoading(false);
        }));

        startBackgroundTask(task, "passenger-profile-load-task");
    }

    private void applyProfile(PassengerProfileDTO profile) {
        if (profile == null) {
            clearFields();
            return;
        }

        String fullName = safe(profile.getFullName());
        String username = safe(profile.getUsername());
        String email = safe(profile.getEmail());

        fullNameLabel.setText(display(fullName));
        usernameLabel.setText(display(username));
        emailLabel.setText(display(email));
        roleLabel.setText("PASSENGER");
        avatarLetterLabel.setText(resolveAvatarLetter(username));

        String[] nameParts = splitName(fullName);
        firstNameField.setText(nameParts[0]);
        lastNameField.setText(nameParts[1]);
        phoneField.setText(profile.getPhone() == null ? "" : profile.getPhone());
        personalEmailField.setText(display(email));

        totalBookingsLabel.setText(String.valueOf(profile.getTotalBookings()));
        completedTripsLabel.setText(String.valueOf(profile.getCompletedTrips()));
        cancelledTripsLabel.setText(String.valueOf(profile.getCancelledTrips()));
        memberSinceLabel.setText(display(profile.getMemberSince()));

    }

    private void clearFields() {
        fullNameLabel.setText("-");
        usernameLabel.setText("-");
        emailLabel.setText("-");
        roleLabel.setText("PASSENGER");
        avatarLetterLabel.setText("?");

        firstNameField.setText("");
        lastNameField.setText("");
        phoneField.setText("");
        personalEmailField.setText("-");

        totalBookingsLabel.setText("0");
        completedTripsLabel.setText("0");
        cancelledTripsLabel.setText("0");
        memberSinceLabel.setText("-");

    }

    private void setupReadonlyFields() {
        personalEmailField.setEditable(false);
    }

    private void setLoading(boolean value) {
        loading = value;
        if (saveButton != null) {
            saveButton.setDisable(value);
        }
        if (cancelButton != null) {
            cancelButton.setDisable(value);
        }
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex <= 0) {
            return new String[]{trimmed, ""};
        }
        String first = trimmed.substring(0, spaceIndex).trim();
        String last = trimmed.substring(spaceIndex + 1).trim();
        return new String[]{first, last};
    }

    private String resolveAvatarLetter(String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(username.trim().charAt(0)));
    }

    private String text(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String display(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
