package com.busticket.controller;

import com.busticket.dto.TripDTO;
import com.busticket.remote.BusTicketRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.PassengerFlowContext;
import com.busticket.util.PassengerViewRouter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class PassengerRouteSearchController {
    @FXML
    private ComboBox<String> originField;
    @FXML
    private ComboBox<String> destinationField;
    @FXML
    private DatePicker travelDatePicker;
    @FXML
    private Label statusLabel;

    @FXML
    private ListView<TripDTO> tripListView;

    private BusTicketRemote busTicketRemote;

    public void initialize() {
        setupList();
        try {
            busTicketRemote = RMIClient.getBusTicketRemote();
            loadOriginCities();
            loadDestinationCities();
            originField.setOnAction(event -> loadDestinationCities());
            loadAdvertisedTrips();
            statusLabel.setText("Available routes are shown below. Filter if needed.");
        } catch (Exception e) {
            statusLabel.setText("Unable to connect to server.");
        }
    }

    @FXML
    public void handleSearchTrips() {
        try {
            if (busTicketRemote == null) {
                statusLabel.setText("Server not connected.");
                return;
            }
            if (originField.getValue() == null || destinationField.getValue() == null) {
                statusLabel.setText("Select from and to.");
                return;
            }
            LocalDate requestedDate = travelDatePicker.getValue();
            List<TripDTO> trips;
            if (requestedDate == null) {
                trips = busTicketRemote.searchTripsClosest(
                        originField.getValue(),
                        destinationField.getValue(),
                        LocalDate.now()
                );
                if (!trips.isEmpty()) {
                    statusLabel.setText("Showing closest available dates for this route.");
                }
            } else {
                trips = busTicketRemote.searchTrips(
                        originField.getValue(),
                        destinationField.getValue(),
                        requestedDate
                );
            }
            if (trips.isEmpty() && requestedDate != null) {
                trips = busTicketRemote.searchTripsClosest(
                        originField.getValue(),
                        destinationField.getValue(),
                        requestedDate
                );
                if (!trips.isEmpty()) {
                    LocalDate closest = trips.get(0).getTravelDate();
                    statusLabel.setText("No exact date. Showing closest available date: " + closest);
                }
            }
            tripListView.setItems(FXCollections.observableArrayList(trips));
            if (trips.isEmpty()) {
                statusLabel.setText("No trips found.");
            } else if (!statusLabel.getText().startsWith("No exact date")) {
                statusLabel.setText(trips.size() + " trip(s) found.");
            }
        } catch (Exception e) {
            statusLabel.setText("Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleBookSelectedTrip() {
        TripDTO selected = tripListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a trip first.");
            return;
        }
        PassengerFlowContext.setSelectedTrip(selected);
        PassengerFlowContext.setCurrentBooking(null);
        PassengerViewRouter.open("order");
    }

    private void setupList() {
        tripListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TripDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label routeLabel = new Label(item.getOriginCity() + " -> " + item.getDestinationCity());
                routeLabel.getStyleClass().add("trip-route-title");

                Label metaLabel = new Label("Bus " + item.getBusNumber()
                        + " | " + item.getTravelDate()
                        + " | " + item.getDepartureTime() + " - " + item.getArrivalTime());
                metaLabel.getStyleClass().add("trip-meta");

                VBox left = new VBox(routeLabel, metaLabel);
                left.setSpacing(4);

                Label priceLabel = new Label(String.format("$%.2f", item.getPrice()));
                priceLabel.getStyleClass().add("trip-price");

                Label statusPill = new Label(item.getStatus());
                statusPill.getStyleClass().add(item.getAvailableSeats() > 0 ? "status-success" : "status-error");

                VBox right = new VBox(priceLabel, statusPill);
                right.setSpacing(6);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(left, spacer, right);
                row.getStyleClass().add("trip-list-item");
                row.setSpacing(10);

                setGraphic(row);
            }
        });
    }

    private void loadAdvertisedTrips() {
        try {
            List<TripDTO> trips = busTicketRemote.getAdvertisedTrips();
            tripListView.setItems(FXCollections.observableArrayList(trips));
        } catch (Exception e) {
            statusLabel.setText("Cannot load advertised trips.");
        }
    }

    private void loadOriginCities() {
        try {
            ObservableList<String> origins = FXCollections.observableArrayList(busTicketRemote.getOriginCities());
            originField.setItems(origins);
        } catch (Exception e) {
            statusLabel.setText("Cannot load origin cities.");
        }
    }

    private void loadDestinationCities() {
        try {
            String origin = originField.getValue();
            ObservableList<String> destinations = FXCollections.observableArrayList(busTicketRemote.getDestinationCities(origin));
            destinationField.setItems(destinations);
            destinationField.getSelectionModel().clearSelection();
        } catch (Exception e) {
            statusLabel.setText("Cannot load destination cities.");
        }
    }
}
