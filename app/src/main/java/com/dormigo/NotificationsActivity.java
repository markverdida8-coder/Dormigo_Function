package com.dormigo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Adjust for system bars
        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupUI();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupFilters();
    }

    private void setupFilters() {
        TextView filterAll = findViewById(R.id.filterAll);
        TextView filterRequests = findViewById(R.id.filterRequests);
        TextView filterPayments = findViewById(R.id.filterPayments);
        TextView filterMessages = findViewById(R.id.filterMessages);

        View[] notifItems = {
                findViewById(R.id.notifItem1), // Request
                findViewById(R.id.notifItem2), // Message
                findViewById(R.id.notifItem3), // Payment
                findViewById(R.id.notifItem4), // Other (show in All)
                findViewById(R.id.notifItem5), // Request
                findViewById(R.id.notifItem6)  // Payment
        };

        TextView[] filters = {filterAll, filterRequests, filterPayments, filterMessages};

        View.OnClickListener filterListener = v -> {
            // Reset all filters to unselected state
            for (TextView filter : filters) {
                if (filter != null) {
                    filter.setBackgroundResource(R.drawable.bg_chip_selectable);
                    filter.setTextColor(0xFF6E6E73);
                    filter.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }

            // Set selected state for clicked filter
            TextView selectedFilter = (TextView) v;
            selectedFilter.setBackgroundResource(R.drawable.bg_button_filled);
            selectedFilter.setTextColor(0xFFFFFFFF);
            selectedFilter.setTypeface(null, android.graphics.Typeface.BOLD);

            // Filter logic
            int id = v.getId();
            if (id == R.id.filterAll) {
                for (View item : notifItems) {
                    if (item != null) item.setVisibility(View.VISIBLE);
                }
            } else if (id == R.id.filterRequests) {
                setVisibility(notifItems, new int[]{0, 4});
            } else if (id == R.id.filterPayments) {
                setVisibility(notifItems, new int[]{2, 5});
            } else if (id == R.id.filterMessages) {
                setVisibility(notifItems, new int[]{1});
            }
        };

        if (filterAll != null) filterAll.setOnClickListener(filterListener);
        if (filterRequests != null) filterRequests.setOnClickListener(filterListener);
        if (filterPayments != null) filterPayments.setOnClickListener(filterListener);
        if (filterMessages != null) filterMessages.setOnClickListener(filterListener);
    }

    private void setVisibility(View[] items, int[] visibleIndices) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                boolean shouldShow = false;
                for (int index : visibleIndices) {
                    if (i == index) {
                        shouldShow = true;
                        break;
                    }
                }
                items[i].setVisibility(shouldShow ? View.VISIBLE : View.GONE);
            }
        }
    }
}