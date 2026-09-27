package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NotificationsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private int userId;
    private final List<JSONObject> notificationsList = new ArrayList<>();
    private String currentFilter = "ALL";
    private LinearLayout notificationsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        notificationsContainer = findViewById(R.id.notifListLayout);

        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView markAllAsRead = findViewById(R.id.btnMarkAllRead);
        if (markAllAsRead != null) {
            markAllAsRead.setOnClickListener(v -> {
                if (userId > 0) {
                    apiClient.markAllNotificationsAsRead(userId, new Callback() {
                        @Override public void onFailure(Call call, IOException e) {}
                        @Override public void onResponse(Call call, Response response) throws IOException {
                            runOnUiThread(() -> loadNotifications());
                        }
                    });
                }
            });
        }

        setupFilters();
    }

    private void setupFilters() {
        TextView filterAll = findViewById(R.id.filterAll);
        TextView filterRequests = findViewById(R.id.filterRequests);
        TextView filterPayments = findViewById(R.id.filterPayments);
        TextView filterMessages = findViewById(R.id.filterMessages);

        TextView[] filters = {filterAll, filterRequests, filterPayments, filterMessages};

        View.OnClickListener filterListener = v -> {
            for (TextView filter : filters) {
                if (filter != null) {
                    filter.setBackgroundResource(R.drawable.bg_chip_selectable);
                    filter.setTextColor(0xFF6E6E73);
                    filter.setTypeface(null, Typeface.NORMAL);
                }
            }

            TextView selectedFilter = (TextView) v;
            selectedFilter.setBackgroundResource(R.drawable.bg_button_filled);
            selectedFilter.setTextColor(0xFFFFFFFF);
            selectedFilter.setTypeface(null, Typeface.BOLD);

            int id = v.getId();
            if (id == R.id.filterAll) {
                currentFilter = "ALL";
            } else if (id == R.id.filterRequests) {
                currentFilter = "BOOKING";
            } else if (id == R.id.filterPayments) {
                currentFilter = "PAYMENT";
            } else if (id == R.id.filterMessages) {
                currentFilter = "CHAT";
            }
            
            renderNotifications();
        };

        if (filterAll != null) filterAll.setOnClickListener(filterListener);
        if (filterRequests != null) filterRequests.setOnClickListener(filterListener);
        if (filterPayments != null) filterPayments.setOnClickListener(filterListener);
        if (filterMessages != null) filterMessages.setOnClickListener(filterListener);
    }

    private void loadNotifications() {
        if (userId <= 0) return;
        
        apiClient.getNotifications(userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        runOnUiThread(() -> {
                            notificationsList.clear();
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    notificationsList.add(data.optJSONObject(i));
                                }
                            }
                            renderNotifications();
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void renderNotifications() {
        // We will replace existing hardcoded mock elements if present
        if (notificationsContainer == null) return;
        
        // Remove children except filters/headers if needed. For now, assume a dedicated container or we will dynamically add.
        // Assuming we replace the whole ScrollView content if it's currently hardcoded.
        LinearLayout parent = findViewById(R.id.notifListLayout);
        if (parent != null) {
            parent.removeAllViews();
        } else {
            return;
        }

        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject notif : notificationsList) {
            String type = notif.optString("type", "");
            if ("ALL".equals(currentFilter) || type.equals(currentFilter)) {
                filtered.add(notif);
            }
        }

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No notifications found.");
            empty.setTextColor(0xFF9A9A9E);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            parent.addView(empty);
            return;
        }

        for (JSONObject notif : filtered) {
            View card = createNotificationCard(notif);
            parent.addView(card);
        }
    }

    private View createNotificationCard(JSONObject notif) {
        int notifId = notif.optInt("notification_id", 0);
        String title = notif.optString("title", "");
        String message = notif.optString("message", "");
        String type = notif.optString("type", "");
        boolean isRead = notif.optBoolean("is_read", false);
        String dateStr = notif.optString("created_at", "");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(0, dp(16), 0, dp(16));
        card.setBackgroundResource(isRead ? 0 : R.drawable.bg_unread_notif);

        // Icon
        ImageView icon = new ImageView(this);
        int iconRes = R.drawable.ic_bell;
        if ("BOOKING".equals(type)) iconRes = R.drawable.ic_receipt;
        if ("PAYMENT".equals(type)) iconRes = R.drawable.ic_receipt;
        if ("CHAT".equals(type)) iconRes = R.drawable.ic_message_square;
        if ("REVIEW".equals(type)) iconRes = R.drawable.ic_star;
        
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#1A1A1A"));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconLp.setMarginEnd(dp(16));
        card.addView(icon, iconLp);

        // Text Content
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        card.addView(textCol, textLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#1A1A1A"));
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textCol.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.parseColor("#6E6E73"));
        tvMsg.setTextSize(14);
        tvMsg.setPadding(0, dp(4), 0, 0);
        textCol.addView(tvMsg);

        TextView tvTime = new TextView(this);
        try {
            SimpleDateFormat s1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date d = s1.parse(dateStr);
            SimpleDateFormat s2 = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US);
            tvTime.setText(d != null ? s2.format(d) : dateStr);
        } catch (Exception e) {
            tvTime.setText(dateStr);
        }
        tvTime.setTextColor(Color.parseColor("#9A9A9E"));
        tvTime.setTextSize(12);
        tvTime.setPadding(0, dp(6), 0, 0);
        textCol.addView(tvTime);

        // Delete button
        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(R.drawable.ic_close);
        btnDelete.setColorFilter(Color.parseColor("#9A9A9E"));
        btnDelete.setPadding(dp(8), dp(8), dp(8), dp(8));
        btnDelete.setOnClickListener(v -> {
            apiClient.deleteNotification(notifId, new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> loadNotifications());
                }
            });
        });
        card.addView(btnDelete);

        // Click Action
        card.setOnClickListener(v -> {
            if (!isRead) {
                apiClient.markNotificationAsRead(notifId, new Callback() {
                    @Override public void onFailure(Call call, IOException e) {}
                    @Override public void onResponse(Call call, Response response) throws IOException {}
                });
            }
            
            // Navigate based on type
            if ("BOOKING".equals(type)) {
                startActivity(new Intent(this, BookingRequestsActivity.class));
            } else if ("PAYMENT".equals(type)) {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            } else if ("CHAT".equals(type)) {
                startActivity(new Intent(this, ChatHistoryActivity.class));
            } else if ("REVIEW".equals(type)) {
                startActivity(new Intent(this, MyReviewsActivity.class));
            }
        });

        return card;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}