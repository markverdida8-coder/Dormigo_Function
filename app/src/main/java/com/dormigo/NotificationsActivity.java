package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private final ApiClient apiClient = new ApiClient();
    private int userId;
    private final List<JSONObject> rawNotificationList = new ArrayList<>();
    private final List<JSONObject> filteredNotificationList = new ArrayList<>();
    private String currentFilter = "ALL";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private LinearLayout emptyStateLayout;
    private TextView textUnreadCount;

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

        bindViews();
        setupUI();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        textUnreadCount = findViewById(R.id.textUnreadCount);
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
                        @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                        @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                            runOnUiThread(() -> loadNotifications());
                        }
                    });
                }
            });
        }

        setupFilters();
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new NotificationAdapter(this, filteredNotificationList, this);
        recyclerView.setAdapter(adapter);

        // Swipe-to-dismiss
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < filteredNotificationList.size()) {
                    adapter.removeItem(position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
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

            filterAndRenderNotifications();
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        runOnUiThread(() -> {
                            rawNotificationList.clear();
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    rawNotificationList.add(data.optJSONObject(i));
                                }
                            }
                            filterAndRenderNotifications();
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void filterAndRenderNotifications() {
        filteredNotificationList.clear();
        int unreadCount = 0;

        for (JSONObject notif : rawNotificationList) {
            boolean isRead = notif.optBoolean("is_read", false);
            if (!isRead) unreadCount++;

            String type = notif.optString("type", "").toUpperCase(Locale.ROOT);
            if ("ALL".equals(currentFilter) || type.equals(currentFilter)) {
                filteredNotificationList.add(notif);
            }
        }

        if (textUnreadCount != null) {
            if (unreadCount > 0) {
                textUnreadCount.setText(unreadCount + " unread notification" + (unreadCount == 1 ? "" : "s"));
            } else {
                textUnreadCount.setText("All caught up");
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (filteredNotificationList.isEmpty()) {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNotificationClick(JSONObject notif, int position) {
        int notifId = notif.optInt("notification_id", 0);
        boolean isRead = notif.optBoolean("is_read", false);

        if (!isRead && notifId > 0) {
            try {
                notif.put("is_read", true);
            } catch (Exception ignored) {}

            apiClient.markNotificationAsRead(notifId, new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
            });
            filterAndRenderNotifications();
        }

        // Centralized Role-Based Notification Routing
        NotificationRouter.route(this, notif);
    }

    @Override
    public void onNotificationDismissed(JSONObject notif, int position) {
        int notifId = notif.optInt("notification_id", 0);
        rawNotificationList.remove(notif);

        if (notifId > 0) {
            apiClient.deleteNotification(notifId, new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
            });
        }

        filterAndRenderNotifications();
    }
}
