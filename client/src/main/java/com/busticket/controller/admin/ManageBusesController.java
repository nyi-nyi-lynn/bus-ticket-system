package com.busticket.controller.admin;

import com.busticket.dto.BusDTO;
import com.busticket.exception.ValidationException;
import com.busticket.remote.BusRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
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
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ManageBusesController {
    @FXML private TextField globalSearchField;
    @FXML private ComboBox<String> busTypeFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<BusDTO> busesTable;
    @FXML private TableColumn<BusDTO, String> busIdColumn;
    @FXML private TableColumn<BusDTO, String> busNumberColumn;
    @FXML private TableColumn<BusDTO, String> busNameColumn;
    @FXML private TableColumn<BusDTO, String> totalSeatsColumn;
    @FXML private TableColumn<BusDTO, String> busTypeColumn;
    @FXML private TableColumn<BusDTO, String> statusColumn;
    @FXML private TableColumn<BusDTO, Void> actionsColumn;

    private final ObservableList<BusDTO> busesData = FXCollections.observableArrayList();
    private FilteredList<BusDTO> filteredData;
    private SortedList<BusDTO> sortedData;
    private BusRemote busRemote;
    private Long currentAdminUserId;

    @FXML
    private void initialize() {
        try {
            busRemote = RMIClient.getBusRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        currentAdminUserId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : null;

        setupTable();
        setupFilters();
        loadBuses();
    }

    @FXML
    private void onAddBus() {
        openBusFormDialog(BusFormDialogController.Mode.ADD, null);
    }

    private void setupTable() {
        busesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        busIdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        busIdColumn.setCellFactory(col -> new TableCell<>() {
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
        busNumberColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getBusNumber())));
        busNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveBusName(data.getValue())));
        totalSeatsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getTotalSeats())));
        busTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getBusType())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(normalizeStatus(data.getValue().getStatus())));

        statusColumn.setCellFactory(col -> new StatusBadgeCell());

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actions = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().addAll("action-button", "action-button-danger");

                editButton.setOnAction(event -> {
                    BusDTO bus = getTableView().getItems().get(getIndex());
                    openBusFormDialog(BusFormDialogController.Mode.EDIT, bus);
                });

                deleteButton.setOnAction(event -> {
                    BusDTO bus = getTableView().getItems().get(getIndex());
                    onDeleteBus(bus);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(actions);
            }
        });

        busesTable.setRowFactory(table -> {
            TableRow<BusDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openBusFormDialog(BusFormDialogController.Mode.EDIT, row.getItem());
                }
            });
            return row;
        });
    }

    private void setupFilters() {
        List<String> typeOptions = Arrays.stream(com.busticket.enums.BusType.values())
                .map(Enum::name)
                .toList();
        List<String> statusOptions = List.of("ACTIVE", "INACTIVE");

        busTypeFilter.setItems(FXCollections.observableArrayList());
        busTypeFilter.getItems().add("ALL");
        busTypeFilter.getItems().addAll(typeOptions);

        statusFilter.setItems(FXCollections.observableArrayList());
        statusFilter.getItems().add("ALL");
        statusFilter.getItems().addAll(statusOptions);

        busTypeFilter.setValue("ALL");
        statusFilter.setValue("ALL");

        filteredData = new FilteredList<>(busesData, bus -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(busesTable.comparatorProperty());
        busesTable.setItems(sortedData);

        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        busTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    private void onResetFilters() {
        globalSearchField.clear();
        busTypeFilter.setValue("ALL");
        statusFilter.setValue("ALL");
        applyFilters();
    }

    private void loadBuses() {
        loadBuses(null);
    }

    private void loadBuses(Runnable onSuccess) {
        Task<List<BusDTO>> loadTask = new Task<>() {
            @Override
            protected List<BusDTO> call() throws Exception {
                return busRemote.getAllBuses();
            }
        };

        loadTask.setOnSucceeded(event -> Platform.runLater(() -> {
            List<BusDTO> buses = loadTask.getValue();
            busesData.setAll(buses == null ? List.of() : buses);
            applyFilters();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }));

        loadTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(
                        Alert.AlertType.ERROR,
                        "Load Failed",
                        "Unable to load buses.",
                        loadTask.getException() == null ? "Unexpected error." : loadTask.getException().getMessage()
                )));

        startBackgroundTask(loadTask, "manage-buses-load-task");
    }

    private void applyFilters() {
        String global = normalize(globalSearchField == null ? null : globalSearchField.getText());
        String typeValue = normalizeFilter(busTypeFilter == null ? null : busTypeFilter.getValue());
        String statusValue = normalizeFilter(statusFilter == null ? null : statusFilter.getValue());

        filteredData.setPredicate(bus -> {
            if (bus == null) {
                return false;
            }
            if (!global.isEmpty() && !matchesGlobal(bus, global)) {
                return false;
            }
            if (!typeValue.isEmpty() && !equalsIgnoreCase(bus.getBusType(), typeValue)) {
                return false;
            }
            String busStatus = normalizeStatus(bus.getStatus());
            if (!statusValue.isEmpty() && !equalsIgnoreCase(busStatus, statusValue)) {
                return false;
            }
            return true;
        });
    }

    private void openBusFormDialog(BusFormDialogController.Mode mode, BusDTO bus) {
        if (busRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Bus service is unavailable.", "Please reconnect and try again.");
            return;
        }
        if (currentAdminUserId == null) {
            showAlert(Alert.AlertType.ERROR, "Unauthorized", "You must be logged in as ADMIN.", "Please sign in again.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/busticket/view/admin/BusFormDialog.fxml"));
            Parent root = loader.load();

            BusFormDialogController controller = loader.getController();
            controller.configure(busRemote, currentAdminUserId, mode, bus, savedBus ->
                    loadBuses(() -> showAlert(
                            Alert.AlertType.INFORMATION,
                            mode == BusFormDialogController.Mode.ADD ? "Bus Created" : "Bus Updated",
                            mode == BusFormDialogController.Mode.ADD
                                    ? "New bus created successfully."
                                    : "Bus details updated successfully.",
                            savedBus == null
                                    ? "The buses list has been refreshed."
                                    : resolveBusName(savedBus) + " has been saved."
                    )));

            Stage dialog = new Stage();
            dialog.setTitle(mode == BusFormDialogController.Mode.ADD ? "Add Bus" : "Edit Bus");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (busesTable.getScene() != null && busesTable.getScene().getWindow() != null) {
                dialog.initOwner(busesTable.getScene().getWindow());
            }
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Unable to open bus dialog.", ex.getMessage());
        }
    }

    private void onDeleteBus(BusDTO bus) {
        if (bus == null || bus.getBusId() == null) {
            return;
        }
        if (!confirm("Delete Bus", "Are you sure you want to delete this bus?")) {
            return;
        }

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                busRemote.deleteBus(bus.getBusId());
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> Platform.runLater(() ->
                loadBuses(() -> showAlert(
                        Alert.AlertType.INFORMATION,
                        "Bus Deleted",
                        "The bus has been deleted.",
                        resolveBusName(bus) + " was removed successfully."
                ))));

        deleteTask.setOnFailed(event -> Platform.runLater(() ->
                handleDeleteFailure(deleteTask.getException(), bus)));

        startBackgroundTask(deleteTask, "manage-buses-delete-task");
    }

    private void handleDeleteFailure(Throwable throwable, BusDTO bus) {
        Throwable root = unwrap(throwable);
        if (root instanceof ValidationException validationException) {
            String message = switch (validationException.getMessage()) {
                case "BUS_IN_USE" -> "Cannot delete this bus because it is referenced by existing trips.";
                case "BUS_NOT_FOUND" -> "The bus no longer exists. Please refresh and try again.";
                default -> "Unable to delete bus due to validation error.";
            };
            showAlert(Alert.AlertType.WARNING, "Delete Failed", "Bus could not be deleted.", message);
            return;
        }

        if (root instanceof RemoteException) {
            showAlert(Alert.AlertType.ERROR, "Delete Failed", "Server connection failed while deleting bus.", root.getMessage());
            return;
        }

        showAlert(
                Alert.AlertType.ERROR,
                "Delete Failed",
                "Unable to delete bus.",
                root == null ? "Unexpected error." : root.getMessage()
        );
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String resolveBusName(BusDTO bus) {
        if (bus == null) {
            return "-";
        }
        String busName = safeText(bus.getBusName());
        if ("-".equals(busName)) {
            return safeText(bus.getBusNumber());
        }
        return busName;
    }

    private String normalizeStatus(String status) {
        String normalized = safeText(status).toUpperCase(Locale.ROOT);
        if ("1".equals(normalized)) {
            return "ACTIVE";
        }
        if ("0".equals(normalized)) {
            return "INACTIVE";
        }
        if ("ACTIVE".equals(normalized) || "INACTIVE".equals(normalized)) {
            return normalized;
        }
        return "-";
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private boolean matchesGlobal(BusDTO bus, String term) {
        return containsIgnoreCase(String.valueOf(bus.getBusId()), term)
                || containsIgnoreCase(bus.getBusNumber(), term)
                || containsIgnoreCase(resolveBusName(bus), term)
                || containsIgnoreCase(bus.getBusType(), term)
                || containsIgnoreCase(normalizeStatus(bus.getStatus()), term)
                || containsIgnoreCase(String.valueOf(bus.getTotalSeats()), term);
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

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
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

    private static class StatusBadgeCell extends TableCell<BusDTO, String> {
        private final Label label = new Label();

        private StatusBadgeCell() {
            label.getStyleClass().add("badge");
            label.getStyleClass().add("badge-status-pill");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || item.isBlank() || "-".equals(item)) {
                setGraphic(null);
                return;
            }
            String normalized = item.trim().toUpperCase(Locale.ROOT);
            label.setText(normalized);
            label.getStyleClass().removeAll("badge-status-active", "badge-status-inactive");
            label.getStyleClass().add("ACTIVE".equals(normalized) ? "badge-status-active" : "badge-status-inactive");
            setGraphic(label);
        }
    }
}
