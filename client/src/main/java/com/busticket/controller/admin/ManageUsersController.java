package com.busticket.controller.admin;

import com.busticket.dto.UserDTO;
import com.busticket.remote.UserRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ManageUsersController {
    @FXML private TextField globalSearchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> nameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> phoneColumn;
    @FXML private TableColumn<UserDTO, String> roleColumn;
    @FXML private TableColumn<UserDTO, String> statusColumn;
    @FXML private TableColumn<UserDTO, String> createdAtColumn;
    @FXML private TableColumn<UserDTO, Void> actionsColumn;

    private final ObservableList<UserDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<UserDTO> filteredData;
    private SortedList<UserDTO> sortedData;

    private UserRemote userRemote;
    private Long currentAdminUserId;

    @FXML
    private void initialize() {
        try {
            userRemote = RMIClient.getUserRemote();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        currentAdminUserId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : null;

        setupTable();
        setupFilters();
        loadUsers();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getName())));
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getEmail())));
        phoneColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getPhone())));
        roleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getRole())));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getStatus())));
        createdAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getCreatedAt())));

        roleColumn.setCellFactory(col -> new BadgeCell("badge-role"));
        statusColumn.setCellFactory(col -> new BadgeCell("badge-status"));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button blockButton = new Button();
            private final HBox container = new HBox(8, editButton, blockButton);

            {
                editButton.getStyleClass().add("action-button");
                blockButton.getStyleClass().add("action-button");
                container.setFillHeight(true);
                container.setMinHeight(Region.USE_PREF_SIZE);

                editButton.setOnAction(event -> {
                    UserDTO user = getTableView().getItems().get(getIndex());
                    onEditUser(user);
                });

                blockButton.setOnAction(event -> {
                    UserDTO user = getTableView().getItems().get(getIndex());
                    onToggleBlock(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                UserDTO user = getTableView().getItems().get(getIndex());
                boolean blocked = isBlocked(user);
                blockButton.setText(blocked ? "Unblock" : "Block");
                blockButton.getStyleClass().removeAll("action-button", "action-button-danger");
                blockButton.getStyleClass().add(blocked ? "action-button" : "action-button-danger");

                boolean isSelf = currentAdminUserId != null && currentAdminUserId.equals(user.getUserId());
                blockButton.setDisable(isSelf);

                setGraphic(container);
            }
        });

        usersTable.setRowFactory(table -> {
            TableRow<UserDTO> row = new TableRow<>();
            row.getStyleClass().add("card-row");
            return row;
        });
    }

    private void setupFilters() {
        List<String> roleOptions = List.of("ALL", "ADMIN", "STAFF", "PASSENGER");
        List<String> statusOptions = List.of("ALL", "ACTIVE", "BLOCKED");

        roleFilter.setItems(FXCollections.observableArrayList(roleOptions));
        statusFilter.setItems(FXCollections.observableArrayList(statusOptions));

        roleFilter.setValue("ALL");
        statusFilter.setValue("ALL");

        filteredData = new FilteredList<>(masterData, user -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedData);

        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String global = normalize(globalSearchField.getText());
        String roleGlobal = normalizeFilter(roleFilter.getValue());
        String statusGlobal = normalizeFilter(statusFilter.getValue());

        filteredData.setPredicate(user -> {
            if (user == null) {
                return false;
            }
            if (currentAdminUserId != null && currentAdminUserId.equals(user.getUserId())) {
                return false;
            }

            if (!global.isEmpty() && !matchesGlobal(user, global)) {
                return false;
            }

            if (!roleGlobal.isEmpty() && !equalsIgnoreCase(user.getRole(), roleGlobal)) {
                return false;
            }

            if (!statusGlobal.isEmpty() && !equalsIgnoreCase(user.getStatus(), statusGlobal)) {
                return false;
            }

            return true;
        });
    }

    private boolean matchesGlobal(UserDTO user, String term) {
        return containsIgnoreCase(user.getName(), term)
                || containsIgnoreCase(user.getEmail(), term)
                || containsIgnoreCase(user.getPhone(), term)
                || containsIgnoreCase(user.getRole(), term)
                || containsIgnoreCase(user.getStatus(), term);
    }

    private void loadUsers() {
        try {
            List<UserDTO> users = userRemote.getAllUsers();
            masterData.setAll(users == null ? List.of() : users);
        } catch (RemoteException ex) {
            showAlert(Alert.AlertType.ERROR, "Load Failed", "Unable to load users.", ex.getMessage());
        }
    }

    @FXML
    private void onAddUser() {
    }

    @FXML
    private void onResetFilters() {
        globalSearchField.clear();
        roleFilter.setValue("ALL");
        statusFilter.setValue("ALL");
        applyFilters();
    }

    private void onEditUser(UserDTO user) {
    }

    private void onToggleBlock(UserDTO user) {
        if (user == null) {
            return;
        }

        boolean blocked = isBlocked(user);
        String targetStatus = blocked ? "ACTIVE" : "BLOCKED";
        String actionLabel = blocked ? "Unblock" : "Block";

        if (!confirm("Confirm " + actionLabel, "Are you sure you want to " + actionLabel.toLowerCase(Locale.ROOT) + " this user?")) {
            return;
        }

        String previousStatus = user.getStatus();
        user.setStatus(targetStatus);

        try {
            boolean updated = userRemote.updateUser(user);
            if (!updated) {
                user.setStatus(previousStatus);
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Unable to update user status.", "Please try again.");
            } else {
                usersTable.refresh();
            }
        } catch (RemoteException ex) {
            user.setStatus(previousStatus);
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Unable to update user status.", ex.getMessage());
        }
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private boolean isBlocked(UserDTO user) {
        return "BLOCKED".equalsIgnoreCase(safeText(user.getStatus()));
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

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class BadgeCell extends TableCell<UserDTO, String> {
        private final Label label = new Label();
        private final String extraStyleClass;

        private BadgeCell(String extraStyleClass) {
            this.extraStyleClass = extraStyleClass;
            label.getStyleClass().add("badge");
            label.getStyleClass().add(extraStyleClass);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || item.isBlank()) {
                setGraphic(null);
                return;
            }
            label.setText(item.toUpperCase(Locale.ROOT));
            setGraphic(label);
        }
    }
}

