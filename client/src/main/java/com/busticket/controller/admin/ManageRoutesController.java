package com.busticket.controller.admin;

import com.busticket.dto.RouteDTO;
import com.busticket.remote.RouteRemote;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ManageRoutesController {
    @FXML private TextField globalSearchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<RouteDTO> routesTable;
    @FXML private TableColumn<RouteDTO, String> noColumn;
    @FXML private TableColumn<RouteDTO, String> originColumn;
    @FXML private TableColumn<RouteDTO, String> destinationColumn;
    @FXML private TableColumn<RouteDTO, String> distanceColumn;
    @FXML private TableColumn<RouteDTO, String> durationColumn;
    @FXML private TableColumn<RouteDTO, String> statusColumn;
    @FXML private TableColumn<RouteDTO, Void> actionsColumn;

    private final ObservableList<RouteDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<RouteDTO> filteredData;
    private SortedList<RouteDTO> sortedData;
    private RouteRemote routeRemote;
    private static final DecimalFormat DISTANCE_FMT = new DecimalFormat("0.##");

    @FXML
    private void initialize() {
        try {
            routeRemote = RMIClient.getRouteRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        setupTable();
        setupFilters();
        loadRoutes();
    }

    @FXML
    private void onAddRoute() {
        openRouteFormDialog(RouteFormDialogController.Mode.ADD, null);
    }

    @FXML
    private void onResetFilters() {
        globalSearchField.clear();
        statusFilter.setValue("ALL");
        applyFilters();
    }

    private void setupTable() {
        routesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        originColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getOriginCity())));
        destinationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getDestinationCity())));
        distanceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDistance(data.getValue().getDistanceKm())));
        durationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getEstimatedDuration())));
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
                    RouteDTO route = getTableView().getItems().get(getIndex());
                    openRouteFormDialog(RouteFormDialogController.Mode.EDIT, route);
                });

                deleteButton.setOnAction(event -> {
                    RouteDTO route = getTableView().getItems().get(getIndex());
                    onDeactivateRoute(route);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                RouteDTO route = getTableView().getItems().get(getIndex());
                deleteButton.setDisable("INACTIVE".equalsIgnoreCase(normalizeStatus(route.getStatus())));
                setGraphic(actions);
            }
        });

        routesTable.setRowFactory(table -> {
            TableRow<RouteDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openRouteFormDialog(RouteFormDialogController.Mode.EDIT, row.getItem());
                }
            });
            return row;
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(List.of("ALL", "ACTIVE", "INACTIVE")));
        statusFilter.setValue("ALL");

        filteredData = new FilteredList<>(masterData, route -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(routesTable.comparatorProperty());
        routesTable.setItems(sortedData);

        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadRoutes() {
        loadRoutes(null);
    }

    private void loadRoutes(Runnable onSuccess) {
        Task<List<RouteDTO>> loadTask = new Task<>() {
            @Override
            protected List<RouteDTO> call() throws Exception {
                return routeRemote.getAllRoutes();
            }
        };

        loadTask.setOnSucceeded(event -> Platform.runLater(() -> {
            List<RouteDTO> routes = loadTask.getValue();
            masterData.setAll(routes == null ? List.of() : routes);
            applyFilters();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }));

        loadTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(
                        Alert.AlertType.ERROR,
                        "Load Failed",
                        "Unable to load routes.",
                        loadTask.getException() == null ? "Unexpected error." : loadTask.getException().getMessage()
                )));

        startBackgroundTask(loadTask, "manage-routes-load-task");
    }

    private void applyFilters() {
        String global = normalize(globalSearchField.getText());
        String statusValue = normalizeFilter(statusFilter.getValue());

        filteredData.setPredicate(route -> {
            if (route == null) {
                return false;
            }
            if (!global.isEmpty() && !matchesGlobal(route, global)) {
                return false;
            }
            if (!statusValue.isEmpty() && !equalsIgnoreCase(normalizeStatus(route.getStatus()), statusValue)) {
                return false;
            }
            return true;
        });
    }

    private boolean matchesGlobal(RouteDTO route, String term) {
        return containsIgnoreCase(String.valueOf(route.getRouteId()), term)
                || containsIgnoreCase(route.getOriginCity(), term)
                || containsIgnoreCase(route.getDestinationCity(), term)
                || containsIgnoreCase(route.getEstimatedDuration(), term)
                || containsIgnoreCase(formatDistance(route.getDistanceKm()), term)
                || containsIgnoreCase(normalizeStatus(route.getStatus()), term);
    }

    private void openRouteFormDialog(RouteFormDialogController.Mode mode, RouteDTO route) {
        if (routeRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Unavailable", "Route service is unavailable.", "Please reconnect and try again.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/busticket/view/admin/RouteFormDialog.fxml"));
            Parent root = loader.load();

            RouteFormDialogController controller = loader.getController();
            controller.configure(routeRemote, mode, route, savedRoute ->
                    loadRoutes(() -> showAlert(
                            Alert.AlertType.INFORMATION,
                            mode == RouteFormDialogController.Mode.ADD ? "Route Created" : "Route Updated",
                            mode == RouteFormDialogController.Mode.ADD
                                    ? "New route created successfully."
                                    : "Route updated successfully.",
                            savedRoute == null
                                    ? "The route list has been refreshed."
                                    : safeText(savedRoute.getOriginCity()) + " -> " + safeText(savedRoute.getDestinationCity())
                            )
                    ));

            Stage dialog = new Stage();
            dialog.setTitle(mode == RouteFormDialogController.Mode.ADD ? "Add Route" : "Edit Route");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (routesTable.getScene() != null && routesTable.getScene().getWindow() != null) {
                dialog.initOwner(routesTable.getScene().getWindow());
            }
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Dialog Error", "Unable to open Route dialog.", ex.getMessage());
        }
    }

    private void onDeactivateRoute(RouteDTO route) {
        if (route == null || route.getRouteId() == null) {
            return;
        }
        if (!confirm("Deactivate Route", "Are you sure you want to deactivate this route?")) {
            return;
        }

        Task<Boolean> deactivateTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return routeRemote.deactivateRoute(route.getRouteId());
            }
        };

        deactivateTask.setOnSucceeded(event -> Platform.runLater(() -> {
            boolean ok = Boolean.TRUE.equals(deactivateTask.getValue());
            if (!ok) {
                showAlert(Alert.AlertType.ERROR, "Deactivate Failed", "Unable to deactivate route.", "Please try again.");
                return;
            }
            loadRoutes(() -> showAlert(
                    Alert.AlertType.INFORMATION,
                    "Route Deactivated",
                    "Route has been deactivated.",
                    safeText(route.getOriginCity()) + " -> " + safeText(route.getDestinationCity())
            ));
        }));

        deactivateTask.setOnFailed(event -> Platform.runLater(() ->
                showAlert(
                        Alert.AlertType.ERROR,
                        "Deactivate Failed",
                        "Unable to deactivate route.",
                        deactivateTask.getException() == null ? "Unexpected error." : deactivateTask.getException().getMessage()
                )));

        startBackgroundTask(deactivateTask, "manage-routes-deactivate-task");
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
        if ("1".equals(normalized)) {
            return "ACTIVE";
        }
        if ("0".equals(normalized)) {
            return "INACTIVE";
        }
        if ("ACTIVE".equals(normalized) || "INACTIVE".equals(normalized)) {
            return normalized;
        }
        return "ACTIVE";
    }

    private String formatDistance(double distance) {
        return DISTANCE_FMT.format(distance) + " km";
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

    private static class StatusBadgeCell extends TableCell<RouteDTO, String> {
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
            label.getStyleClass().add("ACTIVE".equals(normalized) ? "badge-status-active" : "badge-status-inactive");
            setGraphic(label);
        }
    }
}
