package com.busticket.controller;

import com.busticket.dto.RouteDTO;
import com.busticket.remote.RouteRemote;
import com.busticket.rmi.RMIClient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;


public class RouteController {
    @FXML
    private TextField originField;
    @FXML private TextField destinationField;
    @FXML private TextField distanceField;
    @FXML private TextField durationField;
    @FXML private TableView<RouteDTO> routeTable;

    private RouteRemote routeRemote;

    @FXML
    public void initialize() {

        try {
            routeRemote = RMIClient.getRouteRemote();
            loadRoutes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAdd() {

        try {
            RouteDTO dto = new RouteDTO();
            dto.setOriginCity(originField.getText());
            dto.setDestinationCity(destinationField.getText());
            dto.setDistanceKm(Double.parseDouble(distanceField.getText()));
            dto.setEstimatedDuration(durationField.getText());

            routeRemote.saveRoute(dto);
            loadRoutes();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRoutes() throws Exception {
        List<RouteDTO> routes = routeRemote.getAllRoutes();
        routeTable.setItems(FXCollections.observableArrayList(routes));
    }
}
