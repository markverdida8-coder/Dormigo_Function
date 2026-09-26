package com.dormigo;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PaymentActivity extends AppCompatActivity {

    private final ApiClient apiClient =
            new ApiClient();

    private int bookingId = 0;

    private int paymentId = 0;
    private int paymentPeriod = 1;

    private double monthlyRent = 0;

    private String moveInDate = "";
    private String dueDate = "";

    private String bookingStatus = "";

    private String houseName = "";
    private String roomName = "";

    private View methodGCash;
    private View methodCash;

    private View radioGCash;
    private View radioCash;

    private View btnPay;

    private TextView houseNameLabel;
    private TextView paymentDescription;
    private TextView rentAmountLabel;
    private TextView totalDueLabel;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_payment
        );

        setupWindowInsets();

        readIntentData();

        bindViews();

        setupPaymentMethods();

        setupBackButton();

        loadBooking();
    }

    // =========================================================
    // WINDOW INSETS
    // =========================================================

    private void setupWindowInsets() {

        View mainLayout =
                findViewById(
                        R.id.mainLayout
                );

        if (mainLayout == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                mainLayout,
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars() | WindowInsetsCompat.Type.ime()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    // =========================================================
    // INTENT DATA
    // =========================================================

    private void readIntentData() {

        Intent intent =
                getIntent();

        bookingId =
                intent.getIntExtra(
                        "BOOKING_ID",
                        0
                );

        String receivedHouseName =
                intent.getStringExtra(
                        "HOUSE_NAME"
                );

        String receivedRoomName =
                intent.getStringExtra(
                        "ROOM_NAME"
                );

        if (receivedHouseName != null) {
            houseName =
                    receivedHouseName.trim();
        }

        if (receivedRoomName != null) {
            roomName =
                    receivedRoomName.trim();
        }
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private void bindViews() {

        houseNameLabel =
                findViewById(
                        R.id.houseNameLabel
                );

        paymentDescription =
                findViewById(
                        R.id.paymentDescription
                );

        rentAmountLabel =
                findViewById(
                        R.id.rentAmount
                );

        totalDueLabel =
                findViewById(
                        R.id.totalDue
                );

        methodGCash =
                findViewById(
                        R.id.methodGCash
                );

        methodCash =
                findViewById(
                        R.id.methodCash
                );

        radioGCash =
                findViewById(
                        R.id.radioGCash
                );

        radioCash =
                findViewById(
                        R.id.radioCash
                );

        btnPay =
                findViewById(
                        R.id.btnPay
                );

        if (
                houseNameLabel != null
                        && !houseName.isEmpty()
        ) {

            houseNameLabel.setText(
                    houseName
            );
        }

        if (
                paymentDescription != null
                        && !roomName.isEmpty()
                        && !houseName.isEmpty()
        ) {

            paymentDescription.setText(
                    getString(
                            R.string.room_at_house,
                            roomName,
                            houseName
                    )
            );
        }

        setPaymentButtonEnabled(
                false
        );
    }

    // =========================================================
    // LOAD BOOKING
    // =========================================================

    private void loadBooking() {

        if (bookingId <= 0) {

            showToast(
                    "Invalid booking. Please open payment from an approved booking."
            );

            setPaymentButtonEnabled(
                    false
            );

            return;
        }

        apiClient.getBookingById(
                bookingId,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            showToast(
                                    "Unable to load booking."
                            );

                            setPaymentButtonEnabled(
                                    false
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        String result =
                                body;

                        runOnUiThread(() -> {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                result
                                        );

                                if (!json.optBoolean(
                                        "success",
                                        false
                                )) {

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Unable to load booking."
                                            )
                                    );

                                    setPaymentButtonEnabled(
                                            false
                                    );

                                    return;
                                }

                                JSONArray dataArray =
                                        json.optJSONArray(
                                                "data"
                                        );

                                JSONObject data = null;

                                if (dataArray != null && dataArray.length() > 0) {
                                    data = dataArray.optJSONObject(0);
                                } else {
                                    data = json.optJSONObject("data");
                                }

                                if (data == null) {

                                    showToast(
                                            "Booking information is missing."
                                    );

                                    return;
                                }

                                bookingStatus =
                                        data.optString(
                                                        "status",
                                                        ""
                                                ).trim()
                                                .toUpperCase(
                                                        Locale.ROOT
                                                );

                                monthlyRent =
                                        data.optDouble(
                                                "agreed_monthly_rent",
                                                0
                                        );

                                moveInDate =
                                        data.optString(
                                                "move_in_date",
                                                ""
                                        );

                                if (!"APPROVED".equals(
                                        bookingStatus
                                )) {

                                    setPaymentButtonEnabled(
                                            false
                                    );

                                    showToast(
                                            "Only approved bookings can be paid."
                                    );

                                    return;
                                }

                                if (monthlyRent <= 0) {

                                    setPaymentButtonEnabled(
                                            false
                                    );

                                    showToast(
                                            "Invalid booking amount."
                                    );

                                    return;
                                }

                                updateAmountViews();

                                loadPayments();

                            } catch (Exception e) {

                                showToast(
                                        "Invalid booking response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // LOAD PAYMENTS FOR BOOKING
    // =========================================================

    private void loadPayments() {

        apiClient.getPaymentsForBooking(
                bookingId,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            showToast(
                                    "Unable to load payment records."
                            );

                            setPaymentButtonEnabled(
                                    false
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        String result =
                                body;

                        runOnUiThread(() -> {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                result
                                        );

                                if (!json.optBoolean(
                                        "success",
                                        false
                                )) {

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Unable to load payments."
                                            )
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                paymentId = 0;
                                paymentPeriod = 1;

                                int highestPeriod = 0;

                                if (data != null) {

                                    for (
                                            int i = 0;
                                            i < data.length();
                                            i++
                                    ) {

                                        JSONObject item =
                                                data.getJSONObject(
                                                        i
                                                );

                                        int period =
                                                item.optInt(
                                                        "payment_period",
                                                        0
                                                );

                                        if (
                                                period >
                                                        highestPeriod
                                        ) {

                                            highestPeriod =
                                                    period;
                                        }

                                        String status =
                                                item.optString(
                                                                "status",
                                                                ""
                                                        ).trim()
                                                        .toUpperCase(
                                                                Locale.ROOT
                                                        );

                                        if (
                                                paymentId == 0
                                                        && "PENDING".equals(
                                                        status
                                                )
                                        ) {

                                            paymentId =
                                                    item.optInt(
                                                            "payment_id",
                                                            0
                                                    );

                                            paymentPeriod =
                                                    period;

                                            dueDate =
                                                    item.optString(
                                                            "due_date",
                                                            ""
                                                    );

                                            double savedAmount =
                                                    item.optDouble(
                                                            "amount",
                                                            monthlyRent
                                                    );

                                            if (
                                                    savedAmount >
                                                            0
                                            ) {

                                                monthlyRent =
                                                        savedAmount;
                                            }
                                        }
                                    }
                                }

                                if (paymentId == 0) {

                                    paymentPeriod =
                                            highestPeriod + 1;

                                    dueDate =
                                            calculateDueDate(
                                                    moveInDate,
                                                    paymentPeriod
                                            );
                                }

                                updateAmountViews();

                                setPaymentButtonEnabled(
                                        true
                                );

                            } catch (Exception e) {

                                showToast(
                                        "Invalid payment response."
                                );

                                setPaymentButtonEnabled(
                                        false
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // AMOUNT
    // =========================================================

    private void updateAmountViews() {

        String formatted =
                formatMoney(
                        monthlyRent
                );

        if (rentAmountLabel != null) {

            rentAmountLabel.setText(
                    formatted
            );
        }

        if (totalDueLabel != null) {

            totalDueLabel.setText(
                    formatted
            );
        }

        if (btnPay instanceof TextView) {

            ((TextView) btnPay)
                    .setText(
                            getString(
                                    R.string.pay_amount,
                                    formatted
                            )
                    );
        }
    }

    // =========================================================
    // PAYMENT METHODS
    // =========================================================

    private void setupPaymentMethods() {

        if (
                methodGCash == null
                        || methodCash == null
                        || radioGCash == null
                        || radioCash == null
        ) {

            return;
        }

        methodGCash.setOnClickListener(v -> {

            methodGCash.setSelected(
                    true
            );

            methodCash.setSelected(
                    false
            );

            radioGCash.setBackgroundTintList(
                    ColorStateList.valueOf(
                            0xFF1B5E4C
                    )
            );

            radioCash.setBackgroundTintList(
                    ColorStateList.valueOf(
                            0xFFEFEFEF
                    )
            );
        });

        methodCash.setOnClickListener(v -> {

            methodGCash.setSelected(
                    false
            );

            methodCash.setSelected(
                    true
            );

            radioGCash.setBackgroundTintList(
                    ColorStateList.valueOf(
                            0xFFEFEFEF
                    )
            );

            radioCash.setBackgroundTintList(
                    ColorStateList.valueOf(
                            0xFF1B5E4C
                    )
            );
        });

        methodGCash.performClick();

        if (btnPay != null) {

            btnPay.setOnClickListener(v ->
                    handlePayment()
            );
        }
    }

    // =========================================================
    // HANDLE PAYMENT
    // =========================================================

    private void handlePayment() {

        if (bookingId <= 0) {

            showToast(
                    "Invalid booking."
            );

            return;
        }

        if (!"APPROVED".equals(
                bookingStatus
        )) {

            showToast(
                    "Booking must be approved before payment."
            );

            return;
        }

        if (monthlyRent <= 0) {

            showToast(
                    "Invalid payment amount."
            );

            return;
        }

        if (
                methodGCash != null
                        && methodGCash.isSelected()
        ) {

            showGcashQrBottomSheet(
                    formatMoney(
                            monthlyRent
                    )
            );

        } else {

            submitOnsitePayment();
        }
    }

    // =========================================================
    // ONSITE PAYMENT
    // =========================================================

    private void submitOnsitePayment() {

        setPaymentButtonEnabled(
                false
        );

        if (paymentId > 0) {

            navigateToSuccess(
                    formatMoney(
                            monthlyRent
                    )
            );

            return;
        }

        apiClient.createPayment(
                bookingId,
                paymentPeriod,
                dueDate,
                monthlyRent,
                "ONSITE",
                null,
                "PENDING",
                null,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            setPaymentButtonEnabled(
                                    true
                            );

                            showToast(
                                    "Unable to save payment."
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        handleCreatePaymentResponse(
                                response,
                                false
                        );
                    }
                }
        );
    }

    // =========================================================
    // QR BOTTOM SHEET
    // =========================================================

    private void showGcashQrBottomSheet(
            String amount
    ) {

        BottomSheetDialog bottomSheetDialog =
                new BottomSheetDialog(
                        this
                );

        View view =
                getLayoutInflater()
                        .inflate(
                                R.layout.gcash_qr_bottom_sheet,
                                findViewById(
                                        R.id.mainLayout
                                ),
                                false
                        );

        bottomSheetDialog.setContentView(
                view
        );

        TextView amountLabel =
                view.findViewById(
                        R.id.amountToPayLabel
                );

        if (amountLabel != null) {

            amountLabel.setText(
                    amount
            );
        }

        View btnClose =
                view.findViewById(
                        R.id.btnCloseQr
                );

        if (btnClose != null) {

            btnClose.setOnClickListener(v ->
                    bottomSheetDialog.dismiss()
            );
        }

        View btnDone =
                view.findViewById(
                        R.id.btnDonePayment
                );

        if (btnDone != null) {

            btnDone.setOnClickListener(v -> {

                btnDone.setEnabled(
                        false
                );

                bottomSheetDialog.dismiss();

                submitQrPayment();
            });
        }

        bottomSheetDialog.show();
    }

    // =========================================================
    // QR PAYMENT
    // =========================================================

    private void submitQrPayment() {

        setPaymentButtonEnabled(
                false
        );

        String paymentDate =
                getCurrentDate();

        String transactionRef =
                "QR-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(
                                0,
                                8
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (paymentId > 0) {

            apiClient.updatePaymentStatus(
                    paymentId,
                    "PAID",
                    paymentDate,
                    transactionRef,
                    new Callback() {

                        @Override
                        public void onFailure(
                                @NonNull Call call,
                                @NonNull IOException e
                        ) {

                            runOnUiThread(() -> {

                                setPaymentButtonEnabled(
                                        true
                                );

                                showToast(
                                        "Unable to complete payment."
                                );
                            });
                        }

                        @Override
                        public void onResponse(
                                @NonNull Call call,
                                @NonNull Response response
                        ) throws IOException {

                            handlePaidResponse(
                                    response
                            );
                        }
                    }
            );

            return;
        }

        apiClient.createPayment(
                bookingId,
                paymentPeriod,
                dueDate,
                monthlyRent,
                "QR",
                paymentDate,
                "PAID",
                transactionRef,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            setPaymentButtonEnabled(
                                    true
                            );

                            showToast(
                                    "Unable to complete payment."
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        handleCreatePaymentResponse(
                                response,
                                true
                        );
                    }
                }
        );
    }

    // =========================================================
    // CREATE PAYMENT RESPONSE
    // =========================================================

    private void handleCreatePaymentResponse(
            Response response,
            boolean paid
    ) throws IOException {

        String body = "";

        if (response.body() != null) {

            body =
                    response.body()
                            .string();
        }

        String result =
                body;

        runOnUiThread(() -> {

            setPaymentButtonEnabled(
                    true
            );

            try {

                JSONObject json =
                        new JSONObject(
                                result
                        );

                if (!json.optBoolean(
                        "success",
                        false
                )) {

                    showToast(
                            json.optString(
                                    "message",
                                    "Payment failed."
                            )
                    );

                    return;
                }

                paymentId =
                        json.optInt(
                                "payment_id",
                                paymentId
                        );

                navigateToSuccess(
                        formatMoney(
                                monthlyRent
                        )
                );

            } catch (Exception e) {

                showToast(
                        "Invalid payment response."
                );
            }
        });
    }

    // =========================================================
    // PAID RESPONSE
    // =========================================================

    private void handlePaidResponse(
            Response response
    ) throws IOException {

        String body = "";

        if (response.body() != null) {

            body =
                    response.body()
                            .string();
        }

        String result =
                body;

        runOnUiThread(() -> {

            setPaymentButtonEnabled(
                    true
            );

            try {

                JSONObject json =
                        new JSONObject(
                                result
                        );

                if (!json.optBoolean(
                        "success",
                        false
                )) {

                    showToast(
                            json.optString(
                                    "message",
                                    "Payment failed."
                            )
                    );

                    return;
                }

                navigateToSuccess(
                        formatMoney(
                                monthlyRent
                        )
                );

            } catch (Exception e) {

                showToast(
                        "Invalid payment response."
                );
            }
        });
    }

    // =========================================================
    // DUE DATE
    // =========================================================

    private String calculateDueDate(
            String firstDueDate,
            int period
    ) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            format.setLenient(
                    false
            );

            Date date =
                    format.parse(
                            firstDueDate
                    );

            if (date == null) {

                return getCurrentDate();
            }

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(
                    date
            );

            calendar.add(
                    Calendar.MONTH,
                    Math.max(
                            0,
                            period - 1
                    )
            );

            return format.format(
                    calendar.getTime()
            );

        } catch (Exception e) {

            return getCurrentDate();
        }
    }

    private String getCurrentDate() {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        return format.format(
                new Date()
        );
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    private void navigateToSuccess(
            String amount
    ) {

        Intent successIntent =
                new Intent(
                        this,
                        PaymentSuccessActivity.class
                );

        successIntent.putExtra(
                "AMOUNT",
                amount
        );

        successIntent.putExtra(
                "BOOKING_ID",
                bookingId
        );

        successIntent.putExtra(
                "PAYMENT_ID",
                paymentId
        );

        successIntent.putExtra(
                "PAYMENT_PERIOD",
                paymentPeriod
        );

        startActivity(
                successIntent
        );

        finish();
    }

    // =========================================================
    // BACK
    // =========================================================

    private void setupBackButton() {

        View btnBack =
                findViewById(
                        R.id.btnBack
                );

        if (btnBack != null) {

            btnBack.setOnClickListener(v ->
                    finish()
            );
        }
    }

    // =========================================================
    // BUTTON STATE
    // =========================================================

    private void setPaymentButtonEnabled(
            boolean enabled
    ) {

        if (btnPay == null) {
            return;
        }

        btnPay.setEnabled(
                enabled
        );

        btnPay.setAlpha(
                enabled
                        ? 1.0f
                        : 0.55f
        );
    }

    // =========================================================
    // MONEY
    // =========================================================

    private String formatMoney(
            double amount
    ) {

        NumberFormat format =
                NumberFormat.getCurrencyInstance(
                        new Locale(
                                "en",
                                "PH"
                        )
                );

        return format.format(
                amount
        );
    }

    // =========================================================
    // TOAST
    // =========================================================

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}