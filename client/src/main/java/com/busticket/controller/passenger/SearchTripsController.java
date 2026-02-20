package com.busticket.controller.passenger;

import com.busticket.dto.TripDTO;
import com.busticket.remote.TripRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.util.SceneSwitcher;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
        // Table Columns Setting
        colTrip.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTripId())));
        colRoute.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRouteId() + ""));
        colDeparture.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDepartureTime())));
        colArrival.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getArrivalTime())));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPrice())));
        colSeats.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAvailableSeats())));

        // Data မရှိတဲ့အခါ ပြမယ့် စာသား
        resultTable.setPlaceholder(new Label("ရှာဖွေမှုရလဒ် မရှိသေးပါ။"));
    }

    @FXML
    private void onSearch() {
        // Input စစ်ဆေးခြင်း
        if (originField.getText().isEmpty() || destinationField.getText().isEmpty() || datePicker.getValue() == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "သတိပေးချက်", "အချက်အလက် ပြည့်စုံစွာ ဖြည့်ပါ", "မြို့အမည် နှင့် နေ့စွဲကို ရွေးချယ်ပေးပါ။");
            return;
        }

        try {
            // RMI Connection ယူခြင်း
            if (tripRemote == null) {
                tripRemote = RMIClient.getTripRemote();
            }

            // Data လှမ်းယူခြင်း
            List<TripDTO> trips = tripRemote.searchTrips(
                    originField.getText(),
                    destinationField.getText(),
                    datePicker.getValue()
            );

            // ရလာတဲ့ data ကို table ထဲထည့်ခြင်း
            if (trips == null || trips.isEmpty()) {
                resultTable.getItems().clear();
                showSimpleAlert(Alert.AlertType.INFORMATION, "ရှာဖွေမှုရလဒ်", "မတွေ့ပါ", "သင်ရှာဖွေနေသော ခရီးစဉ်ကို မတွေ့ရှိပါ။");
            } else {
                resultTable.getItems().setAll(trips);
            }

        } catch (ClassCastException cce) {
            // ClassCastException အတွက် အထူးသီးသန့် ရှင်းပြချက်
            showSimpleAlert(Alert.AlertType.ERROR, "System Error", "Class Loading Error",
                    "Client နှင့် Server ကြား Interface မကိုက်ညီပါ။ Shared library ကို 'mvn install' လုပ်ပြီး Server ကို Restart ချကြည့်ပါ။");
            cce.printStackTrace();
        } catch (Exception ex) {
            // အထွေထွေ Error များအတွက်
            showSimpleAlert(Alert.AlertType.ERROR, "Error", "ချိတ်ဆက်မှု မအောင်မြင်ပါ", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Alert ပြရန် Helper Method
    private void showSimpleAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onSelectTrip() {
        TripDTO selected = resultTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSimpleAlert(Alert.AlertType.WARNING, "သတိပေးချက်", "ခရီးစဉ် မရွေးရသေးပါ", "ကျေးဇူးပြု၍ ခရီးစဉ်တစ်ခုကို အရင်ရွေးချယ်ပါ။");
            return;
        }
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SeatSelectionView.fxml");
    }
}