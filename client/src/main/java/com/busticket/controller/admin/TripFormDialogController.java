package com.busticket.controller.admin;

import com.busticket.dto.BusDTO;
import com.busticket.dto.RouteDTO;
import com.busticket.dto.TripDTO;
import com.busticket.remote.BusRemote;
import com.busticket.remote.RouteRemote;
import com.busticket.remote.TripRemote;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class TripFormDialogController {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @FXML private VBox dialogRoot;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label formErrorLabel;

    @FXML private ComboBox<OptionItem> busCombo;
    @FXML private Label busErrorLabel;
    @FXML private ComboBox<OptionItem> routeCombo;
    @FXML private Label routeErrorLabel;
    @FXML private DatePicker travelDatePicker;
    @FXML private Label travelDateErrorLabel;
    @FXML private TextField departureTimeField;
    @FXML private Label departureErrorLabel;
    @FXML private TextField arrivalTimeField;
    @FXML private Label arrivalErrorLabel;
    @FXML private TextField priceField;
    @FXML private Label priceErrorLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label statusErrorLabel;

    @FXML private Button saveButton;
    @FXML private ProgressIndicator progressIndicator;

    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty formChanged = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty loadingRefs = new SimpleBooleanProperty(false);
    private final BooleanProperty requiresChange = new SimpleBooleanProperty(false);

    private TripRemote tripRemote;
    private BusRemote busRemote;
    private RouteRemote routeRemote;
    private Mode mode = Mode.ADD;
    private TripDTO originalTrip;
    private Consumer<TripDTO> onSaveSuccess;
    private boolean submissionAttempted;

    public enum Mode {
        ADD,
        EDIT
    }

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll("OPEN", "CLOSED");
        statusCombo.setValue("OPEN");

        priceField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null));

        saveButton.disableProperty().bind(
                formValid.not()
                        .or(saving)
                        .or(loadingRefs)
                        .or(Bindings.when(requiresChange).then(formChanged.not()).otherwise(false))
        );
        dialogRoot.disableProperty().bind(saving.or(loadingRefs));
        progressIndicator.visibleProperty().bind(saving.or(loadingRefs));
        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());

        addValidationListeners();
        hideFieldValidationErrors();
        validateForm(false);
    }

    public void configure(
            TripRemote tripRemote,
            BusRemote busRemote,
            RouteRemote routeRemote,
            Mode mode,
            TripDTO trip,
            Consumer<TripDTO> onSaveSuccess
    ) {
        this.tripRemote = tripRemote;
        this.busRemote = busRemote;
        this.routeRemote = routeRemote;
        this.mode = mode == null ? Mode.ADD : mode;
        this.originalTrip = trip == null ? null : copyTrip(trip);
        this.onSaveSuccess = onSaveSuccess;
        this.submissionAttempted = false;

        if (this.mode == Mode.EDIT) {
            requiresChange.set(true);
            titleLabel.setText("Edit Trip");
            subtitleLabel.setText("Update trip schedule and status.");
            saveButton.setText("Save Changes");
        } else {
            requiresChange.set(false);
            titleLabel.setText("Add Trip");
            subtitleLabel.setText("Create a new trip schedule.");
            saveButton.setText("Create Trip");
            clearForm();
        }
        clearFormError();
        loadReferenceData();
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
        if (tripRemote == null) {
            setFormError("Trip service is unavailable. Please reopen this dialog.");
            return;
        }

        TripDTO payload = buildPayload();
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (mode == Mode.EDIT) {
                    return tripRemote.updateTrip(payload);
                }
                return tripRemote.saveTrip(payload);
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
            setFormError("Unable to save trip. Please check form data.");
        }));

        saveTask.setOnFailed(event -> Platform.runLater(() -> {
            saving.set(false);
            Throwable ex = saveTask.getException();
            if (ex instanceof RemoteException) {
                setFormError("Server connection failed while saving trip.");
            } else {
                setFormError("Unable to save trip right now. Please try again.");
            }
        }));

        saving.set(true);
        startBackgroundTask(saveTask, mode == Mode.EDIT ? "edit-trip-task" : "create-trip-task");
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void loadReferenceData() {
        loadingRefs.set(true);
        Task<ReferenceData> refTask = new Task<>() {
            @Override
            protected ReferenceData call() throws Exception {
                List<BusDTO> buses = busRemote.getAllBuses();
                List<RouteDTO> routes = routeRemote.getAllRoutes();
                return new ReferenceData(buses == null ? List.of() : buses, routes == null ? List.of() : routes);
            }
        };

        refTask.setOnSucceeded(event -> Platform.runLater(() -> {
            loadingRefs.set(false);
            ReferenceData data = refTask.getValue();
            populateReferenceCombos(data);
            if (mode == Mode.EDIT) {
                populateForm(originalTrip);
            }
            updateChangedState();
            validateForm(false);
        }));

        refTask.setOnFailed(event -> Platform.runLater(() -> {
            loadingRefs.set(false);
            setFormError("Failed to load buses/routes. Please retry.");
        }));

        startBackgroundTask(refTask, "trip-reference-load-task");
    }

    private void populateReferenceCombos(ReferenceData data) {
        busCombo.setItems(FXCollections.observableArrayList(
                data.buses.stream()
                        .map(bus -> new OptionItem(
                                bus.getBusId(),
                                (bus.getBusNumber() == null ? "-" : bus.getBusNumber()) + " (" + normalizeStatus(bus.getStatus(), "ACTIVE") + ")"
                        ))
                        .toList()
        ));
        routeCombo.setItems(FXCollections.observableArrayList(
                data.routes.stream()
                        .map(route -> new OptionItem(
                                route.getRouteId(),
                                safe(route.getOriginCity()) + " -> " + safe(route.getDestinationCity())
                                        + " (" + normalizeStatus(route.getStatus(), "ACTIVE") + ")"
                        ))
                        .toList()
        ));
    }

    private void addValidationListeners() {
        busCombo.valueProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        routeCombo.valueProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        travelDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        departureTimeField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        arrivalTimeField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        priceField.textProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> onInputChanged());
    }

    private void onInputChanged() {
        clearFormError();
        updateChangedState();
        validateForm(submissionAttempted);
    }

    private boolean validateForm(boolean showErrors) {
        OptionItem bus = busCombo.getValue();
        OptionItem route = routeCombo.getValue();
        LocalDate travelDate = travelDatePicker.getValue();
        String departureText = trimmed(departureTimeField.getText());
        String arrivalText = trimmed(arrivalTimeField.getText());
        String priceText = trimmed(priceField.getText());
        String status = normalizeTripStatus(statusCombo.getValue());

        String busError = bus == null ? "Bus is required." : "";
        String routeError = route == null ? "Route is required." : "";
        String dateError = travelDate == null ? "Travel date is required." : "";

        String departureError = parseTime(departureText) == null ? "Departure time must be hh:mm am/pm." : "";
        String arrivalError = parseTime(arrivalText) == null ? "Arrival time must be hh:mm am/pm." : "";

        String priceError;
        Double price = parsePrice(priceText);
        if (priceText.isBlank()) {
            priceError = "Price is required.";
        } else if (price == null || price <= 0) {
            priceError = "Price must be greater than 0.";
        } else {
            priceError = "";
        }

        String statusError = status.isBlank() ? "Status is required." : "";

        if (showErrors) {
            updateError(busErrorLabel, busError);
            updateError(routeErrorLabel, routeError);
            updateError(travelDateErrorLabel, dateError);
            updateError(departureErrorLabel, departureError);
            updateError(arrivalErrorLabel, arrivalError);
            updateError(priceErrorLabel, priceError);
            updateError(statusErrorLabel, statusError);
        } else {
            hideFieldValidationErrors();
        }

        boolean valid = busError.isEmpty()
                && routeError.isEmpty()
                && dateError.isEmpty()
                && departureError.isEmpty()
                && arrivalError.isEmpty()
                && priceError.isEmpty()
                && statusError.isEmpty();
        formValid.set(valid);
        return valid;
    }

    private TripDTO buildPayload() {
        TripDTO dto = new TripDTO();
        if (mode == Mode.EDIT && originalTrip != null) {
            dto.setTripId(originalTrip.getTripId());
        }
        OptionItem bus = busCombo.getValue();
        OptionItem route = routeCombo.getValue();
        dto.setBusId(bus == null ? null : bus.id());
        dto.setRouteId(route == null ? null : route.id());
        dto.setTravelDate(travelDatePicker.getValue());
        dto.setDepartureTime(parseTime(departureTimeField.getText()));
        dto.setArrivalTime(parseTime(arrivalTimeField.getText()));
        dto.setPrice(parsePrice(priceField.getText()) == null ? 0.0 : parsePrice(priceField.getText()));
        dto.setStatus(normalizeTripStatus(statusCombo.getValue()));
        return dto;
    }

    private void populateForm(TripDTO trip) {
        if (trip == null) {
            return;
        }
        busCombo.getSelectionModel().select(findOptionById(busCombo, trip.getBusId()));
        routeCombo.getSelectionModel().select(findOptionById(routeCombo, trip.getRouteId()));
        travelDatePicker.setValue(trip.getTravelDate());
        departureTimeField.setText(trip.getDepartureTime() == null ? "" : trip.getDepartureTime().format(TIME_FMT).toLowerCase(Locale.ENGLISH));
        arrivalTimeField.setText(trip.getArrivalTime() == null ? "" : trip.getArrivalTime().format(TIME_FMT).toLowerCase(Locale.ENGLISH));
        priceField.setText(String.valueOf(trip.getPrice()));
        statusCombo.setValue(normalizeTripStatus(trip.getStatus()));
    }

    private void clearForm() {
        busCombo.getSelectionModel().clearSelection();
        routeCombo.getSelectionModel().clearSelection();
        travelDatePicker.setValue(null);
        departureTimeField.clear();
        arrivalTimeField.clear();
        priceField.clear();
        statusCombo.setValue("OPEN");
    }

    private void updateChangedState() {
        if (!requiresChange.get() || originalTrip == null) {
            formChanged.set(true);
            return;
        }
        OptionItem selectedBus = busCombo.getValue();
        OptionItem selectedRoute = routeCombo.getValue();

        boolean changed = !Objects.equals(selectedBus == null ? null : selectedBus.id(), originalTrip.getBusId())
                || !Objects.equals(selectedRoute == null ? null : selectedRoute.id(), originalTrip.getRouteId())
                || !Objects.equals(travelDatePicker.getValue(), originalTrip.getTravelDate())
                || !Objects.equals(parseTime(departureTimeField.getText()), originalTrip.getDepartureTime())
                || !Objects.equals(parseTime(arrivalTimeField.getText()), originalTrip.getArrivalTime())
                || !Objects.equals(parsePrice(priceField.getText()), originalTrip.getPrice())
                || !Objects.equals(normalizeTripStatus(statusCombo.getValue()), normalizeTripStatus(originalTrip.getStatus()));
        formChanged.set(changed);
    }

    private OptionItem findOptionById(ComboBox<OptionItem> comboBox, Long id) {
        if (id == null || comboBox.getItems() == null) {
            return null;
        }
        for (OptionItem item : comboBox.getItems()) {
            if (Objects.equals(item.id(), id)) {
                return item;
            }
        }
        return null;
    }

    private LocalTime parseTime(String value) {
        String text = trimmed(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(text.toUpperCase(Locale.ENGLISH), TIME_FMT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private Double parsePrice(String value) {
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

    private void hideFieldValidationErrors() {
        updateError(busErrorLabel, "");
        updateError(routeErrorLabel, "");
        updateError(travelDateErrorLabel, "");
        updateError(departureErrorLabel, "");
        updateError(arrivalErrorLabel, "");
        updateError(priceErrorLabel, "");
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

    private TripDTO copyTrip(TripDTO source) {
        TripDTO copy = new TripDTO();
        copy.setTripId(source.getTripId());
        copy.setBusId(source.getBusId());
        copy.setRouteId(source.getRouteId());
        copy.setTravelDate(source.getTravelDate());
        copy.setDepartureTime(source.getDepartureTime());
        copy.setArrivalTime(source.getArrivalTime());
        copy.setPrice(source.getPrice());
        copy.setStatus(normalizeTripStatus(source.getStatus()));
        copy.setBusNumber(source.getBusNumber());
        copy.setOriginCity(source.getOriginCity());
        copy.setDestinationCity(source.getDestinationCity());
        copy.setTotalSeats(source.getTotalSeats());
        copy.setAvailableSeats(source.getAvailableSeats());
        return copy;
    }

    private String normalizeTripStatus(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("CLOSED".equals(normalized)) {
            return "CLOSED";
        }
        return "OPEN";
    }

    private String normalizeStatus(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(normalized) || "INACTIVE".equals(normalized)) {
            return normalized;
        }
        if ("OPEN".equals(normalized) || "CLOSED".equals(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
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

    private record OptionItem(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record ReferenceData(List<BusDTO> buses, List<RouteDTO> routes) {
    }
}
