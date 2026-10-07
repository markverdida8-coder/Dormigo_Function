package com.dormigo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordTenantsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();

    private int houseId;
    private int landlordId;
    private String propertyName = "";
    private final Set<Integer> moveInRequestsInProgress = new HashSet<>();

    private TextView tenantsSubheader;
    private TextView tenantsStatus;
    private LinearLayout tenantsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_tenants);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        bindViews();
        readIntentData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTenants();
    }

    private void bindViews() {
        tenantsSubheader = findViewById(R.id.tenantsSubheader);
        tenantsStatus = findViewById(R.id.tenantsStatus);
        tenantsContainer = findViewById(R.id.tenantsContainer);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            houseId = intent.getIntExtra("HOUSE_ID", 0);
            propertyName = intent.getStringExtra("PROPERTY_NAME") != null ? intent.getStringExtra("PROPERTY_NAME") : "";
        }

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        landlordId = prefs.getInt("userId", 0);

        if (!propertyName.isEmpty() && tenantsSubheader != null) {
            tenantsSubheader.setText(propertyName + " — Active Tenants");
        }
    }

    // =========================================================
    // LOAD ACTIVE TENANTS
    // =========================================================

    private void loadTenants() {
        if (tenantsStatus != null) {
            tenantsStatus.setVisibility(View.VISIBLE);
            tenantsStatus.setText("Loading tenants...");
        }

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (tenantsStatus != null) {
                        tenantsStatus.setVisibility(View.VISIBLE);
                        tenantsStatus.setText("Unable to load tenants. Tap to retry.");
                        tenantsStatus.setOnClickListener(v -> loadTenants());
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray list = json.optJSONArray("data");
                        if (list == null) list = new JSONArray();

                        renderTenants(list);
                    } catch (Exception e) {
                        if (tenantsStatus != null) {
                            tenantsStatus.setVisibility(View.VISIBLE);
                            tenantsStatus.setText("Failed to process tenant records.");
                        }
                    }
                });
            }
        };

        if (houseId > 0) {
            apiClient.getBookingsForHouse(houseId, callback);
        } else if (landlordId > 0) {
            apiClient.getBookingsForLandlord(landlordId, callback);
        } else {
            apiClient.getBookings(callback);
        }
    }

    private void renderTenants(JSONArray bookings) {
        if (tenantsContainer == null) return;
        tenantsContainer.removeAllViews();

        int activeCount = 0;

        for (int i = 0; i < bookings.length(); i++) {
            JSONObject booking = bookings.optJSONObject(i);
            if (booking == null) continue;

            String status = booking.optString("status", "").toUpperCase(Locale.ROOT);
            if (!"APPROVED".equals(status) && !"ACTIVE".equals(status)) {
                continue;
            }

            activeCount++;
            View card = createTenantCard(booking);
            tenantsContainer.addView(card);
        }

        if (tenantsSubheader != null) {
            String title = propertyName.isEmpty() ? "All Properties" : propertyName;
            tenantsSubheader.setText(title + " — Active Tenants (" + activeCount + ")");
        }

        if (tenantsStatus != null) {
            if (activeCount == 0) {
                tenantsStatus.setVisibility(View.VISIBLE);
                tenantsStatus.setText("No active or approved tenants found.");
            } else {
                tenantsStatus.setVisibility(View.GONE);
            }
        }
    }

    private View createTenantCard(JSONObject booking) {
        int bookingId = booking.optInt("booking_id", 0);
        String studentName = booking.optString("full_name", "Student Tenant");
        String roomNumber = booking.optString("room_number", "Room");
        String roomType = booking.optString("room_type", "Standard");
        double monthlyRent = booking.optDouble("agreed_monthly_rent", 0.0);
        String moveInDate = booking.optString("move_in_date", "");
        String status = booking.optString("status", "ACTIVE").toUpperCase(Locale.ROOT);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_rounded);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(12);
        card.setLayoutParams(cardParams);

        // Top Row: Avatar Initials + Name/Room + Status Badge
        RelativeLayout topRow = new RelativeLayout(this);
        topRow.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Initials circle
        TextView avatar = new TextView(this);
        avatar.setId(View.generateViewId());
        avatar.setText(getInitials(studentName));
        avatar.setTextColor(Color.parseColor("#1B5E4C"));
        avatar.setTextSize(14);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_circle_green_light);
        RelativeLayout.LayoutParams avatarParams = new RelativeLayout.LayoutParams(dp(42), dp(42));
        avatar.setLayoutParams(avatarParams);
        topRow.addView(avatar);

        // Name & Room Column
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams nameParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nameParams.addRule(RelativeLayout.RIGHT_OF, avatar.getId());
        nameParams.leftMargin = dp(12);
        nameCol.setLayoutParams(nameParams);

        TextView nameText = new TextView(this);
        nameText.setText(studentName);
        nameText.setTextColor(Color.parseColor("#1A1A1A"));
        nameText.setTextSize(15);
        nameText.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameText);

        TextView roomText = new TextView(this);
        roomText.setText(roomNumber + " · " + roomType);
        roomText.setTextColor(Color.parseColor("#9A9A9E"));
        roomText.setTextSize(12);
        roomText.setPadding(0, dp(2), 0, 0);
        nameCol.addView(roomText);

        topRow.addView(nameCol);

        // Status Badge
        TextView badge = new TextView(this);
        badge.setText(status.equals("APPROVED") ? "Approved" : "Active");
        badge.setTextSize(11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackgroundResource(R.drawable.bg_badge_yellow);
        badge.setTextColor(Color.parseColor("#1B5E4C"));

        RelativeLayout.LayoutParams badgeParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        badge.setLayoutParams(badgeParams);
        topRow.addView(badge);

        card.addView(topRow);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#EFEFEF"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        divParams.topMargin = dp(12);
        divParams.bottomMargin = dp(12);
        card.addView(divider, divParams);

        // Bottom Row: Rent + End Tenancy Button + Chat Button
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout rentCol = new LinearLayout(this);
        rentCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rentColParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        rentCol.setLayoutParams(rentColParams);

        TextView rentText = new TextView(this);
        rentText.setText(String.format(Locale.getDefault(), "₱%,.2f/mo", monthlyRent));
        rentText.setTextColor(Color.parseColor("#1B5E4C"));
        rentText.setTextSize(14);
        rentText.setTypeface(null, Typeface.BOLD);
        rentCol.addView(rentText);

        if (!moveInDate.isEmpty()) {
            TextView dateText = new TextView(this);
            dateText.setText("Since " + moveInDate);
            dateText.setTextColor(Color.parseColor("#9A9A9E"));
            dateText.setTextSize(11);
            rentCol.addView(dateText);
        }

        bottomRow.addView(rentCol);

        if ("ACTIVE".equals(status)) {
            TextView btnEndTenancy = new TextView(this);
            btnEndTenancy.setText("End Tenancy");
            btnEndTenancy.setTextColor(Color.parseColor("#FF3B30"));
            btnEndTenancy.setTextSize(12);
            btnEndTenancy.setTypeface(null, Typeface.BOLD);
            btnEndTenancy.setBackgroundResource(R.drawable.bg_button_outline_red);
            btnEndTenancy.setPadding(dp(12), dp(6), dp(12), dp(6));
            btnEndTenancy.setClickable(true);
            btnEndTenancy.setFocusable(true);
            btnEndTenancy.setOnClickListener(v -> showEndTenancyDialog(bookingId, studentName, roomNumber));
            bottomRow.addView(btnEndTenancy);
        } else if ("APPROVED".equals(status) && bookingId > 0) {
            TextView btnConfirmMoveIn = new TextView(this);
            boolean requestInProgress = moveInRequestsInProgress.contains(bookingId);
            btnConfirmMoveIn.setText(requestInProgress ? "Confirming..." : "Confirm Move-in");
            btnConfirmMoveIn.setTextColor(Color.WHITE);
            btnConfirmMoveIn.setTextSize(12);
            btnConfirmMoveIn.setTypeface(null, Typeface.BOLD);
            btnConfirmMoveIn.setBackgroundResource(R.drawable.bg_button_filled);
            btnConfirmMoveIn.setPadding(dp(12), dp(6), dp(12), dp(6));
            btnConfirmMoveIn.setClickable(!requestInProgress);
            btnConfirmMoveIn.setFocusable(true);
            btnConfirmMoveIn.setEnabled(!requestInProgress);
            btnConfirmMoveIn.setOnClickListener(v ->
                    showConfirmMoveInDialog(bookingId, studentName, btnConfirmMoveIn)
            );
            bottomRow.addView(btnConfirmMoveIn);
        }

        // Chat Button
        TextView btnChat = new TextView(this);
        btnChat.setText("Chat");
        btnChat.setTextColor(Color.WHITE);
        btnChat.setTextSize(12);
        btnChat.setTypeface(null, Typeface.BOLD);
        btnChat.setBackgroundResource(R.drawable.bg_button_filled);
        btnChat.setPadding(dp(16), dp(6), dp(16), dp(6));
        btnChat.setClickable(true);
        btnChat.setFocusable(true);
        LinearLayout.LayoutParams chatParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chatParams.leftMargin = dp(8);
        btnChat.setLayoutParams(chatParams);
        btnChat.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, LandlordChatActivity.class);
            chatIntent.putExtra("STUDENT_NAME", studentName);
            startActivity(chatIntent);
        });
        bottomRow.addView(btnChat);

        card.addView(bottomRow);

        return card;
    }

    private void showConfirmMoveInDialog(int bookingId, String studentName, TextView button) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Move-in")
                .setMessage("Confirm that " + studentName + " has moved in? This will mark the student as an active tenant.")
                .setPositiveButton("Confirm Move-in", (dialog, which) ->
                        confirmMoveIn(bookingId, studentName, button)
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmMoveIn(int bookingId, String studentName, TextView button) {
        if (bookingId <= 0 || !moveInRequestsInProgress.add(bookingId)) {
            return;
        }

        button.setEnabled(false);
        button.setClickable(false);
        button.setText("Confirming...");

        apiClient.confirmMoveIn(bookingId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    moveInRequestsInProgress.remove(bookingId);
                    button.setEnabled(true);
                    button.setClickable(true);
                    button.setText("Confirm Move-in");
                    Toast.makeText(
                            LandlordTenantsActivity.this,
                            "Unable to confirm move-in. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                boolean success = false;
                String message = "Unable to confirm move-in. Please try again.";
                try {
                    JSONObject result = new JSONObject(body);
                    success = response.isSuccessful() && result.optBoolean("success", false);
                    if (!success) {
                        message = result.optString("message", message);
                        if (message.trim().isEmpty()) {
                            message = "Unable to confirm move-in. Please try again.";
                        }
                    }
                } catch (Exception ignored) {
                    if (response.isSuccessful()) {
                        message = "The server returned an invalid response. Please refresh and check the booking status.";
                    }
                }

                final boolean moveInConfirmed = success;
                final String resultMessage = message;
                runOnUiThread(() -> {
                    moveInRequestsInProgress.remove(bookingId);
                    if (moveInConfirmed) {
                        Toast.makeText(
                                LandlordTenantsActivity.this,
                                "Move-in confirmed for " + studentName + ".",
                                Toast.LENGTH_LONG
                        ).show();
                        loadTenants();
                    } else {
                        button.setEnabled(true);
                        button.setClickable(true);
                        button.setText("Confirm Move-in");
                        Toast.makeText(
                                LandlordTenantsActivity.this,
                                resultMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    private void showEndTenancyDialog(int bookingId, String studentName, String roomNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("End Tenancy");
        builder.setMessage("Are you sure you want to end tenancy for " + studentName + " in " + roomNumber + "?\n\nThe lease will be marked completed and room availability will be recalculated.");

        builder.setPositiveButton("End Tenancy", (dialog, which) -> {
            apiClient.updateBookingStatus(bookingId, "COMPLETED", new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LandlordTenantsActivity.this, "Failed to end tenancy.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    boolean completed = false;
                    try {
                        completed = response.isSuccessful()
                                && new JSONObject(body).optBoolean("success", false);
                    } catch (Exception ignored) {
                    }

                    if (completed) {
                        runOnUiThread(() -> {
                            Toast.makeText(LandlordTenantsActivity.this, "Tenancy ended. Room availability was recalculated.", Toast.LENGTH_LONG).show();
                            loadTenants();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(LandlordTenantsActivity.this, "Unable to end tenancy.", Toast.LENGTH_LONG).show());
                    }
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "T";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
