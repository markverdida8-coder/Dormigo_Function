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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordRequestsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();

    private LinearLayout requestsContainer;
    private TextView requestsCount;

    private int landlordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_requests);

        requestsContainer =
                findViewById(R.id.requestsContainer);

        requestsCount =
                findViewById(R.id.requestsCount);

        setupWindowInsets();

        View btnBack =
                findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        "DormigoPrefs",
                        MODE_PRIVATE
                );

        landlordId =
                prefs.getInt(
                        "userId",
                        0
                );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLandlordRequests();
    }

    // =========================================================
    // WINDOW INSETS
    // =========================================================

    private void setupWindowInsets() {

        View mainLayout =
                findViewById(R.id.mainLayout);

        if (mainLayout == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                mainLayout,
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
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
    // LOAD LANDLORD REQUESTS
    // =========================================================

    private void loadLandlordRequests() {

        if (landlordId <= 0) {
            showEmptyState(
                    "Please log in as a landlord to view booking requests."
            );
            return;
        }

        showLoadingState();

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {
                        showEmptyState(
                                "Unable to load your boarding houses."
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {
                            body = response.body().string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(body);

                            if (!json.optBoolean(
                                    "success",
                                    false
                            )) {
                                showEmptyState(
                                        "Unable to load your boarding houses."
                                );
                                return;
                            }

                            JSONArray data =
                                    json.optJSONArray("data");

                            Map<Integer, JSONObject> ownedHouseMap =
                                    new HashMap<>();

                            Set<Integer> ownedHouseIds =
                                    new HashSet<>();

                            if (data != null) {

                                for (int i = 0;
                                     i < data.length();
                                     i++) {

                                    JSONObject house =
                                            data.getJSONObject(i);

                                    int houseLandlordId =
                                            house.optInt(
                                                    "landlord_id",
                                                    0
                                            );

                                    if (houseLandlordId != landlordId) {
                                        continue;
                                    }

                                    int houseId =
                                            house.optInt(
                                                    "house_id",
                                                    0
                                            );

                                    if (houseId > 0) {
                                        ownedHouseIds.add(houseId);
                                        ownedHouseMap.put(
                                                houseId,
                                                house
                                        );
                                    }
                                }
                            }

                            if (ownedHouseIds.isEmpty()) {
                                showEmptyState(
                                        "No boarding houses are linked to this landlord account."
                                );
                                return;
                            }

                            loadOwnedRooms(
                                    ownedHouseIds,
                                    ownedHouseMap
                            );

                        } catch (Exception e) {
                            showEmptyState(
                                    "Invalid boarding house response."
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD ROOMS BELONGING TO LANDLORD
    // =========================================================

    private void loadOwnedRooms(
            Set<Integer> ownedHouseIds,
            Map<Integer, JSONObject> ownedHouseMap
    ) {

        apiClient.getRooms(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {
                        showEmptyState(
                                "Unable to load rooms."
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {
                            body = response.body().string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(body);

                            JSONArray data =
                                    json.optJSONArray("data");

                            Map<Integer, JSONObject> ownedRoomMap =
                                    new HashMap<>();

                            if (data != null) {

                                for (int i = 0;
                                     i < data.length();
                                     i++) {

                                    JSONObject room =
                                            data.getJSONObject(i);

                                    int houseId =
                                            room.optInt(
                                                    "house_id",
                                                    0
                                            );

                                    if (!ownedHouseIds.contains(houseId)) {
                                        continue;
                                    }

                                    int roomId =
                                            room.optInt(
                                                    "room_id",
                                                    0
                                            );

                                    if (roomId > 0) {
                                        ownedRoomMap.put(
                                                roomId,
                                                room
                                        );
                                    }
                                }
                            }

                            if (ownedRoomMap.isEmpty()) {
                                showEmptyState(
                                        "No rooms are linked to your boarding houses."
                                );
                                return;
                            }

                            loadPendingBookings(
                                    ownedHouseMap,
                                    ownedRoomMap
                            );

                        } catch (Exception e) {
                            showEmptyState(
                                    "Invalid room response."
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD PENDING BOOKINGS
    // =========================================================

    private void loadPendingBookings(
            Map<Integer, JSONObject> ownedHouseMap,
            Map<Integer, JSONObject> ownedRoomMap
    ) {

        apiClient.getBookings(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {
                        showEmptyState(
                                "Unable to load booking requests."
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {
                            body = response.body().string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(body);

                            if (!json.optBoolean(
                                    "success",
                                    false
                            )) {
                                showEmptyState(
                                        "Unable to load booking requests."
                                );
                                return;
                            }

                            JSONArray data =
                                    json.optJSONArray("data");

                            JSONArray pendingBookings =
                                    new JSONArray();

                            if (data != null) {

                                for (int i = 0;
                                     i < data.length();
                                     i++) {

                                    JSONObject booking =
                                            data.getJSONObject(i);

                                    int roomId =
                                            booking.optInt(
                                                    "room_id",
                                                    0
                                            );

                                    String status =
                                            booking.optString(
                                                    "status",
                                                    ""
                                            );

                                    if (
                                            ownedRoomMap.containsKey(roomId)
                                                    && "PENDING".equalsIgnoreCase(status)
                                    ) {
                                        pendingBookings.put(booking);
                                    }
                                }
                            }

                            if (pendingBookings.length() == 0) {
                                showEmptyState(
                                        "You have no pending booking requests."
                                );
                                return;
                            }

                            loadStudents(
                                    pendingBookings,
                                    ownedHouseMap,
                                    ownedRoomMap
                            );

                        } catch (Exception e) {
                            showEmptyState(
                                    "Invalid booking response."
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD STUDENT DETAILS
    // =========================================================

    private void loadStudents(
            JSONArray pendingBookings,
            Map<Integer, JSONObject> ownedHouseMap,
            Map<Integer, JSONObject> ownedRoomMap
    ) {

        apiClient.getUsers(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {
                        renderRequests(
                                pendingBookings,
                                ownedHouseMap,
                                ownedRoomMap,
                                new HashMap<>()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        Map<Integer, JSONObject> userMap =
                                new HashMap<>();

                        String body = "";

                        if (response.body() != null) {
                            body = response.body().string();
                        }

                        try {

                            JSONObject json =
                                    new JSONObject(body);

                            JSONArray data =
                                    json.optJSONArray("data");

                            if (data != null) {

                                for (int i = 0;
                                     i < data.length();
                                     i++) {

                                    JSONObject user =
                                            data.getJSONObject(i);

                                    int userId =
                                            user.optInt(
                                                    "user_id",
                                                    0
                                            );

                                    if (userId > 0) {
                                        userMap.put(
                                                userId,
                                                user
                                        );
                                    }
                                }
                            }

                        } catch (Exception ignored) {
                        }

                        renderRequests(
                                pendingBookings,
                                ownedHouseMap,
                                ownedRoomMap,
                                userMap
                        );
                    }
                }
        );
    }

    // =========================================================
    // RENDER REQUESTS
    // =========================================================

    private void renderRequests(
            JSONArray bookings,
            Map<Integer, JSONObject> houseMap,
            Map<Integer, JSONObject> roomMap,
            Map<Integer, JSONObject> userMap
    ) {

        runOnUiThread(() -> {

            if (requestsContainer == null) {
                return;
            }

            requestsContainer.removeAllViews();

            int count =
                    bookings.length();

            if (requestsCount != null) {
                requestsCount.setText(
                        count == 1
                                ? "1 pending request"
                                : count + " pending requests"
                );
            }

            for (int i = 0;
                 i < bookings.length();
                 i++) {

                try {

                    JSONObject booking =
                            bookings.getJSONObject(i);

                    createRequestCard(
                            booking,
                            houseMap,
                            roomMap,
                            userMap,
                            i
                    );

                } catch (Exception ignored) {
                }
            }
        });
    }

    // =========================================================
    // CREATE REQUEST CARD
    // =========================================================

    private void createRequestCard(
            JSONObject booking,
            Map<Integer, JSONObject> houseMap,
            Map<Integer, JSONObject> roomMap,
            Map<Integer, JSONObject> userMap,
            int position
    ) {

        int bookingId =
                booking.optInt(
                        "booking_id",
                        0
                );

        int studentId =
                booking.optInt(
                        "user_id",
                        0
                );

        int roomId =
                booking.optInt(
                        "room_id",
                        0
                );

        JSONObject room =
                roomMap.get(roomId);

        int houseId =
                room != null
                        ? room.optInt(
                        "house_id",
                        0
                )
                        : 0;

        JSONObject house =
                houseMap.get(houseId);

        JSONObject student =
                userMap.get(studentId);

        String studentName =
                student != null
                        ? student.optString(
                        "full_name",
                        "Student"
                )
                        : "Student #" + studentId;

        String studentEmail =
                student != null
                        ? student.optString(
                        "email",
                        ""
                )
                        : "";

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
                )
                        : "";

        String roomType =
                room != null
                        ? room.optString(
                        "room_type",
                        ""
                )
                        : "";

        String moveInDate =
                booking.optString(
                        "move_in_date",
                        "Not specified"
                );

        int durationMonths =
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

        String messageToLandlord =
                booking.optString(
                        "message_to_landlord",
                        ""
                ).trim();

        LinearLayout card =
                new LinearLayout(this);

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
                dp(position == 0 ? 16 : 12);

        requestsContainer.addView(
                card,
                cardParams
        );

        // -----------------------------------------------------
        // Student header
        // -----------------------------------------------------

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.addView(
                header,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView initials =
                createTextView(
                        getInitials(studentName),
                        13,
                        Color.rgb(27, 94, 76)
                );

        initials.setGravity(
                Gravity.CENTER
        );

        initials.setTypeface(
                null,
                Typeface.BOLD
        );

        initials.setBackgroundResource(
                R.drawable.bg_circle_green_light
        );

        header.addView(
                initials,
                new LinearLayout.LayoutParams(
                        dp(38),
                        dp(38)
                )
        );

        LinearLayout studentInfo =
                new LinearLayout(this);

        studentInfo.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams studentInfoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        studentInfoParams.leftMargin =
                dp(12);

        header.addView(
                studentInfo,
                studentInfoParams
        );

        TextView studentNameText =
                createTextView(
                        studentName,
                        14,
                        Color.rgb(26, 26, 26)
                );

        studentNameText.setTypeface(
                null,
                Typeface.BOLD
        );

        studentInfo.addView(
                studentNameText
        );

        TextView studentDetailText =
                createTextView(
                        studentEmail.isEmpty()
                                ? "Dormigo Student"
                                : studentEmail,
                        11,
                        Color.rgb(154, 154, 158)
                );

        LinearLayout.LayoutParams studentDetailParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        studentDetailParams.topMargin =
                dp(2);

        studentInfo.addView(
                studentDetailText,
                studentDetailParams
        );

        TextView statusBadge =
                createTextView(
                        "PENDING",
                        10,
                        Color.rgb(138, 109, 11)
                );

        statusBadge.setTypeface(
                null,
                Typeface.BOLD
        );

        statusBadge.setGravity(
                Gravity.CENTER
        );

        statusBadge.setPadding(
                dp(8),
                dp(3),
                dp(8),
                dp(3)
        );

        statusBadge.setBackground(
                createRoundedBackground(
                        Color.rgb(255, 248, 216),
                        dp(8)
                )
        );

        header.addView(statusBadge);

        addDivider(card);

        addInfoText(
                card,
                "Boarding house: " + houseName
        );

        String roomLabel =
                buildRoomLabel(
                        roomNumber,
                        roomType,
                        roomId
                );

        addInfoText(
                card,
                "Room: " + roomLabel
        );

        addInfoText(
                card,
                "Move-in date: " + moveInDate
        );

        addInfoText(
                card,
                "Stay duration: "
                        + durationMonths
                        + " month"
                        + (durationMonths == 1 ? "" : "s")
        );

        addInfoText(
                card,
                "Monthly rent: "
                        + formatMoney(monthlyRent)
        );

        addInfoText(
                card,
                "Total amount: "
                        + formatMoney(totalAmount)
        );

        if (!messageToLandlord.isEmpty()) {

            TextView messageLabel =
                    createTextView(
                            "Message from student",
                            12,
                            Color.rgb(26, 26, 26)
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

            TextView messageText =
                    createTextView(
                            messageToLandlord,
                            13,
                            Color.rgb(110, 110, 115)
                    );

            LinearLayout.LayoutParams messageParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            messageParams.topMargin =
                    dp(6);

            card.addView(
                    messageText,
                    messageParams
            );
        }

        // -----------------------------------------------------
        // Actions
        // -----------------------------------------------------

        LinearLayout actions =
                new LinearLayout(this);

        actions.setOrientation(
                LinearLayout.HORIZONTAL
        );

        actions.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        actionsParams.topMargin =
                dp(16);

        card.addView(
                actions,
                actionsParams
        );

        TextView approveButton =
                createActionButton(
                        "Approve",
                        true
                );

        TextView declineButton =
                createActionButton(
                        "Decline",
                        false
                );

        TextView chatButton =
                createActionButton(
                        "Chat",
                        false
                );

        LinearLayout.LayoutParams approveParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1f
                );

        actions.addView(
                approveButton,
                approveParams
        );

        LinearLayout.LayoutParams declineParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1f
                );

        declineParams.leftMargin =
                dp(8);

        actions.addView(
                declineButton,
                declineParams
        );

        LinearLayout.LayoutParams chatParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1f
                );

        chatParams.leftMargin =
                dp(8);

        actions.addView(
                chatButton,
                chatParams
        );

        approveButton.setOnClickListener(v ->
                updateBookingStatus(
                        bookingId,
                        "APPROVED",
                        studentName,
                        approveButton,
                        declineButton,
                        chatButton
                )
        );

        declineButton.setOnClickListener(v ->
                updateBookingStatus(
                        bookingId,
                        "DECLINED",
                        studentName,
                        approveButton,
                        declineButton,
                        chatButton
                )
        );

        chatButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            LandlordChatActivity.class
                    );

            intent.putExtra(
                    "STUDENT_ID",
                    studentId
            );

            intent.putExtra(
                    "STUDENT_NAME",
                    studentName
            );

            intent.putExtra(
                    "BOOKING_ID",
                    bookingId
            );

            startActivity(intent);
        });
    }

    // =========================================================
    // APPROVE / DECLINE
    // =========================================================

    private void updateBookingStatus(
            int bookingId,
            String newStatus,
            String studentName,
            View approveButton,
            View declineButton,
            View chatButton
    ) {

        if (bookingId <= 0) {
            showToast("Invalid booking request.");
            return;
        }

        approveButton.setEnabled(false);
        declineButton.setEnabled(false);
        chatButton.setEnabled(false);

        apiClient.updateBookingStatus(
                bookingId,
                newStatus,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {
                            approveButton.setEnabled(true);
                            declineButton.setEnabled(true);
                            chatButton.setEnabled(true);
                            showToast(
                                    "Unable to update booking request."
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
                            body = response.body().string();
                        }

                        String result = body;

                        runOnUiThread(() -> {

                            try {

                                JSONObject json =
                                        new JSONObject(result);

                                if (!json.optBoolean(
                                        "success",
                                        false
                                )) {
                                    approveButton.setEnabled(true);
                                    declineButton.setEnabled(true);
                                    chatButton.setEnabled(true);

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Unable to update booking request."
                                            )
                                    );
                                    return;
                                }

                                String actionText =
                                        "APPROVED".equals(newStatus)
                                                ? "approved"
                                                : "declined";

                                showToast(
                                        "Request "
                                                + actionText
                                                + " for "
                                                + studentName
                                );

                                loadLandlordRequests();

                            } catch (Exception e) {
                                approveButton.setEnabled(true);
                                declineButton.setEnabled(true);
                                chatButton.setEnabled(true);
                                showToast(
                                        "Invalid server response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private void showLoadingState() {

        runOnUiThread(() -> {

            if (requestsContainer == null) {
                return;
            }

            requestsContainer.removeAllViews();

            if (requestsCount != null) {
                requestsCount.setText(
                        "Loading requests..."
                );
            }
        });
    }

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
                        "0 pending requests"
                );
            }

            TextView emptyText =
                    createTextView(
                            message,
                            14,
                            Color.rgb(110, 110, 115)
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

    private TextView createTextView(
            String text,
            int size,
            int color
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);

        return textView;
    }

    private TextView createActionButton(
            String text,
            boolean primary
    ) {

        TextView button =
                createTextView(
                        text,
                        12,
                        primary
                                ? Color.WHITE
                                : Color.rgb(27, 94, 76)
                );

        button.setGravity(
                Gravity.CENTER
        );

        button.setTypeface(
                null,
                Typeface.BOLD
        );

        button.setClickable(true);
        button.setFocusable(true);

        if (primary) {
            button.setBackgroundResource(
                    R.drawable.bg_button_filled
            );
        } else {
            GradientDrawable background =
                    createRoundedBackground(
                            Color.WHITE,
                            dp(10)
                    );

            background.setStroke(
                    dp(1),
                    Color.rgb(220, 225, 223)
            );

            button.setBackground(background);
        }

        return button;
    }

    private void addDivider(
            LinearLayout card
    ) {

        View divider =
                new View(this);

        divider.setBackgroundColor(
                Color.rgb(239, 239, 239)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        params.topMargin =
                dp(14);

        params.bottomMargin =
                dp(8);

        card.addView(
                divider,
                params
        );
    }

    private void addInfoText(
            LinearLayout card,
            String text
    ) {

        TextView textView =
                createTextView(
                        text,
                        12,
                        Color.rgb(110, 110, 115)
                );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin =
                dp(5);

        card.addView(
                textView,
                params
        );
    }

    private GradientDrawable createRoundedBackground(
            int color,
            int radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    private String buildRoomLabel(
            String roomNumber,
            String roomType,
            int roomId
    ) {

        boolean hasNumber =
                roomNumber != null
                        && !roomNumber.trim().isEmpty();

        boolean hasType =
                roomType != null
                        && !roomType.trim().isEmpty();

        if (hasNumber && hasType) {
            return roomNumber.trim()
                    + " • "
                    + roomType.trim();
        }

        if (hasNumber) {
            return roomNumber.trim();
        }

        if (hasType) {
            return roomType.trim();
        }

        return "Room ID " + roomId;
    }

    private String getInitials(
            String name
    ) {

        if (name == null
                || name.trim().isEmpty()) {
            return "ST";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0]
                    .substring(0, 1)
                    .toUpperCase(Locale.US);
        }

        return (
                parts[0].substring(0, 1)
                        + parts[parts.length - 1]
                        .substring(0, 1)
        ).toUpperCase(Locale.US);
    }

    private double parseAmount(
            String value
    ) {

        try {
            return Double.parseDouble(
                    value
                            .replace(",", "")
                            .replace("₱", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatMoney(
            double amount
    ) {

        return String.format(
                Locale.US,
                "₱%,.2f",
                amount
        );
    }

    private int dp(
            int value
    ) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
