package com.busticket.controller.passenger;

import com.busticket.dto.PaymentRequestDTO;
import com.busticket.dto.TripDTO;
import com.busticket.enums.PaymentMethod;
import com.busticket.exception.UnauthorizedException;
import com.busticket.remote.PaymentRemote;
import com.busticket.rmi.RMIClient;
import com.busticket.session.Session;
import com.busticket.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class PaymentController {

    @FXML private Label routeValueLabel;
    @FXML private Label travelDateValueLabel;
    @FXML private Label departureTimeValueLabel;
    @FXML private Label selectedSeatsValueLabel;
    @FXML private Label totalPriceValueLabel;

    @FXML private ToggleGroup paymentMethodToggle;
    @FXML private RadioButton kbzPayRadio;
    @FXML private RadioButton wavePayRadio;
    @FXML private RadioButton cardRadio;

    @FXML private VBox qrSection;
    @FXML private VBox cardFormSection;
    @FXML private ImageView qrImageView;
    @FXML private Label accountNameValueLabel;
    @FXML private Label accountPhoneValueLabel;
    @FXML private Label instructionLabel;
    @FXML private Button confirmPaymentButton;

    private static final String METHOD_KBZPAY = "KBZPAY";
    private static final String METHOD_WAVEPAY = "WAVEPAY";
    private static final String METHOD_CARD = "CARD";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private PaymentRemote paymentRemote;
    private TripDTO tripDTO;
    private List<String> selectedSeats = List.of();
    private double totalAmount;

    @FXML
    private void initialize() {
        initializePaymentMethods();
        initializeRemotes();
        renderBookingSummary();

        paymentMethodToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateQrSection());
        updateQrSection();
    }

    @FXML
    private void onPaymentMethodChanged() {
        updateQrSection();
    }

    @FXML
    private void onBack() {
        SceneSwitcher.switchContent("/com/busticket/view/passenger/SearchTripsView.fxml");
    }

    @FXML
    private void onConfirmPayment() {
        if (paymentRemote == null) {
            showAlert(Alert.AlertType.ERROR, "Payment Error", "Payment service unavailable.", "Please try again later.");
            return;
        }

        Long bookingId = resolveBookingId();
        if (bookingId == null) {
            showAlert(Alert.AlertType.WARNING, "Booking Missing", "Booking id not found.", "Please restart payment from booking summary.");
            return;
        }

        if (totalAmount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Total amount must be greater than 0.", "Please review selected seats and booking data.");
            return;
        }

        String selectedMethod = resolveSelectedMethod();
        if (selectedMethod == null) {
            showAlert(Alert.AlertType.WARNING, "Payment Method", "Please choose a payment method.", "Select KBZPay, WavePay, or Credit/Debit Card.");
            return;
        }

        if (confirmPaymentButton != null) {
            confirmPaymentButton.setDisable(true);
        }

        try {
            payBooking(bookingId, totalAmount, selectedMethod);
            Session.clearPendingSelection();
            navigateToTicketView();
        } catch (UnauthorizedException unauthorizedException) {
            showLoginRequiredAndRedirect(unauthorizedException.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Payment Failed", "Unable to process payment.", ex.getMessage());
        } finally {
            if (confirmPaymentButton != null) {
                confirmPaymentButton.setDisable(false);
            }
        }
    }

    private void initializePaymentMethods() {
        kbzPayRadio.setUserData(METHOD_KBZPAY);
        wavePayRadio.setUserData(METHOD_WAVEPAY);
        cardRadio.setUserData(METHOD_CARD);
        cardRadio.setSelected(true);
    }

    private void initializeRemotes() {
        try {
            paymentRemote = RMIClient.getPaymentRemote();
        } catch (Exception ex) {
            paymentRemote = null;
            showAlert(Alert.AlertType.ERROR, "Connection Error", "Payment service unavailable.", ex.getMessage());
        }
    }

    private void renderBookingSummary() {
        tripDTO = resolveTripData();
        selectedSeats = resolveSelectedSeats();
        totalAmount = resolveTotalAmount(selectedSeats.size());

        if (tripDTO == null) {
            routeValueLabel.setText("-");
            travelDateValueLabel.setText("-");
            departureTimeValueLabel.setText("-");
        } else {
            String route = safe(tripDTO.getOriginCity()) + " \u2192 " + safe(tripDTO.getDestinationCity());
            routeValueLabel.setText(route);
            travelDateValueLabel.setText(tripDTO.getTravelDate() == null ? "-" : tripDTO.getTravelDate().format(DATE_FMT));
            departureTimeValueLabel.setText(tripDTO.getDepartureTime() == null
                    ? "-"
                    : tripDTO.getDepartureTime().format(TIME_FMT).toLowerCase(Locale.ENGLISH));
        }

        selectedSeatsValueLabel.setText(selectedSeats.isEmpty() ? "-" : String.join(", ", selectedSeats));
        totalPriceValueLabel.setText(String.format(Locale.ENGLISH, "%.2f", totalAmount));
    }

    private TripDTO resolveTripData() {
        return Session.getPendingTrip();
    }

    private List<String> resolveSelectedSeats() {
        return Session.getPendingSeatNumbers();
    }

    private double resolveTotalAmount(int seatCount) {
        Double sessionAmount = Session.getCurrentBookingAmount();
        if (sessionAmount != null && sessionAmount > 0) {
            return sessionAmount;
        }

        if (tripDTO == null || seatCount <= 0) {
            return 0.0;
        }
        return tripDTO.getPrice() * seatCount;
    }

    private Long resolveBookingId() {
        return Session.getCurrentBookingId();
    }

    private String resolveSelectedMethod() {
        if (paymentMethodToggle == null || paymentMethodToggle.getSelectedToggle() == null) {
            return null;
        }
        Object userData = paymentMethodToggle.getSelectedToggle().getUserData();
        return userData == null ? null : userData.toString();
    }

    private void updateQrSection() {
        String selectedMethod = resolveSelectedMethod();
        boolean mobileBanking = METHOD_KBZPAY.equals(selectedMethod) || METHOD_WAVEPAY.equals(selectedMethod);
        boolean cardSelected = METHOD_CARD.equals(selectedMethod);

        qrSection.setVisible(mobileBanking);
        qrSection.setManaged(mobileBanking);
        cardFormSection.setVisible(cardSelected);
        cardFormSection.setManaged(cardSelected);

        if (!mobileBanking) {
            qrImageView.setImage(null);
            accountNameValueLabel.setText("-");
            accountPhoneValueLabel.setText("-");
            instructionLabel.setText("Scan to pay");
            return;
        }

        if (METHOD_KBZPAY.equals(selectedMethod)) {
            accountNameValueLabel.setText("Bus Ticket System (KBZPay)");
            accountPhoneValueLabel.setText("09-7000-70003");
        } else {
            accountNameValueLabel.setText("Bus Ticket System (WavePay)");
            accountPhoneValueLabel.setText("09-7000-70004");
        }

        instructionLabel.setText("Scan to pay");

        String payload = buildQrPayload(selectedMethod);
        Image qrImage = tryGenerateQrCodeImage(payload, 220, 220);
        if (qrImage == null) {
            qrImageView.setImage(null);
            instructionLabel.setText("Use account name and phone for manual transfer.");
        } else {
            qrImageView.setImage(qrImage);
        }
    }

    private String buildQrPayload(String selectedMethod) {
        Long bookingId = resolveBookingId();
        String bookingPart = bookingId == null ? "N/A" : bookingId.toString();
        return "bookingId=" + bookingPart
                + "|amount=" + String.format(Locale.ENGLISH, "%.2f", totalAmount)
                + "|method=" + selectedMethod;
    }

    private String toBackendPaymentMethod(String selectedMethod) {
        if (METHOD_CARD.equals(selectedMethod)) {
            return PaymentMethod.CARD.name();
        }
        return PaymentMethod.MOBILE_BANKING.name();
    }

    private void navigateToTicketView() {
        try {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketView.fxml");
        } catch (RuntimeException ex) {
            SceneSwitcher.switchContent("/com/busticket/view/passenger/TicketSuccessView.fxml");
        }
    }

    private void payBooking(Long bookingId, double amount, String selectedMethod) throws Exception {
        if (Session.getCurrentUser() == null || Session.getCurrentUser().getUserId() == null) {
            throw new UnauthorizedException("Please login to continue booking");
        }
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setUserId(Session.getCurrentUser().getUserId());
        request.setBookingId(bookingId);
        request.setAmount(amount);
        request.setPaymentMethod(toBackendPaymentMethod(selectedMethod));
        var payment = paymentRemote.processPayment(request);
        if (payment == null || payment.getPaymentId() == null) {
            throw new IllegalStateException("Unable to process payment.");
        }
        Session.setCurrentBookingContext(
                bookingId,
                Session.getCurrentUser().getUserId(),
                Session.getCurrentTicketCode(),
                amount
        );
    }

    private Image tryGenerateQrCodeImage(String payload, int width, int height) {
        try {
            Class<?> writerClass = Class.forName("com.google.zxing.qrcode.QRCodeWriter");
            Class<?> formatClass = Class.forName("com.google.zxing.BarcodeFormat");
            Class<?> matrixClass = Class.forName("com.google.zxing.common.BitMatrix");

            Object writer = writerClass.getDeclaredConstructor().newInstance();
            @SuppressWarnings("unchecked")
            Object qrFormat = Enum.valueOf((Class<Enum>) formatClass.asSubclass(Enum.class), "QR_CODE");
            Method encodeMethod = writerClass.getMethod("encode", String.class, formatClass, int.class, int.class);
            Object matrix = encodeMethod.invoke(writer, payload, qrFormat, width, height);
            Method getMethod = matrixClass.getMethod("get", int.class, int.class);

            WritableImage image = new WritableImage(width, height);
            PixelWriter pixelWriter = image.getPixelWriter();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean isBlack = (Boolean) getMethod.invoke(matrix, x, y);
                    pixelWriter.setColor(x, y, isBlack ? Color.BLACK : Color.WHITE);
                }
            }
            return image;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showLoginRequiredAndRedirect(String message) {
        showAlert(
                Alert.AlertType.WARNING,
                "Login Required",
                "Please login to continue booking",
                message == null || message.isBlank() ? "Please login to continue booking" : message
        );
        Session.clearPendingSelection();
        Session.clearBookingContext();
        SceneSwitcher.resetToAuth("/com/busticket/view/auth/LoginView.fxml");
    }
}
