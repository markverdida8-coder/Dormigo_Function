package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BookingRequestsActivity extends AppCompatActivity {

    private ApiClient apiClient;

    private LinearLayout requestsContainer;
    private TextView requestsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_booking_requests
        );

        apiClient =
                new ApiClient();

        requestsContainer =
                findViewById(
                        R.id.requestsContainer
                );

        requestsCount =
                findViewById(
                        R.id.requestsCount
                );

        setupWindowInsets();

        setupBottomNavigation();
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
                                            .systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            0
                    );

                    View bottomNav =
                            findViewById(
                                    R.id.bottomNav
                            );

                    if (bottomNav != null) {

                        bottomNav.setPadding(
                                0,
                                0,
                                0,
                                systemBars.bottom
                        );
                    }

                    return insets;
                }
        );
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        loadBookings();
    }

    // =========================================================
    // LOAD BOOKINGS
    // =========================================================

    private void loadBookings() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "DormigoPrefs",
                        MODE_PRIVATE
                );

        int userId =
                prefs.getInt(
                        "userId",
                        0
                );

        if (userId <= 0) {

            showEmptyState(
                    "Please log in to view your booking requests."
            );

            return;
        }

        apiClient.getBookings(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                showEmptyState(
                                        "Unable to connect to server."
                                )
                        );
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

                                boolean success =
                                        json.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    showEmptyState(
                                            json.optString(
                                                    "message",
                                                    "Failed to load booking requests."
                                            )
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (data == null) {

                                    showEmptyState(
                                            "No booking requests yet."
                                    );

                                    return;
                                }

                                JSONArray myBookings =
                                        new JSONArray();

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject booking =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int bookingUserId =
                                            booking.optInt(
                                                    "user_id",
                                                    0
                                            );

                                    if (
                                            bookingUserId
                                                    == userId
                                    ) {

                                        myBookings.put(
                                                booking
                                        );
                                    }
                                }

                                if (
                                        myBookings.length()
                                                == 0
                                ) {

                                    showEmptyState(
                                            "You have no booking requests yet."
                                    );

                                    return;
                                }

                                loadRooms(
                                        myBookings
                                );

                            } catch (Exception e) {

                                showEmptyState(
                                        "Invalid server response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // LOAD ROOMS
    // =========================================================

    private void loadRooms(
            JSONArray bookings
    ) {

        apiClient.getRooms(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        loadPaymentsAndRender(
                                bookings,
                                new HashMap<>(),
                                new HashMap<>()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        final Map<Integer, JSONObject> roomMap =
                                new HashMap<>();

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            if (data != null) {

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject room =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int roomId =
                                            room.optInt(
                                                    "room_id",
                                                    0
                                            );

                                    if (roomId > 0) {

                                        roomMap.put(
                                                roomId,
                                                room
                                        );
                                    }
                                }
                            }

                        } catch (Exception ignored) {
                        }

                        loadBoardingHouses(
                                bookings,
                                roomMap
                        );
                    }
                }
        );
    }

    // =========================================================
    // LOAD BOARDING HOUSES
    // =========================================================

    private void loadBoardingHouses(
            JSONArray bookings,
            Map<Integer, JSONObject> roomMap
    ) {

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        loadPaymentsAndRender(
                                bookings,
                                roomMap,
                                new HashMap<>()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        final Map<Integer, JSONObject> houseMap =
                                new HashMap<>();

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            if (data != null) {

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject house =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int houseId =
                                            house.optInt(
                                                    "house_id",
                                                    0
                                            );

                                    if (houseId > 0) {

                                        houseMap.put(
                                                houseId,
                                                house
                                        );
                                    }
                                }
                            }

                        } catch (Exception ignored) {
                        }

                        loadPaymentsAndRender(
                                bookings,
                                roomMap,
                                houseMap
                        );
                    }
                }
        );
    }

    private void loadPaymentsAndRender(
            JSONArray bookings,
            Map<Integer, JSONObject> roomMap,
            Map<Integer, JSONObject> houseMap
    ) {
        apiClient.getPayments(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                renderBookings(bookings, roomMap, houseMap, new HashMap<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Map<Integer, Boolean> paidBookings = new HashMap<>();
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    JSONArray data = json.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject p = data.getJSONObject(i);
                            int bId = p.optInt("booking_id", 0);
                            String pStatus = p.optString("status", "").toUpperCase();
                            if (bId > 0 && (pStatus.equals("COMPLETED") || pStatus.equals("PAID") || pStatus.equals("SUCCESS") || pStatus.equals("CONFIRMED"))) {
                                paidBookings.put(bId, true);
                            }
                        }
                    }
                } catch (Exception ignored) {}

                renderBookings(bookings, roomMap, houseMap, paidBookings);
            }
        });
    }

    // =========================================================
    // RENDER BOOKINGS
    // =========================================================

    private void renderBookings(
            JSONArray bookings,
            Map<Integer, JSONObject> roomMap,
            Map<Integer, JSONObject> houseMap,
            Map<Integer, Boolean> paidBookings
    ) {

        runOnUiThread(() -> {

            if (
                    requestsContainer == null
                            || requestsCount == null
            ) {

                return;
            }

            requestsContainer.removeAllViews();

            int count =
                    bookings.length();

            requestsCount.setText(
                    count == 1
                            ? "1 request sent"
                            : count + " requests sent"
            );

            for (
                    int i = 0;
                    i < bookings.length();
                    i++
            ) {

                try {

                    JSONObject booking =
                            bookings.getJSONObject(
                                    i
                            );

                    createBookingCard(
                            booking,
                            roomMap,
                            houseMap,
                            paidBookings,
                            i
                    );

                } catch (Exception ignored) {
                }
            }
        });
    }

    // =========================================================
    // CREATE BOOKING CARD
    // =========================================================

    private void createBookingCard(
            JSONObject booking,
            Map<Integer, JSONObject> roomMap,
            Map<Integer, JSONObject> houseMap,
            Map<Integer, Boolean> paidBookings,
            int position
    ) {

        int bookingId =
                booking.optInt(
                        "booking_id",
                        0
                );

        int roomId =
                booking.optInt(
                        "room_id",
                        0
                );

        JSONObject room =
                roomMap.get(
                        roomId
                );

        int houseId =
                room != null
                        ? room.optInt(
                        "house_id",
                        0
                )
                        : 0;

        JSONObject house =
                houseMap.get(
                        houseId
                );

        String houseName =
                house != null
                        ? house.optString(
                        "house_name",
                        "Boarding House"
                )
                        : "Boarding House";

        String roomNumber =
                room != null
                        ? room.optString(
                        "room_number",
                        ""
                ).trim()
                        : "";

        String roomType =
                room != null
                        ? room.optString(
                        "room_type",
                        ""
                ).trim()
                        : "";

        String status =
                booking.optString(
                                "status",
                                "PENDING"
                        )
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        String moveInDate =
                booking.optString(
                        "move_in_date",
                        "Not specified"
                );

        int duration =
                booking.optInt(
                        "duration_months",
                        0
                );

        double monthlyRent =
                parseAmount(
                        booking.optString(
                                "agreed_monthly_rent",
                                "0"
                        )
                );

        double totalAmount =
                parseAmount(
                        booking.optString(
                                "agreed_total_amount",
                                "0"
                        )
                );

        String createdAt =
                booking.optString(
                        "created_at",
                        ""
                );

        String messageToLandlord =
                booking.optString(
                        "message_to_landlord",
                        ""
                ).trim();

        // =====================================================
        // CARD
        // =====================================================

        LinearLayout card =
                new LinearLayout(
                        this
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        card.setBackgroundResource(
                R.drawable.bg_card_rounded
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin =
                dp(
                        position == 0
                                ? 24
                                : 16
                );

        requestsContainer.addView(
                card,
                cardParams
        );

        // =====================================================
        // TOP ROW
        // =====================================================

        LinearLayout topRow =
                new LinearLayout(
                        this
                );

        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.addView(
                topRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // =====================================================
        // HOUSE + ROOM
        // =====================================================

        LinearLayout leftInfo =
                new LinearLayout(
                        this
                );

        leftInfo.setOrientation(
                LinearLayout.VERTICAL
        );

        topRow.addView(
                leftInfo,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView houseText =
                createTextView(
                        houseName,
                        15,
                        Color.rgb(
                                26,
                                26,
                                26
                        )
                );

        houseText.setTypeface(
                null,
                Typeface.BOLD
        );

        leftInfo.addView(
                houseText
        );

        String roomLabel;

        if (
                !roomNumber.isEmpty()
                        && !roomType.isEmpty()
        ) {

            roomLabel =
                    "Room "
                            + roomNumber
                            + " • "
                            + roomType;

        } else if (
                !roomNumber.isEmpty()
        ) {

            roomLabel =
                    "Room "
                            + roomNumber;

        } else if (
                !roomType.isEmpty()
        ) {

            roomLabel =
                    roomType;

        } else {

            roomLabel =
                    "Room ID: "
                            + roomId;
        }

        TextView roomText =
                createTextView(
                        roomLabel,
                        13,
                        Color.rgb(
                                154,
                                154,
                                158
                        )
                );

        LinearLayout.LayoutParams roomParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        roomParams.topMargin =
                dp(4);

        leftInfo.addView(
                roomText,
                roomParams
        );

        // =====================================================
        // STATUS BADGE
        // =====================================================

        TextView statusBadge =
                createStatusBadge(
                        status
                );

        topRow.addView(
                statusBadge
        );

        // =====================================================
        // DIVIDER
        // =====================================================

        View divider =
                new View(
                        this
                );

        divider.setBackgroundColor(
                Color.rgb(
                        239,
                        239,
                        239
                )
        );

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        dividerParams.topMargin =
                dp(16);

        card.addView(
                divider,
                dividerParams
        );

        // =====================================================
        // BOOKING INFORMATION
        // =====================================================

        addInfoText(
                card,
                "Booking ID: "
                        + bookingId
        );

        addInfoText(
                card,
                "Move-in date: "
                        + moveInDate
        );

        addInfoText(
                card,
                "Stay duration: "
                        + duration
                        + " month"
                        + (
                        duration == 1
                                ? ""
                                : "s"
                )
        );

        addInfoText(
                card,
                "Monthly rent: "
                        + formatMoney(
                        monthlyRent
                )
        );

        addInfoText(
                card,
                "Total amount: "
                        + formatMoney(
                        totalAmount
                )
        );

        // =====================================================
        // MESSAGE TO LANDLORD
        // =====================================================

        if (!messageToLandlord.isEmpty()) {

            TextView messageLabel =
                    createTextView(
                            "Message to landlord",
                            13,
                            Color.rgb(
                                    26,
                                    26,
                                    26
                            )
                    );

            messageLabel.setTypeface(
                    null,
                    Typeface.BOLD
            );

            LinearLayout.LayoutParams messageLabelParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            messageLabelParams.topMargin =
                    dp(14);

            card.addView(
                    messageLabel,
                    messageLabelParams
            );

            TextView landlordMessageText =
                    createTextView(
                            messageToLandlord,
                            13,
                            Color.rgb(
                                    110,
                                    110,
                                    115
                            )
                    );

            landlordMessageText.setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
            );

            landlordMessageText.setBackground(
                    createRoundedBackground(
                            Color.rgb(
                                    247,
                                    247,
                                    247
                            )
                    )
            );

            LinearLayout.LayoutParams landlordMessageParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            landlordMessageParams.topMargin =
                    dp(8);

            card.addView(
                    landlordMessageText,
                    landlordMessageParams
            );
        }

        // =====================================================
        // STATUS MESSAGE
        // =====================================================

        TextView messageText =
                createTextView(
                        getStatusMessage(
                                status
                        ),
                        13,
                        Color.rgb(
                                110,
                                110,
                                115
                        )
                );

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        messageParams.topMargin =
                dp(12);

        card.addView(
                messageText,
                messageParams
        );

        // =====================================================
        // PAY NOW
        // ONLY APPROVED BOOKINGS
        // =====================================================

        if (
                "APPROVED".equals(
                        status
                )
                        && bookingId > 0
        ) {

            Boolean paidVal = paidBookings.get(bookingId);
            boolean isPaid = paidVal != null && paidVal;

            if (!isPaid) {

                String paymentRoomName;

                if (!roomNumber.isEmpty()) {

                    paymentRoomName =
                            roomNumber;

                } else if (!roomType.isEmpty()) {

                    paymentRoomName =
                            roomType;

                } else {

                    paymentRoomName =
                            "Room";
                }

                TextView payButton =
                        createTextView(
                                "Pay Now",
                                15,
                                Color.WHITE
                        );

                payButton.setTypeface(
                        null,
                        Typeface.BOLD
                );

                payButton.setGravity(
                        Gravity.CENTER
                );

                payButton.setBackgroundResource(
                        R.drawable.bg_button_filled
                );

                payButton.setClickable(
                        true
                );

                payButton.setFocusable(
                        true
                );

                LinearLayout.LayoutParams payButtonParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dp(48)
                        );

                payButtonParams.topMargin =
                        dp(16);

                payButton.setOnClickListener(v ->
                        openPaymentActivity(
                                bookingId,
                                houseName,
                                paymentRoomName
                        )
                );

                card.addView(
                        payButton,
                        payButtonParams
                );

            } else {

                TextView paidText =
                        createTextView(
                                "Payment Successful ✓",
                                14,
                                Color.parseColor("#1B5E4C")
                        );

                paidText.setTypeface(
                        null,
                        Typeface.BOLD
                );

                LinearLayout.LayoutParams paidParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                paidParams.topMargin =
                        dp(16);

                card.addView(
                        paidText,
                        paidParams
                );
            }
        }

        // =====================================================
        // CREATED DATE
        // =====================================================

        if (!createdAt.isEmpty()) {

            addSmallInfoText(
                    card,
                    "Submitted: "
                            + createdAt
            );
        }
    }

    // =========================================================
    // OPEN PAYMENT ACTIVITY
    // =========================================================

    private void openPaymentActivity(
            int bookingId,
            String houseName,
            String roomName
    ) {

        Intent intent =
                new Intent(
                        BookingRequestsActivity.this,
                        PaymentActivity.class
                );

        intent.putExtra(
                "BOOKING_ID",
                bookingId
        );

        intent.putExtra(
                "HOUSE_NAME",
                houseName
        );

        intent.putExtra(
                "ROOM_NAME",
                roomName
        );

        startActivity(
                intent
        );
    }

    // =========================================================
    // INFO TEXT
    // =========================================================

    private void addInfoText(
            LinearLayout card,
            String text
    ) {

        TextView textView =
                createTextView(
                        text,
                        13,
                        Color.rgb(
                                110,
                                110,
                                115
                        )
                );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dp(6);

        card.addView(
                textView,
                params
        );
    }

    private void addSmallInfoText(
            LinearLayout card,
            String text
    ) {

        TextView textView =
                createTextView(
                        text,
                        12,
                        Color.rgb(
                                154,
                                154,
                                158
                        )
                );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dp(8);

        card.addView(
                textView,
                params
        );
    }

    // =========================================================
    // STATUS BADGE
    // =========================================================

    private TextView createStatusBadge(
            String status
    ) {

        TextView badge =
                new TextView(
                        this
                );

        String displayStatus =
                status;

        if (
                "REJECTED".equals(
                        status
                )
                        || "DECLINED".equals(
                        status
                )
        ) {

            displayStatus =
                    "DECLINED";
        }

        badge.setText(
                displayStatus
        );

        badge.setTextSize(
                12
        );

        badge.setTypeface(
                null,
                Typeface.BOLD
        );

        badge.setGravity(
                Gravity.CENTER
        );

        badge.setPadding(
                dp(10),
                dp(4),
                dp(10),
                dp(4)
        );

        if (
                "PENDING".equals(
                        status
                )
        ) {

            badge.setTextColor(
                    Color.rgb(
                            138,
                            109,
                            11
                    )
            );

            badge.setBackground(
                    createRoundedBackground(
                            Color.rgb(
                                    255,
                                    248,
                                    216
                            )
                    )
            );

        } else if (
                "APPROVED".equals(
                        status
                )
        ) {

            badge.setTextColor(
                    Color.rgb(
                            27,
                            94,
                            76
                    )
            );

            badge.setBackground(
                    createRoundedBackground(
                            Color.rgb(
                                    242,
                                    249,
                                    247
                            )
                    )
            );

        } else {

            badge.setTextColor(
                    Color.rgb(
                            245,
                            74,
                            61
                    )
            );

            badge.setBackground(
                    createRoundedBackground(
                            Color.rgb(
                                    255,
                                    241,
                                    240
                            )
                    )
            );
        }

        return badge;
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmptyState(
            String message
    ) {

        runOnUiThread(() -> {

            if (requestsContainer == null) {
                return;
            }

            requestsContainer.removeAllViews();

            if (requestsCount != null) {

                requestsCount.setText(
                        "0 requests sent"
                );
            }

            TextView emptyText =
                    createTextView(
                            message,
                            14,
                            Color.rgb(
                                    110,
                                    110,
                                    115
                            )
                    );

            emptyText.setGravity(
                    Gravity.CENTER
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.topMargin =
                    dp(32);

            requestsContainer.addView(
                    emptyText,
                    params
            );
        });
    }

    // =========================================================
    // STATUS MESSAGE
    // =========================================================

    private String getStatusMessage(
            String status
    ) {

        if (
                "PENDING".equals(
                        status
                )
        ) {

            return "Waiting for landlord confirmation.";

        } else if (
                "APPROVED".equals(
                        status
                )
        ) {

            return "Your booking request has been approved.";

        } else if (
                "REJECTED".equals(
                        status
                )
                        || "DECLINED".equals(
                        status
                )
        ) {

            return "Your booking request was declined.";

        } else if (
                "CANCELLED".equals(
                        status
                )
        ) {

            return "This booking request was cancelled.";

        } else if (
                "ACTIVE".equals(
                        status
                )
        ) {

            return "Your stay is currently active.";

        } else if (
                "COMPLETED".equals(
                        status
                )
        ) {

            return "Your stay has been completed.";
        }

        return "Booking status: "
                + status;
    }

    // =========================================================
    // CREATE TEXT VIEW
    // =========================================================

    private TextView createTextView(
            String text,
            int size,
            int color
    ) {

        TextView textView =
                new TextView(
                        this
                );

        textView.setText(
                text
        );

        textView.setTextSize(
                size
        );

        textView.setTextColor(
                color
        );

        return textView;
    }

    // =========================================================
    // ROUNDED BACKGROUND
    // =========================================================

    private GradientDrawable createRoundedBackground(
            int color
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                color
        );

        drawable.setCornerRadius(
                dp(8)
        );

        return drawable;
    }

    // =========================================================
    // PARSE MONEY
    // =========================================================

    private double parseAmount(
            String value
    ) {

        try {

            return Double.parseDouble(
                    value
                            .replace(
                                    ",",
                                    ""
                            )
                            .replace(
                                    "₱",
                                    ""
                            )
                            .trim()
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // FORMAT MONEY
    // =========================================================

    private String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.US,
                "₱%,.2f",
                amount
        );
    }

    // =========================================================
    // DP
    // =========================================================

    private int dp(
            int value
    ) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        BottomNavigationView bottomNav =
                findViewById(
                        R.id.bottomNav
                );

        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(
                R.id.nav_requests
        );

        bottomNav.setOnItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();

                    if (
                            id == R.id.nav_home
                    ) {

                        Intent intent =
                                new Intent(
                                        this,
                                        HomeActivity.class
                                );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(
                                intent
                        );

                        finish();

                        return true;

                    } else if (
                            id == R.id.nav_explore
                    ) {

                        Intent intent =
                                new Intent(
                                        this,
                                        BoardingHouseListingsActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        return true;

                    } else if (
                            id == R.id.nav_chats
                    ) {

                        Intent intent =
                                new Intent(
                                        this,
                                        ChatHistoryActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        return true;

                    } else if (
                            id == R.id.nav_profile
                    ) {

                        Intent intent =
                                new Intent(
                                        this,
                                        ProfileActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        return true;
                    }

                    return true;
                }
        );
    }
}