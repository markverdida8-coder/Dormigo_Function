package com.dormigo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final ApiClient apiClient =
            new ApiClient();

    private TextView locationLabel;
    private FusedLocationProviderClient fusedLocationClient;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.home_page_layout
        );

        setupSystemBars();

        bindViews();

        updateGreeting();

        setupBottomNavigation();

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(
                                this
                        );

        if (hasLocationPermission()) {

            fetchCurrentLocationLabel();

        } else {

            requestLocationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
    }

    private void updateGreeting() {
        TextView greetingLabel = findViewById(R.id.greetingLabel);
        if (greetingLabel != null) {
            SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
            String fullName = prefs.getString("fullName", "Student");
            String firstName = fullName.trim().split("\\s+")[0];
            greetingLabel.setText("Kumusta, " + firstName);
        }
        loadStudentRentStatus();
    }

    private void loadStudentRentStatus() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return;

        TextView rentAmountView = findViewById(R.id.textRentAmount);
        if (rentAmountView == null) return;

        apiClient.getBookings(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            int activeRoomId = 0;
                            double activeRent = 0;
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject b = data.getJSONObject(i);
                                int bUserId = b.optInt("user_id", 0);
                                String status = b.optString("status", "").trim().toUpperCase();
                                if (bUserId == userId && ("APPROVED".equals(status) || "PENDING".equals(status) || "CONFIRMED".equals(status))) {
                                    activeRoomId = b.optInt("room_id", 0);
                                    double total = b.optDouble("total_amount", 0);
                                    if (total <= 0) {
                                        total = b.optDouble("monthly_rent", 0);
                                    }
                                    if (total > 0) {
                                        activeRent = total;
                                    }
                                    break;
                                }
                            }

                            if (activeRent > 0) {
                                final double rent = activeRent;
                                runOnUiThread(() -> rentAmountView.setText("₱" + String.format(Locale.getDefault(), "%.2f", rent)));
                            } else if (activeRoomId > 0) {
                                final int finalRoomId = activeRoomId;
                                apiClient.getRooms(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {}

                                    @Override
                                    public void onResponse(Call call, Response resp2) throws IOException {
                                        if (!resp2.isSuccessful() || resp2.body() == null) return;
                                        try {
                                            JSONObject rJson = new JSONObject(resp2.body().string());
                                            JSONArray rData = rJson.optJSONArray("data");
                                            if (rData != null) {
                                                for (int j = 0; j < rData.length(); j++) {
                                                    JSONObject r = rData.getJSONObject(j);
                                                    if (r.optInt("room_id", 0) == finalRoomId) {
                                                        double rRent = r.optDouble("monthly_rent", 0);
                                                        if (rRent > 0) {
                                                            runOnUiThread(() -> rentAmountView.setText("₱" + String.format(Locale.getDefault(), "%.2f", rRent)));
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    // =========================================================
    // SYSTEM BARS
    // =========================================================

    private void setupSystemBars() {

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

                    View nav =
                            findViewById(
                                    R.id.bottomNav
                            );

                    if (nav != null) {

                        nav.setPadding(
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
    // BIND VIEWS
    // =========================================================

    private void bindViews() {

        locationLabel =
                findViewById(
                        R.id.locationLabel
                );

        bottomNav =
                findViewById(
                        R.id.bottomNav
                );

        TextView wavingHand =
                findViewById(
                        R.id.wavingHand
                );

        if (wavingHand != null) {

            Animation wave =
                    AnimationUtils.loadAnimation(
                            this,
                            R.anim.wave
                    );

            wavingHand.startAnimation(
                    wave
            );
        }

        setupNotifications();

        setupSearch();

        setupListingCards();

        setupPaymentButton();

        setupHistory();

        setupChats();

        setupRequests();

        setupSeeAll();
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private void setupNotifications() {

        View btnNotifications =
                findViewById(
                        R.id.btnNotifications
                );

        if (btnNotifications == null) {
            return;
        }

        btnNotifications.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            NotificationsActivity.class
                    );

            startActivity(
                    intent
            );

            if (
                    Build.VERSION.SDK_INT
                            < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {

                overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            }
        });
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void setupSearch() {

        EditText homeSearchInput =
                findViewById(
                        R.id.homeSearchInput
                );

        ImageView btnSearchSubmit =
                findViewById(
                        R.id.btnSearchSubmit
                );

        if (homeSearchInput != null) {

            homeSearchInput.setOnEditorActionListener(
                    (v, actionId, event) -> {

                        if (
                                actionId
                                        == EditorInfo.IME_ACTION_SEARCH
                        ) {

                            performSearch(
                                    homeSearchInput
                                            .getText()
                                            .toString()
                            );

                            return true;
                        }

                        return false;
                    }
            );
        }

        if (btnSearchSubmit != null) {

            btnSearchSubmit.setOnClickListener(v -> {

                if (homeSearchInput != null) {

                    performSearch(
                            homeSearchInput
                                    .getText()
                                    .toString()
                    );
                }
            });
        }
    }

    // =========================================================
    // EXISTING HOME LISTING CARDS
    // =========================================================

    private void setupListingCards() {
        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray houses = json.optJSONArray("data");
                        if (houses != null && houses.length() > 0) {
                            runOnUiThread(() -> bindLiveListings(houses));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void bindLiveListings(JSONArray houses) {
        try {
            View nearestSection = findViewById(R.id.nearestSectionContainer);
            if (nearestSection != null) {
                nearestSection.setVisibility(houses.length() > 0 ? View.VISIBLE : View.GONE);
            }

            int[] cardIds = {R.id.cardListing1, R.id.cardListing2, R.id.cardListing3, R.id.cardListing4};

            for (int i = 0; i < cardIds.length; i++) {
                View card = findViewById(cardIds[i]);
                if (card != null) {
                    if (i < houses.length()) {
                        card.setVisibility(View.VISIBLE);
                        JSONObject h = houses.getJSONObject(i);
                        int hId = h.optInt("house_id", 1);
                        int landlordId = h.optInt("landlord_id", 0);
                        String hName = h.optString("house_name", "Boarding House");
                        String hDesc = h.optString("description", "");
                        String hAddress = h.optString("address", "Near Campus");
                        String hRules = h.optString("house_rules", "");
                        String hStatus = h.optString("status", "ACTIVE");

                        if (i == 0) {
                            TextView name1 = findViewById(R.id.textHouseName1);
                            TextView addr1 = findViewById(R.id.textHouseAddress1);
                            TextView rent1 = findViewById(R.id.textHouseRent1);
                            ImageView img1 = findViewById(R.id.imgHouse1);
                            if (name1 != null) name1.setText(hName);
                            if (addr1 != null) addr1.setText(hAddress);
                            if (rent1 != null) rent1.setText("Available");

                            String pPath = getFirstPhotoPath(h);
                            if (img1 != null && !pPath.isEmpty()) {
                                String imgUrl = "http://10.129.224.109/Dormigo_Backend/" + pPath;
                                Glide.with(this)
                                        .load(imgUrl)
                                        .placeholder(R.drawable.bg_image_placeholder)
                                        .into(img1);
                            }

                            TextView rating1 = findViewById(R.id.textHouseRating1);
                            apiClient.getReviewsForHouse(hId, new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {}

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if (!response.isSuccessful()) return;
                                    try {
                                        JSONObject rJson = new JSONObject(response.body().string());
                                        if (rJson.optBoolean("success", false)) {
                                            double avg = rJson.optDouble("average_rating", 0.0);
                                            int count = rJson.optInt("review_count", 0);
                                            runOnUiThread(() -> {
                                                if (rating1 != null) {
                                                    if (count > 0) {
                                                        rating1.setText("★ " + String.format(Locale.US, "%.1f", avg));
                                                    } else {
                                                        rating1.setText("No ratings yet");
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception ignored) {}
                                }
                            });
                        } else if (i == 1) {
                            TextView name2 = findViewById(R.id.textHouseName2);
                            TextView addr2 = findViewById(R.id.textHouseAddress2);
                            TextView rent2 = findViewById(R.id.textHouseRent2);
                            ImageView img2 = findViewById(R.id.imgHouse2);
                            if (name2 != null) name2.setText(hName);
                            if (addr2 != null) addr2.setText(hAddress);
                            if (rent2 != null) rent2.setText("Available");

                            String pPath = getFirstPhotoPath(h);
                            if (img2 != null && !pPath.isEmpty()) {
                                String imgUrl = "http://10.129.224.109/Dormigo_Backend/" + pPath;
                                Glide.with(this)
                                        .load(imgUrl)
                                        .placeholder(R.drawable.bg_image_placeholder)
                                        .into(img2);
                            }

                            TextView rating2 = findViewById(R.id.textHouseRating2);
                            apiClient.getReviewsForHouse(hId, new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {}

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if (!response.isSuccessful()) return;
                                    try {
                                        JSONObject rJson = new JSONObject(response.body().string());
                                        if (rJson.optBoolean("success", false)) {
                                            double avg = rJson.optDouble("average_rating", 0.0);
                                            int count = rJson.optInt("review_count", 0);
                                            runOnUiThread(() -> {
                                                if (rating2 != null) {
                                                    if (count > 0) {
                                                        rating2.setText("★ " + String.format(Locale.US, "%.1f", avg));
                                                    } else {
                                                        rating2.setText("No ratings yet");
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception ignored) {}
                                }
                            });
                        }

                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(this, ViewBoardingHouseActivity.class);
                            intent.putExtra("HOUSE_ID", hId);
                            intent.putExtra("LANDLORD_ID", landlordId);
                            intent.putExtra("HOUSE_NAME", hName);
                            intent.putExtra("HOUSE_DESCRIPTION", hDesc);
                            intent.putExtra("HOUSE_ADDRESS", hAddress);
                            intent.putExtra("HOUSE_RULES", hRules);
                            intent.putExtra("HOUSE_STATUS", hStatus);
                            startActivity(intent);
                        });
                    } else {
                        card.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================
    // PAYMENT BUTTON
    // =========================================================

    private void setupPaymentButton() {

        View btnPayNow =
                findViewById(
                        R.id.btnPayNow
                );

        if (btnPayNow == null) {
            return;
        }

        btnPayNow.setOnClickListener(v ->
                openApprovedBookingPayment(
                        btnPayNow
                )
        );
    }

    // =========================================================
    // FIND APPROVED BOOKING
    // =========================================================

    private void openApprovedBookingPayment(
            View button
    ) {

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

            showToast(
                    "Please log in before making a payment."
            );

            return;
        }

        button.setEnabled(
                false
        );

        apiClient.getBookings(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            button.setEnabled(
                                    true
                            );

                            showToast(
                                    "Unable to load your booking."
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

                                    button.setEnabled(
                                            true
                                    );

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Unable to load bookings."
                                            )
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (
                                        data == null
                                                || data.length() == 0
                                ) {

                                    button.setEnabled(
                                            true
                                    );

                                    showToast(
                                            "You do not have an approved booking to pay."
                                    );

                                    return;
                                }

                                JSONObject approvedBooking =
                                        null;

                                int highestBookingId =
                                        0;

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

                                    String status =
                                            booking.optString(
                                                            "status",
                                                            ""
                                                    ).trim()
                                                    .toUpperCase(
                                                            Locale.ROOT
                                                    );

                                    int currentBookingId =
                                            booking.optInt(
                                                    "booking_id",
                                                    0
                                            );

                                    if (
                                            bookingUserId == userId
                                                    && "APPROVED".equals(
                                                    status
                                            )
                                                    && currentBookingId
                                                    > highestBookingId
                                    ) {

                                        approvedBooking =
                                                booking;

                                        highestBookingId =
                                                currentBookingId;
                                    }
                                }

                                if (
                                        approvedBooking == null
                                ) {

                                    button.setEnabled(
                                            true
                                    );

                                    showToast(
                                            "You do not have an approved booking to pay."
                                    );

                                    return;
                                }

                                int bookingId =
                                        approvedBooking.optInt(
                                                "booking_id",
                                                0
                                        );

                                int roomId =
                                        approvedBooking.optInt(
                                                "room_id",
                                                0
                                        );

                                if (
                                        bookingId <= 0
                                                || roomId <= 0
                                ) {

                                    button.setEnabled(
                                            true
                                    );

                                    showToast(
                                            "Invalid booking information."
                                    );

                                    return;
                                }

                                loadRoomForPayment(
                                        bookingId,
                                        roomId,
                                        button
                                );

                            } catch (Exception e) {

                                button.setEnabled(
                                        true
                                );

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
    // LOAD ROOM
    // =========================================================

    private void loadRoomForPayment(
            int bookingId,
            int roomId,
            View button
    ) {

        apiClient.getRooms(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            button.setEnabled(
                                    true
                            );

                            openPaymentActivity(
                                    bookingId,
                                    "Boarding House",
                                    "Room"
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

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            int houseId = 0;

                            String roomName =
                                    "Room";

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

                                    if (
                                            room.optInt(
                                                    "room_id",
                                                    0
                                            ) == roomId
                                    ) {

                                        houseId =
                                                room.optInt(
                                                        "house_id",
                                                        0
                                                );

                                        String roomNumber =
                                                room.optString(
                                                        "room_number",
                                                        ""
                                                ).trim();

                                        String roomType =
                                                room.optString(
                                                        "room_type",
                                                        ""
                                                ).trim();

                                        if (!roomNumber.isEmpty()) {

                                            roomName =
                                                    roomNumber;

                                        } else if (
                                                !roomType.isEmpty()
                                        ) {

                                            roomName =
                                                    roomType;
                                        }

                                        break;
                                    }
                                }
                            }

                            int finalHouseId =
                                    houseId;

                            String finalRoomName =
                                    roomName;

                            runOnUiThread(() -> {

                                if (finalHouseId <= 0) {

                                    button.setEnabled(
                                            true
                                    );

                                    openPaymentActivity(
                                            bookingId,
                                            "Boarding House",
                                            finalRoomName
                                    );

                                    return;
                                }

                                loadHouseForPayment(
                                        bookingId,
                                        finalHouseId,
                                        finalRoomName,
                                        button
                                );
                            });

                        } catch (Exception e) {

                            runOnUiThread(() -> {

                                button.setEnabled(
                                        true
                                );

                                openPaymentActivity(
                                        bookingId,
                                        "Boarding House",
                                        "Room"
                                );
                            });
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD BOARDING HOUSE
    // =========================================================

    private void loadHouseForPayment(
            int bookingId,
            int houseId,
            String roomName,
            View button
    ) {

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            button.setEnabled(
                                    true
                            );

                            openPaymentActivity(
                                    bookingId,
                                    "Boarding House",
                                    roomName
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

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            String houseName =
                                    "Boarding House";

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

                                    if (
                                            house.optInt(
                                                    "house_id",
                                                    0
                                            ) == houseId
                                    ) {

                                        houseName =
                                                house.optString(
                                                        "house_name",
                                                        "Boarding House"
                                                );

                                        break;
                                    }
                                }
                            }

                            String finalHouseName =
                                    houseName;

                            runOnUiThread(() -> {

                                button.setEnabled(
                                        true
                                );

                                openPaymentActivity(
                                        bookingId,
                                        finalHouseName,
                                        roomName
                                );
                            });

                        } catch (Exception e) {

                            runOnUiThread(() -> {

                                button.setEnabled(
                                        true
                                );

                                openPaymentActivity(
                                        bookingId,
                                        "Boarding House",
                                        roomName
                                );
                            });
                        }
                    }
                }
        );
    }

    // =========================================================
    // OPEN PAYMENT SCREEN
    // =========================================================

    private void openPaymentActivity(
            int bookingId,
            String houseName,
            String roomName
    ) {

        Intent intent =
                new Intent(
                        HomeActivity.this,
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

        if (
                Build.VERSION.SDK_INT
                        < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {

            overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
            );
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void setupHistory() {

        View btnViewHistory =
                findViewById(
                        R.id.btnViewHistory
                );

        View quickHistory =
                findViewById(
                        R.id.quickHistory
                );

        View.OnClickListener historyListener =
                v -> {

                    Intent intent =
                            new Intent(
                                    HomeActivity.this,
                                    TransactionHistoryActivity.class
                            );

                    startActivity(
                            intent
                    );

                    if (
                            Build.VERSION.SDK_INT
                                    < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ) {

                        overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                        );
                    }
                };

        if (btnViewHistory != null) {

            btnViewHistory.setOnClickListener(
                    historyListener
            );
        }

        if (quickHistory != null) {

            quickHistory.setOnClickListener(
                    historyListener
            );
        }
    }

    // =========================================================
    // CHATS
    // =========================================================

    private void setupChats() {

        View quickChats =
                findViewById(
                        R.id.quickChats
                );

        if (quickChats == null) {
            return;
        }

        quickChats.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            ChatHistoryActivity.class
                    );

            startActivity(
                    intent
            );

            if (
                    Build.VERSION.SDK_INT
                            < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {

                overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            }
        });
    }

    // =========================================================
    // REQUESTS
    // =========================================================

    private void setupRequests() {

        View quickRequests =
                findViewById(
                        R.id.quickRequests
                );

        if (quickRequests == null) {
            return;
        }

        quickRequests.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            BookingRequestsActivity.class
                    );

            startActivity(
                    intent
            );

            if (
                    Build.VERSION.SDK_INT
                            < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {

                overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            }
        });
    }

    // =========================================================
    // SEE ALL
    // =========================================================

    private void setupSeeAll() {

        View btnSeeAll =
                findViewById(
                        R.id.btnSeeAll
                );

        if (btnSeeAll == null) {
            return;
        }

        btnSeeAll.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            BoardingHouseListingsActivity.class
                    );

            startActivity(
                    intent
            );

            overridePendingTransition(
                    R.anim.fade_in,
                    R.anim.fade_out
            );
        });
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(
                R.id.nav_home
        );

        bottomNav.setOnItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();

                    if (
                            id == R.id.nav_home
                    ) {

                        return true;

                    } else if (
                            id == R.id.nav_explore
                    ) {

                        Intent intent =
                                new Intent(
                                        HomeActivity.this,
                                        BoardingHouseListingsActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        if (
                                Build.VERSION.SDK_INT
                                        < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {

                            overridePendingTransition(
                                    R.anim.fade_in,
                                    R.anim.fade_out
                            );
                        }

                        return true;

                    } else if (
                            id == R.id.nav_chats
                    ) {

                        Intent intent =
                                new Intent(
                                        HomeActivity.this,
                                        ChatHistoryActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        if (
                                Build.VERSION.SDK_INT
                                        < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {

                            overridePendingTransition(
                                    R.anim.fade_in,
                                    R.anim.fade_out
                            );
                        }

                        return true;

                    } else if (
                            id == R.id.nav_requests
                    ) {

                        Intent intent =
                                new Intent(
                                        HomeActivity.this,
                                        BookingRequestsActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        if (
                                Build.VERSION.SDK_INT
                                        < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {

                            overridePendingTransition(
                                    R.anim.fade_in,
                                    R.anim.fade_out
                            );
                        }

                        return true;

                    } else if (
                            id == R.id.nav_profile
                    ) {

                        Intent intent =
                                new Intent(
                                        HomeActivity.this,
                                        ProfileActivity.class
                                );

                        startActivity(
                                intent
                        );

                        finish();

                        overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                        );

                        return true;
                    }

                    return false;
                }
        );
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

                ||

                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @SuppressWarnings("MissingPermission")
    private void fetchCurrentLocationLabel() {

        if (locationLabel == null) {
            return;
        }

        locationLabel.setText(
                R.string.getting_location
        );

        CancellationTokenSource cts =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.getToken()
                )
                .addOnSuccessListener(
                        this,
                        location -> {

                            if (location != null) {

                                updateLocationLabel(
                                        location
                                );

                            } else {

                                locationLabel.setText(
                                        R.string.location_unavailable
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        this,
                        e -> locationLabel.setText(
                                R.string.location_unavailable
                        )
                );
    }

    private void updateLocationLabel(
            Location location
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            if (
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.TIRAMISU
            ) {

                geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1,
                        addresses -> {

                            if (!addresses.isEmpty()) {

                                Address address =
                                        addresses.get(
                                                0
                                        );

                                runOnUiThread(() ->
                                        locationLabel.setText(
                                                buildNearestPlaceLabel(
                                                        address
                                                )
                                        )
                                );

                            } else {

                                runOnUiThread(() ->
                                        locationLabel.setText(
                                                R.string.location_unavailable
                                        )
                                );
                            }
                        }
                );

            } else {

                List<Address> addresses =
                        geocoder.getFromLocation(
                                location.getLatitude(),
                                location.getLongitude(),
                                1
                        );

                if (
                        addresses != null
                                && !addresses.isEmpty()
                ) {

                    Address address =
                            addresses.get(
                                    0
                            );

                    locationLabel.setText(
                            buildNearestPlaceLabel(
                                    address
                            )
                    );

                } else {

                    locationLabel.setText(
                            R.string.location_unavailable
                    );
                }
            }

        } catch (Exception e) {

            locationLabel.setText(
                    R.string.location_unavailable
            );
        }
    }

    private String buildNearestPlaceLabel(
            Address address
    ) {

        String subLocality =
                address.getSubLocality();

        String locality =
                address.getLocality();

        StringBuilder sb =
                new StringBuilder();

        if (
                subLocality != null
                        && !subLocality.isEmpty()
        ) {

            sb.append(
                    subLocality
            );
        }

        if (
                locality != null
                        && !locality.isEmpty()
        ) {

            if (sb.length() > 0) {

                sb.append(
                        ", "
                );
            }

            sb.append(
                    locality
            );
        }

        String place =
                sb.length() > 0
                        ? sb.toString()
                        : null;

        return getString(
                R.string.near_location,
                Objects.requireNonNullElseGet(
                        place,
                        () -> getString(
                                R.string.your_location
                        )
                )
        );
    }

    // =========================================================
    // LOCATION PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (
                requestCode
                        == LOCATION_PERMISSION_REQUEST_CODE
        ) {

            if (
                    grantResults.length > 0
                            && grantResults[0]
                            == PackageManager.PERMISSION_GRANTED
            ) {

                fetchCurrentLocationLabel();

            } else if (
                    locationLabel != null
            ) {

                locationLabel.setText(
                        R.string.location_permission_needed
                );
            }
        }
    }

    // =========================================================
    // SEARCH NAVIGATION
    // =========================================================

    @SuppressWarnings("deprecation")
    private void performSearch(
            String query
    ) {

        Intent intent =
                new Intent(
                        HomeActivity.this,
                        BoardingHouseListingsActivity.class
                );

        intent.putExtra(
                "SEARCH_QUERY",
                query
        );

        startActivity(
                intent
        );

        if (
                Build.VERSION.SDK_INT
                        < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {

            overridePendingTransition(
                    R.anim.fade_in,
                    R.anim.fade_out
            );
        }
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

    private String getFirstPhotoPath(JSONObject h) {
        try {
            Object pObj = h.opt("photo_paths");
            if (pObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) pObj;
                if (arr.length() > 0) return arr.optString(0, "");
            } else if (pObj instanceof String) {
                String str = (String) pObj;
                str = str.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim();
                String[] parts = str.split(",");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    return parts[0].trim();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}