package com.busticket.controller.admin;

import com.busticket.dto.BusDTO;
import com.busticket.dto.CreateBusRequest;
import com.busticket.dto.UpdateBusRequest;
import com.busticket.enums.BusType;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.remote.BusRemote;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class BusFormDialogController {
    private static final Pattern BUS_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9-]{3,20}$");

    @FXML private VBox dialogRoot;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label formErrorLabel;

    @FXML private TextField busNumberField;
    @FXML private Label busNumberErrorLabel;
    @FXML private TextField busNameField;
    @FXML private Label busNameErrorLabel;
    @FXML private TextField totalSeatsField;
    @FXML private Label totalSeatsErrorLabel;
    @FXML private ComboBox<String> busTypeCombo;
    @FXML private Label busTypeErrorLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label statusErrorLabel;

    @FXML private Button saveButton;
    @FXML private ProgressIndicator progressIndicator;

    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty formChanged = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty requiresChange = new SimpleBooleanProperty(false);

    private BusRemote busRemote;
    private Long currentAdminUserId;
    private Mode mode = Mode.ADD;
    private BusDTO originalBus;
    private Consumer<BusDTO> onSaveSuccess;
    private boolean submissionAttempted;

    public enum Mode {
        ADD,
        EDIT
    }

    @FXML
    private void initialize() {
        busTypeCombo.setItems(FXCollections.observableArrayList(
                Arrays.stream(BusType.values()).map(Enum::name).toList()
        ));
        statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        statusCombo.setValue("ACTIVE");

        totalSeatsField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

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

    public void configure(
            BusRemote busRemote,
            Long currentAdminUserId,
            Mode mode,
            BusDTO bus,
            Consumer<BusDTO> onSaveSuccess
    ) {
        this.busRemote = busRemote;
        this.currentAdminUserId = currentAdminUserId;
        this.mode = mode == null ? Mode.ADD : mode;
        this.onSaveSuccess = onSaveSuccess;
        this.originalBus = bus == null ? null : copyBus(bus);
        this.submissionAttempted = false;

        if (this.mode == Mode.EDIT) {
            requiresChange.set(true);
            titleLabel.setText("Edit Bus");
            subtitleLabel.setText("Update bus details and operational status.");
            saveButton.setText("Save Changes");
            populateForm(this.originalBus);
        } else {
            requiresChange.set(false);
            titleLabel.setText("Add Bus");
            subtitleLabel.setText("Create a new bus profile.");
            saveButton.setText("Create Bus");
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
        if (busRemote == null || currentAdminUserId == null) {
            setFormError("Bus service is unavailable. Please reopen this dialog.");
            return;
        }

        Task<BusDTO> saveTask = new Task<>() {
            @Override
            protected BusDTO call() throws Exception {
                if (mode == Mode.EDIT) {
                    return busRemote.updateBus(buildUpdateRequest());
                }
                return busRemote.createBus(buildCreateRequest());
            }
        };

        saveTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                saving.set(false);
                if (onSaveSuccess != null) {
                    onSaveSuccess.accept(saveTask.getValue());
                }
                closeDialog();
            });
        });
        saveTask.setOnFailed(event -> Platform.runLater(() -> {
            saving.set(false);
            handleSaveFailure(saveTask.getException());
        }));

        saving.set(true);
        startBackgroundTask(saveTask, mode == Mode.EDIT ? "edit-bus-task" : "create-bus-task");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private CreateBusRequest buildCreateRequest() {
        CreateBusRequest request = new CreateBusRequest();
        request.setRequestedByUserId(currentAdminUserId);
        request.setBusNumber(normalizeBusNumber(busNumberField.getText()));
        request.setBusName(trimmed(busNameField.getText()));
        request.setTotalSeats(parseSeats(totalSeatsField.getText()));
        request.setBusType(normalizeUpper(busTypeCombo.getValue()));
        request.setStatus(normalizeStatus(statusCombo.getValue()));
        return request;
    }

    private UpdateBusRequest buildUpdateRequest() {
        UpdateBusRequest request = new UpdateBusRequest();
        request.setBusId(originalBus == null ? null : originalBus.getBusId());
        request.setRequestedByUserId(currentAdminUserId);
        request.setBusNumber(normalizeBusNumber(busNumberField.getText()));
        request.setBusName(trimmed(busNameField.getText()));
        request.setTotalSeats(parseSeats(totalSeatsField.getText()));
        request.setBusType(normalizeUpper(busTypeCombo.getValue()));
        request.setStatus(normalizeStatus(statusCombo.getValue()));
        return request;
    }

    private void addValidationListeners() {
        busNumberField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        busNameField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        totalSeatsField.textProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        busTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
        statusCombo.valueProperty().addListener((obs, oldValue, newValue) -> onInputChanged());
    }

    private void onInputChanged() {
        clearFormError();
        updateChangedState();
        validateForm(submissionAttempted);
    }

    private boolean validateForm(boolean showErrors) {
        String busNumber = normalizeBusNumber(busNumberField.getText());
        String busName = trimmed(busNameField.getText());
        String seatsText = trimmed(totalSeatsField.getText());
        String busType = normalizeUpper(busTypeCombo.getValue());
        String status = normalizeStatus(statusCombo.getValue());

        String busNumberError;
        if (busNumber.isBlank()) {
            busNumberError = "Bus number is required.";
        } else if (!BUS_NUMBER_PATTERN.matcher(busNumber).matches()) {
            busNumberError = "Use 3-20 chars: letters, digits, hyphen.";
        } else {
            busNumberError = "";
        }

        String busNameError = busName.isBlank() ? "Bus name is required." : "";

        String seatsError;
        Integer seats = parseSeats(seatsText);
        if (seatsText.isBlank()) {
            seatsError = "Total seats is required.";
        } else if (seats == null || seats <= 0) {
            seatsError = "Total seats must be a positive integer.";
        } else {
            seatsError = "";
        }

        String busTypeError;
        if (busType.isBlank()) {
            busTypeError = "Bus type is required.";
        } else if (!isValidBusType(busType)) {
            busTypeError = "Invalid bus type.";
        } else {
            busTypeError = "";
        }

        String statusError;
        if (status.isBlank()) {
            statusError = "Status is required.";
        } else if (!isValidStatus(status)) {
            statusError = "Invalid status.";
        } else {
            statusError = "";
        }

        if (showErrors) {
            updateError(busNumberErrorLabel, busNumberError);
            updateError(busNameErrorLabel, busNameError);
            updateError(totalSeatsErrorLabel, seatsError);
            updateError(busTypeErrorLabel, busTypeError);
            updateError(statusErrorLabel, statusError);
        } else {
            hideFieldValidationErrors();
        }

        boolean valid = busNumberError.isEmpty()
                && busNameError.isEmpty()
                && seatsError.isEmpty()
                && busTypeError.isEmpty()
                && statusError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private void populateForm(BusDTO bus) {
        if (bus == null) {
            return;
        }
        busNumberField.setText(trimmed(bus.getBusNumber()));
        busNameField.setText(trimmed(resolveBusName(bus)));
        totalSeatsField.setText(String.valueOf(bus.getTotalSeats()));
        busTypeCombo.setValue(normalizeUpper(bus.getType()));
        statusCombo.setValue(normalizeStatus(bus.getStatus()));
    }

    private void clearForm() {
        busNumberField.clear();
        busNameField.clear();
        totalSeatsField.clear();
        busTypeCombo.getSelectionModel().clearSelection();
        statusCombo.setValue("ACTIVE");
    }

    private void updateChangedState() {
        if (!requiresChange.get() || originalBus == null) {
            formChanged.set(true);
            return;
        }

        boolean changed = !Objects.equals(normalizeBusNumber(busNumberField.getText()), normalizeBusNumber(originalBus.getBusNumber()))
                || !Objects.equals(trimmed(busNameField.getText()), trimmed(resolveBusName(originalBus)))
                || !Objects.equals(parseSeats(totalSeatsField.getText()), originalBus.getTotalSeats())
                || !Objects.equals(normalizeUpper(busTypeCombo.getValue()), normalizeUpper(originalBus.getType()))
                || !Objects.equals(normalizeStatus(statusCombo.getValue()), normalizeStatus(originalBus.getStatus()));
        formChanged.set(changed);
    }

    private void handleSaveFailure(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof DuplicateResourceException) {
            updateError(busNumberErrorLabel, "Bus number already exists.");
            setFormError("Please use a different bus number.");
            return;
        }
        if (root instanceof ValidationException validationException) {
            String code = validationException.getMessage();
            if ("BUS_NUMBER_EXISTS".equals(code)) {
                updateError(busNumberErrorLabel, "Bus number already exists.");
            }
            setFormError(mapValidationMessage(code));
            return;
        }
        if (root instanceof RemoteException) {
            setFormError("Server connection failed while saving bus.");
            return;
        }
        setFormError("Unable to save bus right now. Please try again.");
    }

    private String mapValidationMessage(String code) {
        if (code == null || code.isBlank()) {
            return "Validation failed.";
        }
        return switch (code) {
            case "FORBIDDEN_ONLY_ADMIN" -> "Only ACTIVE ADMIN users can manage buses.";
            case "MISSING_ADMIN_CONTEXT" -> "Admin session is missing. Please sign in again.";
            case "BUS_NUMBER_REQUIRED" -> "Bus number is required.";
            case "INVALID_BUS_NUMBER_FORMAT" -> "Bus number format is invalid.";
            case "BUS_NAME_REQUIRED" -> "Bus name is required.";
            case "INVALID_TOTAL_SEATS" -> "Total seats must be greater than zero.";
            case "BUS_TYPE_REQUIRED" -> "Bus type is required.";
            case "INVALID_BUS_TYPE" -> "Bus type is invalid.";
            case "INVALID_STATUS" -> "Status is invalid.";
            case "BUS_NOT_FOUND" -> "Bus not found.";
            case "BUS_NUMBER_EXISTS" -> "Bus number already exists.";
            case "CREATE_BUS_FAILED" -> "Could not create bus due to a server error.";
            case "UPDATE_BUS_FAILED" -> "Could not update bus due to a server error.";
            default -> "Validation failed. Please review the form.";
        };
    }

    private String resolveBusName(BusDTO bus) {
        if (bus == null) {
            return "";
        }
        if (bus.getBusName() != null && !bus.getBusName().isBlank()) {
            return bus.getBusName();
        }
        return trimmed(bus.getBusNumber());
    }

    private boolean isValidBusType(String busType) {
        try {
            BusType.valueOf(busType);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isValidStatus(String status) {
        return "ACTIVE".equals(status) || "INACTIVE".equals(status);
    }

    private Integer parseSeats(String value) {
        String text = trimmed(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BusDTO copyBus(BusDTO bus) {
        BusDTO copy = new BusDTO();
        copy.setBusId(bus.getBusId());
        copy.setBusNumber(bus.getBusNumber());
        copy.setBusName(bus.getBusName());
        copy.setTotalSeats(bus.getTotalSeats());
        copy.setType(bus.getType());
        copy.setStatus(normalizeStatus(bus.getStatus()));
        return copy;
    }

    private void hideFieldValidationErrors() {
        updateError(busNumberErrorLabel, "");
        updateError(busNameErrorLabel, "");
        updateError(totalSeatsErrorLabel, "");
        updateError(busTypeErrorLabel, "");
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

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeUpper(String value) {
        return trimmed(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeBusNumber(String value) {
        return normalizeUpper(value);
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeUpper(value);
        if ("1".equals(normalized)) {
            return "ACTIVE";
        }
        if ("0".equals(normalized)) {
            return "INACTIVE";
        }
        return normalized;
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
