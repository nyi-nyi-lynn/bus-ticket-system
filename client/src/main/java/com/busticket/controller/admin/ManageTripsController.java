package com.busticket.controller.admin;

import com.busticket.dto.TripDTO;
import com.busticket.remote.BusRemote;
import com.busticket.remote.RouteRemote;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ManageTripsController {
    @FXML private TextField globalSearchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<TripDTO> tripsTable;
    @FXML private TableColumn<TripDTO, String> noColumn;
    @FXML private TableColumn<TripDTO, String> routeColumn;
    @FXML private TableColumn<TripDTO, String> busColumn;
    @FXML private TableColumn<TripDTO, String> travelDateColumn;
    @FXML private TableColumn<TripDTO, String> departureColumn;
    @FXML private TableColumn<TripDTO, String> arrivalColumn;
    @FXML private TableColumn<TripDTO, String> priceColumn;
    @FXML private TableColumn<TripDTO, String> statusColumn;
    @FXML private TableColumn<TripDTO, Void> actionsColumn;

    private final ObservableList<TripDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<TripDTO> filteredData;
    private SortedList<TripDTO> sortedData;
    private TripRemote tripRemote;
    private BusRemote busRemote;
    private RouteRemote routeRemote;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DecimalFormat PRICE_FMT = new DecimalFormat("0.00");

    @FXML
    private void initialize() {
        try {
            tripRemote = RMIClient.getTripRemote();
            busRemote = RMIClient.getBusRemote();
            routeRemote = RMIClient.getRouteRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        setupTable();
        setupFilters();
        loadTrips();
    }

    @FXML
    private void onAddTrip() {
        openTripFormDialog(TripFormDialogController.Mode.ADD, null);
    }

    @FXML
    private void onResetFilters() {
        globalSearchField.clear();
        statusFilter.setValue("ALL");
        applyFilters();
    }

    private void setupTable() {
        tripsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        noColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        noColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(String.valueOf(getIndex() + 1));
            }
        });

        routeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(routeLabel(data.getValue())));
        busColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getBusNumber())));
        travelDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getTravelDate() == null ? "-" : data.getValue().getTravelDate().format(DATE_FMT)
        ));
        departureColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                formatTime(data.getValue().getDepartureTime())
        ));
        arrivalColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                formatTime(data.getValue().getArrivalTime())
        ));
        priceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(PRICE_FMT.format(data.getValue().getPrice())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(normalizeStatus(data.getValue().getStatus())));
        statusColumn.setCellFactory(col -> new StatusBadgeCell());

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button closeButton = new Button("Close");
            private final HBox actions = new HBox(8, editButton, closeButton);

            {
                editButton.getStyleClass().add("action-button");
                closeButton.getStyleClass().addAll("action-button", "action-button-danger");

                editButton.setOnAction(event -> {
                    TripDTO trip = getTableView().getItems().get(getIndex());
                    openTripFormDialog(TripFormDialogController.Mode.EDIT, trip);
                });
                closeButton.setOnAction(event -> {
                    TripDTO trip = getTableView().getItems().get(getIndex());
                    onCloseTrip(trip);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                TripDTO trip = getTableView().getItems().get(getIndex());
                closeButton.setDisable("CLOSED".equalsIgnoreCase(normalizeStatus(trip.getStatus())));
                setGraphic(actions);
            }
        });

        tripsTable.setRowFactory(table -> {
            TableRow<TripDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openTripFormDialog(TripFormDialogController.Mode.EDIT, row.getItem());
                }
            });
            return row;
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(List.of("ALL", "OPEN", "CLOSED")));
        statusFilter.setValue("ALL");

        filteredData = new FilteredList<>(masterData, trip -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tripsTable.comparatorProperty());
        tripsTable.setItems(sortedData);

        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadTrips() {
        loadTrips(null);
    }

    private void loadTrips(Runnable onSuccess) {
        Task<List<TripDTO>> loadTask = new Task<>() {
            @Override
            protected List<TripDTO> call() throws Exception {
                return tripRemote.getAllTrips();
            }
        };

        loadTask.setOnSucceeded(event -> Platform.runLater(() -> {
            List<TripDTO> trips = loadTask.getValue();
            masterData.setAll(trips == null ? List.of() : trips);
            applyFilters();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }));

        loadTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(
                        Alert.AlertType.ERROR,
                        "Load Failed",
                        "Unable to load trips.",
                        loadTask.getException() == null ? "Unexpected error." : loadTask.getException().getMessage()
                )));

        startBackgroundTask(loadTask, "manage-trips-load-task");
    }

    private void applyFilters() {
        String global = normalize(globalSearchField.getText());
        String statusValue = normalizeFilter(statusFilter.getValue());

        filteredData.setPredicate(trip -> {
            if (trip == null) {
                return false;
            }
            if (!global.isEmpty() && !matchesGlobal(trip, global)) {
                return false;
            }
            if (!statusValue.isEmpty() && !equalsIgnoreCase(normalizeStatus(trip.getStatus()), statusValue)) {
                return false;
            }
            return true;
        });
    }

    private boolean matchesGlobal(TripDTO trip, String term) {
        return containsIgnoreCase(String.valueOf(trip.getTripId()), term)
                || containsIgnoreCase(routeLabel(trip), term)
                || containsIgnoreCase(trip.getBusNumber(), term)
                || containsIgnoreCase(trip.getTravelDate() == null ? "" : trip.getTravelDate().format(DATE_FMT), term)
                || containsIgnoreCase(trip.getDepartureTime() == null ? "" : formatTime(trip.getDepartureTime()), term)
                || containsIgnoreCase(trip.getArrivalTime() == null ? "" : formatTime(trip.getArrivalTime()), term)
                || containsIgnoreCase(PRICE_FMT.format(trip.getPrice()), term)
                || containsIgnoreCase(normalizeStatus(trip.getStatus()), term);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FMT).toLowerCase(Locale.ENGLISH);
    }

    private void openTripFormDialog(TripFormDialogController.Mode mode, TripDTO trip) {
        if (tripRemote == null || busRemote == null || routeRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Trip service is unavailable.", "Please reconnect and try again.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/busticket/view/admin/TripFormDialog.fxml"));
            Parent root = loader.load();

            TripFormDialogController controller = loader.getController();
            controller.configure(tripRemote, busRemote, routeRemote, mode, trip, savedTrip ->
                    loadTrips(() -> showAlert(
                            Alert.AlertType.INFORMATION,
                            mode == TripFormDialogController.Mode.ADD ? "Trip Created" : "Trip Updated",
                            mode == TripFormDialogController.Mode.ADD
                                    ? "New trip created successfully."
                                    : "Trip updated successfully.",
                            savedTrip == null ? "The trip list has been refreshed." : routeLabel(savedTrip)
                    )));

            Stage dialog = new Stage();
            dialog.setTitle(mode == TripFormDialogController.Mode.ADD ? "Add Trip" : "Edit Trip");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (tripsTable.getScene() != null && tripsTable.getScene().getWindow() != null) {
                dialog.initOwner(tripsTable.getScene().getWindow());
            }
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Unable to open trip dialog.", ex.getMessage());
        }
    }

    private void onCloseTrip(TripDTO trip) {
        if (trip == null || trip.getTripId() == null) {
            return;
        }
        if (!confirm("Close Trip", "Are you sure you want to close this trip?")) {
            return;
        }

        Task<Boolean> closeTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return tripRemote.deleteTrip(trip.getTripId());
            }
        };

        closeTask.setOnSucceeded(event -> Platform.runLater(() -> {
            if (!Boolean.TRUE.equals(closeTask.getValue())) {
                showAlert(Alert.AlertType.ERROR, "Close Failed", "Unable to close trip.", "Please try again.");
                return;
            }
            loadTrips(() -> showAlert(
                    Alert.AlertType.INFORMATION,
                    "Trip Closed",
                    "Trip has been closed.",
                    routeLabel(trip)
            ));
        }));

        closeTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(
                        Alert.AlertType.ERROR,
                        "Close Failed",
                        "Unable to close trip.",
                        closeTask.getException() == null ? "Unexpected error." : closeTask.getException().getMessage()
                )));

        startBackgroundTask(closeTask, "manage-trips-close-task");
    }

    private String routeLabel(TripDTO trip) {
        if (trip == null) {
            return "-";
        }
        return safeText(trip.getOriginCity()) + " -> " + safeText(trip.getDestinationCity());
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if ("OPEN".equals(normalized) || "CLOSED".equals(normalized)) {
            return normalized;
        }
        return "OPEN";
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String source, String term) {
        if (term == null || term.isBlank()) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String source, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return source.equalsIgnoreCase(expected);
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

    private static class StatusBadgeCell extends TableCell<TripDTO, String> {
        private final Label label = new Label();

        private StatusBadgeCell() {
            label.getStyleClass().add("badge");
            label.getStyleClass().add("badge-status-pill");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || item.isBlank()) {
                setGraphic(null);
                return;
            }
            String normalized = item.trim().toUpperCase(Locale.ROOT);
            label.setText(normalized);
            label.getStyleClass().removeAll("badge-status-active", "badge-status-inactive");
            label.getStyleClass().add("OPEN".equals(normalized) ? "badge-status-active" : "badge-status-inactive");
            setGraphic(label);
        }
    }
}
