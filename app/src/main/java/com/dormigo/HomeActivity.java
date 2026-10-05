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
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isRentStatusLoading = false;
    private boolean isNotificationsLoading = false;

    private String cachedRentText = "";
    private String cachedHouseRoomText = "";
    private String cachedDueDateStatusText = "";
    private String cachedPayButtonText = "";

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadStudentRentStatus();
            loadNotificationsBadge();
            pollHandler.removeCallbacks(this);
            pollHandler.postDelayed(this, 20000);
        }
    };

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
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 20000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
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
        loadNotificationsBadge();
    }

    private void loadNotificationsBadge() {
        if (isNotificationsLoading) return;
        isNotificationsLoading = true;

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) {
            isNotificationsLoading = false;
            return;
        }

        apiClient.getNotifications(userId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                isNotificationsLoading = false;
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isNotificationsLoading = false;
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        int unreadCount = json.optInt("unread_count", 0);
                        JSONArray data = json.optJSONArray("data");

                        boolean hasUnreadBooking = false;
                        boolean hasUnreadChat = false;
                        boolean hasUnreadPayment = false;

                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject notif = data.optJSONObject(i);
                                if (notif != null && !notif.optBoolean("is_read", false)) {
                                    String type = notif.optString("type", "");
                                    if ("BOOKING".equals(type)) hasUnreadBooking = true;
                                    if ("CHAT".equals(type)) hasUnreadChat = true;
                                    if ("PAYMENT".equals(type)) hasUnreadPayment = true;
                                }
                            }
                        }

                        final boolean finalHasUnreadBooking = hasUnreadBooking;
                        final boolean finalHasUnreadChat = hasUnreadChat;
                        final boolean finalHasUnreadPayment = hasUnreadPayment;

                        runOnUiThread(() -> {
                            TextView badge = findViewById(R.id.notificationBadge);
                            if (badge != null) {
                                if (unreadCount > 0) {
                                    String badgeText = unreadCount > 9 ? "9+" : String.valueOf(unreadCount);
                                    if (!badgeText.equals(badge.getText().toString())) {
                                        badge.setText(badgeText);
                                    }
                                    if (badge.getVisibility() != View.VISIBLE) {
                                        badge.setVisibility(View.VISIBLE);
                                    }
                                } else {
                                    if (badge.getVisibility() != View.GONE) {
                                        badge.setVisibility(View.GONE);
                                    }
                                }
                            }

                            View badgeRequests = findViewById(R.id.badgeRequests);
                            if (badgeRequests != null) {
                                int vis = finalHasUnreadBooking ? View.VISIBLE : View.GONE;
                                if (badgeRequests.getVisibility() != vis) {
                                    badgeRequests.setVisibility(vis);
                                }
                            }

                            View badgeChats = findViewById(R.id.badgeChats);
                            if (badgeChats != null) {
                                int vis = finalHasUnreadChat ? View.VISIBLE : View.GONE;
                                if (badgeChats.getVisibility() != vis) {
                                    badgeChats.setVisibility(vis);
                                }
                            }

                            View badgeHistory = findViewById(R.id.badgeHistory);
                            if (badgeHistory != null) {
                                int vis = finalHasUnreadPayment ? View.VISIBLE : View.GONE;
                                if (badgeHistory.getVisibility() != vis) {
                                    badgeHistory.setVisibility(vis);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void loadStudentRentStatus() {
        if (isRentStatusLoading) return;
        isRentStatusLoading = true;

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) {
            isRentStatusLoading = false;
            return;
        }

        TextView rentAmountView = findViewById(R.id.textRentAmount);
        TextView houseRoomLabel = findViewById(R.id.textHouseRoomLabel);
        TextView dueDateStatus = findViewById(R.id.textDueDateAndStatus);
        TextView btnPayNow = findViewById(R.id.btnPayNow);

        apiClient.getBookings(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                isRentStatusLoading = false;
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isRentStatusLoading = false;
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            JSONObject targetBooking = null;
                            String targetStatus = "";
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject b = data.getJSONObject(i);
                                int bUserId = b.optInt("user_id", 0);
                                String status = b.optString("status", "").trim().toUpperCase(Locale.ROOT);
                                if (bUserId == userId && ("ACTIVE".equals(status) || "APPROVED".equals(status))) {
                                    targetBooking = b;
                                    targetStatus = status;
                                    if ("ACTIVE".equals(status)) {
                                        break;
                                    }
                                }
                            }

                            if (targetBooking != null) {
                                double monthlyRent = targetBooking.optDouble("agreed_monthly_rent", 0);
                                String houseName = targetBooking.optString("house_name", "Boarding House");
                                String roomNumber = targetBooking.optString("room_number", "Room");
                                int advanceMonths = targetBooking.optInt("advance_months", 1);
                                int depositMonths = targetBooking.optInt("deposit_months", 1);
                                double utilityDeposit = targetBooking.optDouble("utility_deposit", 0);
                                double otherFees = targetBooking.optDouble("other_fees", 0);
                                int paymentDueDay = targetBooking.optInt("payment_due_day", 1);
                                String moveInDate = targetBooking.optString("move_in_date", "");
                                boolean initialCompleted = targetBooking.optBoolean("initial_payment_completed", false);
                                String dbNextDueDate = targetBooking.optString("next_due_date", "");

                                double monthlyRecurringFees = targetBooking.optDouble("monthly_recurring_fees", 0);
                                double utilitiesFixed = targetBooking.optDouble("utilities_fixed", 0);

                                double displayAmount = monthlyRent;
                                if ("APPROVED".equals(targetStatus) && !initialCompleted) {
                                    displayAmount = monthlyRent + (monthlyRent * advanceMonths) + (monthlyRent * depositMonths) + utilityDeposit + otherFees;
                                } else if (initialCompleted) {
                                    displayAmount = monthlyRent + monthlyRecurringFees + utilitiesFixed;
                                }

                                String nextDue;
                                if (initialCompleted && dbNextDueDate != null && !dbNextDueDate.isEmpty() && !"null".equals(dbNextDueDate)) {
                                    try {
                                        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                        SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                                        nextDue = sdf2.format(sdf1.parse(dbNextDueDate));
                                    } catch (Exception e) {
                                        nextDue = dbNextDueDate;
                                    }
                                } else {
                                    nextDue = calculateNextFutureDueDate(moveInDate, paymentDueDay);
                                }

                                final String newRentText = "₱" + String.format(Locale.getDefault(), "%.2f", displayAmount);
                                final String newHouseRoomText = houseName + " · Room " + roomNumber;
                                final String newPayBtnText = "APPROVED".equals(targetStatus) ? "Pay Initial Payment" : "Pay Monthly Rent";
                                String dueStatus = calculateDueStatus(nextDue);
                                final String newDueStatusText = "Next Due Date: " + nextDue + " (" + dueStatus + ")";

                                runOnUiThread(() -> {
                                    if (rentAmountView != null && !newRentText.equals(cachedRentText)) {
                                        rentAmountView.setText(newRentText);
                                        cachedRentText = newRentText;
                                    }
                                    if (houseRoomLabel != null && !newHouseRoomText.equals(cachedHouseRoomText)) {
                                        houseRoomLabel.setText(newHouseRoomText);
                                        cachedHouseRoomText = newHouseRoomText;
                                    }
                                    if (dueDateStatus != null && !newDueStatusText.equals(cachedDueDateStatusText)) {
                                        dueDateStatus.setText(newDueStatusText);
                                        cachedDueDateStatusText = newDueStatusText;
                                    }
                                    if (btnPayNow != null && !newPayBtnText.equals(cachedPayButtonText)) {
                                        btnPayNow.setText(newPayBtnText);
                                        cachedPayButtonText = newPayBtnText;
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public static String calculateNextFutureDueDate(String moveInDateStr, int dueDay) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar moveInCal = Calendar.getInstance();
            if (moveInDateStr != null && !moveInDateStr.isEmpty() && !moveInDateStr.equals("Not specified")) {
                moveInCal.setTime(sdf.parse(moveInDateStr));
            }
            Calendar now = Calendar.getInstance();
            Calendar target = (Calendar) now.clone();
            target.set(Calendar.DAY_OF_MONTH, Math.min(dueDay, 28));

            if (target.before(now) || target.equals(now)) {
                target.add(Calendar.MONTH, 1);
            }
            while (target.before(moveInCal)) {
                target.add(Calendar.MONTH, 1);
            }
            return new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(target.getTime());
        } catch (Exception e) {
            return "Every " + dueDay + getOrdinalSuffix(dueDay) + " of the month";
        }
    }

    public static String calculateDueStatus(String dueDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            Date dueDate = sdf.parse(dueDateStr);
            if (dueDate == null) return "Due soon";

            long diffMillis = dueDate.getTime() - System.currentTimeMillis();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            if (diffDays < 0) {
                long overdueDays = Math.abs(diffDays);
                return "Overdue by " + overdueDays + (overdueDays == 1 ? " day" : " days");
            } else if (diffDays == 0) {
                return "Due Today";
            } else if (diffDays == 1) {
                return "Due Tomorrow";
            } else {
                return "Due in " + diffDays + " days";
            }
        } catch (Exception e) {
            return "Due soon";
        }
    }

    private static String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
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
            
            TextView badge = findViewById(R.id.notificationBadge);
            if (badge != null) badge.setVisibility(View.GONE);

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
        apiClient.getAvailableBoardingHouses(new Callback() {
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
                                String imgUrl = "http://10.209.52.109/Dormigo_Backend/" + pPath;
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
                                String imgUrl = "http://10.209.52.109/Dormigo_Backend/" + pPath;
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
                        } else if (i == 2) {
                            TextView name3 = findViewById(R.id.textHouseName3);
                            TextView addr3 = findViewById(R.id.textHouseAddress3);
                            TextView rent3 = findViewById(R.id.textHouseRent3);
                            ImageView img3 = findViewById(R.id.imgHouse3);
                            if (name3 != null) name3.setText(hName);
                            if (addr3 != null) addr3.setText(hAddress);
                            if (rent3 != null) rent3.setText("Available");

                            String pPath = getFirstPhotoPath(h);
                            if (img3 != null && !pPath.isEmpty()) {
                                String imgUrl = "http://10.209.52.109/Dormigo_Backend/" + pPath;
                                Glide.with(this)
                                        .load(imgUrl)
                                        .placeholder(R.drawable.bg_image_placeholder)
                                        .into(img3);
                            }

                            TextView rating3 = findViewById(R.id.textHouseRating3);
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
                                                if (rating3 != null) {
                                                    if (count > 0) {
                                                        rating3.setText("★ " + String.format(Locale.US, "%.1f", avg));
                                                    } else {
                                                        rating3.setText("No ratings yet");
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception ignored) {}
                                }
                            });
                        } else if (i == 3) {
                            TextView name4 = findViewById(R.id.textHouseName4);
                            TextView addr4 = findViewById(R.id.textHouseAddress4);
                            TextView rent4 = findViewById(R.id.textHouseRent4);
                            ImageView img4 = findViewById(R.id.imgHouse4);
                            if (name4 != null) name4.setText(hName);
                            if (addr4 != null) addr4.setText(hAddress);
                            if (rent4 != null) rent4.setText("Available");

                            String pPath = getFirstPhotoPath(h);
                            if (img4 != null && !pPath.isEmpty()) {
                                String imgUrl = "http://10.209.52.109/Dormigo_Backend/" + pPath;
                                Glide.with(this)
                                        .load(imgUrl)
                                        .placeholder(R.drawable.bg_image_placeholder)
                                        .into(img4);
                            }

                            TextView rating4 = findViewById(R.id.textHouseRating4);
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
                                                if (rating4 != null) {
                                                    if (count > 0) {
                                                        rating4.setText("★ " + String.format(Locale.US, "%.1f", avg));
                                                    } else {
                                                        rating4.setText("No ratings yet");
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

                                JSONObject targetBooking =
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
                                                    && ("APPROVED".equals(
                                                    status
                                            ) || "ACTIVE".equals(status))
                                                    && currentBookingId
                                                    > highestBookingId
                                    ) {

                                        targetBooking =
                                                booking;

                                        highestBookingId =
                                                currentBookingId;

                                        if ("ACTIVE".equals(status)) {
                                            break;
                                        }
                                    }
                                }

                                if (
                                        targetBooking == null
                                ) {

                                    button.setEnabled(
                                            true
                                    );

                                    showToast(
                                            "You do not have an active or approved booking to pay."
                                    );

                                    return;
                                }

                                int bookingId =
                                        targetBooking.optInt(
                                                "booking_id",
                                                0
                                        );

                                int roomId =
                                        targetBooking.optInt(
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
                                        button,
                                        targetBooking.optString("status", "").toUpperCase().contains("APPROVED") ? "INITIAL" : "MONTHLY"
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
            View button,
            String paymentType
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
                                    "Room",
                                    paymentType
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
                                            finalRoomName,
                                            paymentType
                                    );

                                    return;
                                }

                                loadHouseForPayment(
                                        bookingId,
                                        finalHouseId,
                                        finalRoomName,
                                        button,
                                        paymentType
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
                                        "Room",
                                        paymentType
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
            View button,
            String paymentType
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
                                    roomName,
                                    paymentType
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
                                                ).trim();

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
                                        roomName,
                                        paymentType
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
                                        roomName,
                                        paymentType
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
            String roomName,
            String paymentType
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

        intent.putExtra(
                "payment_type",
                paymentType
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
                    SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
                    int userId = prefs.getInt("userId", -1);
                    if (userId > 0) {
                        apiClient.markNotificationsAsReadByType(userId, "PAYMENT", new Callback() {
                            @Override public void onFailure(Call call, IOException e) {}
                            @Override public void onResponse(Call call, Response response) {}
                        });
                        View badgeHistory = findViewById(R.id.badgeHistory);
                        if (badgeHistory != null) badgeHistory.setVisibility(View.GONE);
                    }

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
            SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
            int userId = prefs.getInt("userId", -1);
            if (userId > 0) {
                apiClient.markNotificationsAsReadByType(userId, "CHAT", new Callback() {
                    @Override public void onFailure(Call call, IOException e) {}
                    @Override public void onResponse(Call call, Response response) {}
                });
                View badgeChats = findViewById(R.id.badgeChats);
                if (badgeChats != null) badgeChats.setVisibility(View.GONE);
            }

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
            SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
            int userId = prefs.getInt("userId", -1);
            if (userId > 0) {
                apiClient.markNotificationsAsReadByType(userId, "BOOKING", new Callback() {
                    @Override public void onFailure(Call call, IOException e) {}
                    @Override public void onResponse(Call call, Response response) {}
                });
                View badgeRequests = findViewById(R.id.badgeRequests);
                if (badgeRequests != null) badgeRequests.setVisibility(View.GONE);
            }

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