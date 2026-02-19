package com.busticket.controller.guest;

import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class GuestInfoController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML
    private void onContinue() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/PaymentView.fxml");
    }
}
