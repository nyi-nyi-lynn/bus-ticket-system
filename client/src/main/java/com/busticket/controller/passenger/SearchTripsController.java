package com.busticket.controller.passenger;

import com.busticket.dto.TripDTO;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class SearchTripsController {
    @FXML private TextField originField;
    @FXML private TextField destinationField;
    @FXML private DatePicker datePicker;

    @FXML private TableView<TripDTO> resultTable;
    @FXML private TableColumn<TripDTO, String> colTrip;
    @FXML private TableColumn<TripDTO, String> colRoute;
    @FXML private TableColumn<TripDTO, String> colDeparture;
    @FXML private TableColumn<TripDTO, String> colArrival;
    @FXML private TableColumn<TripDTO, String> colPrice;
    @FXML private TableColumn<TripDTO, String> colSeats;

    private TripRemote tripRemote;

    @FXML
    private void initialize() {
        colTrip.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTripId())));
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRouteId() + ""));
        colDeparture.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDepartureTime())));
        colArrival.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getArrivalTime())));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPrice())));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAvailableSeats())));
    }

    @FXML
    private void onSearch() {
        try {
            if (tripRemote == null) {
                tripRemote = RMIClient.getTripRemote();
            }
            List<TripDTO> trips = tripRemote.searchTrips(
                    originField.getText(),
                    destinationField.getText(),
                    datePicker.getValue()
            );
            System.out.println(originField.getText() + " " + destinationField.getText() + " " + datePicker.getValue() + " " + trips.size());
            resultTable.getItems().setAll(trips);
        } catch (Exception ex) {
            resultTable.getItems().clear();
        }
    }

    @FXML
    private void onSelectTrip() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SeatSelectionView.fxml");
    }
}
