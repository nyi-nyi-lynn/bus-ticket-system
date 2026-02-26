package com.busticket.controller.admin;

import com.busticket.dto.RouteDTO;
import com.busticket.remote.RouteRemote;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class RouteFormDialogController {
    @FXML private VBox dialogRoot;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label formErrorLabel;

    @FXML private TextField originField;
    @FXML private Label originErrorLabel;
    @FXML private TextField destinationField;
    @FXML private Label destinationErrorLabel;
    @FXML private TextField distanceField;
    @FXML private Label distanceErrorLabel;
    @FXML private TextField durationField;
    @FXML private Label durationErrorLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label statusErrorLabel;

    @FXML private Button saveButton;
    @FXML private ProgressIndicator progressIndicator;

    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty formChanged = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty requiresChange = new SimpleBooleanProperty(false);

    private RouteRemote routeRemote;
    private Mode mode = Mode.ADD;
    private RouteDTO originalRoute;
    private Consumer<RouteDTO> onSaveSuccess;
    private boolean submissionAttempted;

    public enum Mode {
        ADD,
        EDIT
    }

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");

        distanceField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null));

        saveButton.disableProperty().bind(
                formValid.not()
                        .or(saving)
                        .or(Bindings.when(requiresChange).then(formChanged.not()).otherwise(false))
        );
        dialogRoot.disableProperty().bind(saving);
        progressIndicator.visibleProperty().bind(saving);
        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());

        addValidationListeners();
        hideFieldValidationErrors();
        validateForm(false);
    }

    public void configure(RouteRemote routeRemote, Mode mode, RouteDTO route, Consumer<RouteDTO> onSaveSuccess) {
        this.routeRemote = routeRemote;
        this.mode = mode == null ? Mode.ADD : mode;
        this.originalRoute = route == null ? null : copyRoute(route);
        this.onSaveSuccess = onSaveSuccess;
        this.submissionAttempted = false;

        if (this.mode == Mode.EDIT) {
            requiresChange.set(true);
            titleLabel.setText("Edit Route");
            subtitleLabel.setText("Update route details and status.");
            saveButton.setText("Save Changes");
            populateForm(this.originalRoute);
        } else {
            requiresChange.set(false);
            titleLabel.setText("Add Route");
            subtitleLabel.setText("Create a new route profile.");
            saveButton.setText("Create Route");
            clearForm();
        }
        clearFormError();
        updateChangedState();
        validateForm(false);
    }

    @FXML
    private void onSave() {
        submissionAttempted = true;
        clearFormError();
        if (!validateForm(true)) {
            return;
        }
        if (requiresChange.get() && !formChanged.get()) {
            setFormError("No changes to save.");
            return;
        }
        if (routeRemote == null) {
            setFormError("Route service is unavailable. Please reopen this dialog.");
            return;
        }

        RouteDTO payload = buildPayload();
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (mode == Mode.EDIT) {
                    return routeRemote.updateRoute(payload);
                }
                return routeRemote.saveRoute(payload);
            }
        };

        saveTask.setOnSucceeded(event -> Platform.runLater(() -> {
            saving.set(false);
            if (Boolean.TRUE.equals(saveTask.getValue())) {
                if (onSaveSuccess != null) {
                    onSaveSuccess.accept(payload);
                }
                closeDialog();
                return;
            }
            setFormError("Unable to save route. Please check form data.");
        }));

        saveTask.setOnFailed(event -> Platform.runLater(() -> {
            saving.set(false);
            Throwable ex = saveTask.getException();
            if (ex instanceof RemoteException) {
                setFormError("Server connection failed while saving route.");
            } else {
                setFormError("Unable to save route right now. Please try again.");
            }
        }));

        saving.set(true);
        startBackgroundTask(saveTask, mode == Mode.EDIT ? "edit-route-task" : "create-route-task");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void addValidationListeners() {
        originField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        destinationField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        distanceField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        durationField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
    }

    private void onInputChanged() {
        clearFormError();
        updateChangedState();
        validateForm(submissionAttempted);
    }

    private boolean validateForm(boolean showErrors) {
        String origin = trimmed(originField.getText());
        String destination = trimmed(destinationField.getText());
        String distanceText = trimmed(distanceField.getText());
        String duration = trimmed(durationField.getText());
        String status = normalizeStatus(statusCombo.getValue());

        String originError = origin.isBlank() ? "Origin city is required." : "";
        String destinationError = destination.isBlank() ? "Destination city is required." : "";
        if (originError.isEmpty() && destinationError.isEmpty() && origin.equalsIgnoreCase(destination)) {
            destinationError = "Origin and destination must be different.";
        }

        String distanceError;
        Double distanceValue = parseDistance(distanceText);
        if (distanceText.isBlank()) {
            distanceError = "Distance is required.";
        } else if (distanceValue == null || distanceValue < 0) {
            distanceError = "Distance must be a valid number >= 0.";
        } else {
            distanceError = "";
        }

        String durationError = duration.isBlank() ? "Estimated duration is required." : "";
        String statusError = status.isBlank() ? "Status is required." : "";

        if (showErrors) {
            updateError(originErrorLabel, originError);
            updateError(destinationErrorLabel, destinationError);
            updateError(distanceErrorLabel, distanceError);
            updateError(durationErrorLabel, durationError);
            updateError(statusErrorLabel, statusError);
        } else {
            hideFieldValidationErrors();
        }

        boolean valid = originError.isEmpty()
                && destinationError.isEmpty()
                && distanceError.isEmpty()
                && durationError.isEmpty()
                && statusError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private RouteDTO buildPayload() {
        RouteDTO dto = new RouteDTO();
        if (mode == Mode.EDIT && originalRoute != null) {
            dto.setRouteId(originalRoute.getRouteId());
        }
        dto.setOriginCity(trimmed(originField.getText()));
        dto.setDestinationCity(trimmed(destinationField.getText()));
        dto.setDistanceKm(parseDistance(distanceField.getText()) == null ? 0.0 : parseDistance(distanceField.getText()));
        dto.setEstimatedDuration(trimmed(durationField.getText()));
        dto.setStatus(normalizeStatus(statusCombo.getValue()));
        return dto;
    }

    private void populateForm(RouteDTO route) {
        if (route == null) {
            return;
        }
        originField.setText(trimmed(route.getOriginCity()));
        destinationField.setText(trimmed(route.getDestinationCity()));
        distanceField.setText(String.valueOf(route.getDistanceKm()));
        durationField.setText(trimmed(route.getEstimatedDuration()));
        statusCombo.setValue(normalizeStatus(route.getStatus()));
    }

    private void clearForm() {
        originField.clear();
        destinationField.clear();
        distanceField.clear();
        durationField.clear();
        statusCombo.setValue("ACTIVE");
    }

    private void updateChangedState() {
        if (!requiresChange.get() || originalRoute == null) {
            formChanged.set(true);
            return;
        }
        boolean changed = !Objects.equals(trimmed(originField.getText()), trimmed(originalRoute.getOriginCity()))
                || !Objects.equals(trimmed(destinationField.getText()), trimmed(originalRoute.getDestinationCity()))
                || !Objects.equals(parseDistance(distanceField.getText()), originalRoute.getDistanceKm())
                || !Objects.equals(trimmed(durationField.getText()), trimmed(originalRoute.getEstimatedDuration()))
                || !Objects.equals(normalizeStatus(statusCombo.getValue()), normalizeStatus(originalRoute.getStatus()));
        formChanged.set(changed);
    }

    private void hideFieldValidationErrors() {
        updateError(originErrorLabel, "");
        updateError(destinationErrorLabel, "");
        updateError(distanceErrorLabel, "");
        updateError(durationErrorLabel, "");
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

    private RouteDTO copyRoute(RouteDTO route) {
        RouteDTO copy = new RouteDTO();
        copy.setRouteId(route.getRouteId());
        copy.setOriginCity(route.getOriginCity());
        copy.setDestinationCity(route.getDestinationCity());
        copy.setDistanceKm(route.getDistanceKm());
        copy.setEstimatedDuration(route.getEstimatedDuration());
        copy.setStatus(normalizeStatus(route.getStatus()));
        return copy;
    }

    private Double parseDistance(String value) {
        String text = trimmed(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeStatus(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("1".equals(normalized)) {
            return "ACTIVE";
        }
        if ("0".equals(normalized)) {
            return "INACTIVE";
        }
        if (normalized.isBlank()) {
            return "ACTIVE";
        }
        return normalized;
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private void closeDialog() {
        if (saveButton != null && saveButton.getScene() != null) {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        }
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }
}
