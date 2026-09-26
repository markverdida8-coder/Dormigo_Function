package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TransactionHistoryActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private int userId;
    private final List<JSONObject> transactionList = new ArrayList<>();
    private String currentFilter = "ALL";

    private TextView totalPaidAmount;
    private TextView txStatus;
    private LinearLayout transactionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction_history);

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        // Adjust for system bars
        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                View bottomNav = findViewById(R.id.bottomNav);
                if (bottomNav != null) {
                    bottomNav.setPadding(0, 0, 0, systemBars.bottom);
                }
                return insets;
            });
        }

        totalPaidAmount = findViewById(R.id.totalPaidAmount);
        txStatus = findViewById(R.id.txStatus);
        transactionsContainer = findViewById(R.id.transactionsContainer);

        setupUI();
        setupBottomNavigation();
        setupFilters();
        loadTransactions();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnDownload = findViewById(R.id.btnDownload);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> showToast("Downloading report..."));
        }
    }

    private void setupFilters() {
        TextView filterAll = findViewById(R.id.filterAll);
        TextView filterPaid = findViewById(R.id.filterPaid);
        TextView filterPending = findViewById(R.id.filterPending);
        TextView filterFailed = findViewById(R.id.filterFailed);

        TextView[] filters = {filterAll, filterPaid, filterPending, filterFailed};

        View.OnClickListener filterListener = v -> {
            for (TextView f : filters) {
                if (f != null) {
                    f.setBackgroundResource(R.drawable.bg_chip);
                    f.setTextColor(0xFF6E6E73);
                    f.setTypeface(null, Typeface.NORMAL);
                }
            }

            TextView selected = (TextView) v;
            selected.setBackgroundResource(R.drawable.bg_button_filled);
            selected.setTextColor(0xFFFFFFFF);
            selected.setTypeface(null, Typeface.BOLD);

            int id = v.getId();
            if (id == R.id.filterAll) {
                currentFilter = "ALL";
            } else if (id == R.id.filterPaid) {
                currentFilter = "PAID";
            } else if (id == R.id.filterPending) {
                currentFilter = "PENDING";
            } else if (id == R.id.filterFailed) {
                currentFilter = "FAILED";
            }

            renderTransactions();
        };

        if (filterAll != null) filterAll.setOnClickListener(filterListener);
        if (filterPaid != null) filterPaid.setOnClickListener(filterListener);
        if (filterPending != null) filterPending.setOnClickListener(filterListener);
        if (filterFailed != null) filterFailed.setOnClickListener(filterListener);
    }

    private void loadTransactions() {
        if (userId <= 0) {
            if (txStatus != null) {
                txStatus.setVisibility(View.VISIBLE);
                txStatus.setText("User account not found. Please log in.");
            }
            return;
        }

        if (txStatus != null) {
            txStatus.setVisibility(View.VISIBLE);
            txStatus.setText("Loading transactions...");
        }

        apiClient.getPaymentsForUser(userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (txStatus != null) {
                        txStatus.setVisibility(View.VISIBLE);
                        txStatus.setText("Failed to load transactions. Check your network.");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (txStatus != null) {
                            txStatus.setVisibility(View.VISIBLE);
                            txStatus.setText("Error loading transactions from server.");
                        }
                    });
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    boolean success = json.optBoolean("success", false);
                    JSONArray data = json.optJSONArray("data");

                    List<JSONObject> loaded = new ArrayList<>();
                    double totalPaid = 0.0;

                    if (success && data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            loaded.add(obj);

                            String status = obj.optString("status", "").toUpperCase();
                            if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                                totalPaid += obj.optDouble("amount", 0.0);
                            }
                        }
                    }

                    final double finalTotal = totalPaid;
                    runOnUiThread(() -> {
                        transactionList.clear();
                        transactionList.addAll(loaded);

                        if (totalPaidAmount != null) {
                            totalPaidAmount.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(finalTotal));
                        }

                        renderTransactions();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        if (txStatus != null) {
                            txStatus.setVisibility(View.VISIBLE);
                            txStatus.setText("Failed to parse transaction records.");
                        }
                    });
                }
            }
        });
    }

    private void renderTransactions() {
        if (transactionsContainer == null) return;
        transactionsContainer.removeAllViews();

        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject obj : transactionList) {
            String status = obj.optString("status", "").toUpperCase();
            if ("ALL".equals(currentFilter)) {
                filtered.add(obj);
            } else if ("PAID".equals(currentFilter)) {
                if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                    filtered.add(obj);
                }
            } else if ("PENDING".equals(currentFilter)) {
                if ("PENDING".equals(status)) {
                    filtered.add(obj);
                }
            } else if ("FAILED".equals(currentFilter)) {
                if ("FAILED".equals(status) || "CANCELLED".equals(status) || "REJECTED".equals(status)) {
                    filtered.add(obj);
                }
            }
        }

        if (filtered.isEmpty()) {
            if (txStatus != null) {
                txStatus.setVisibility(View.VISIBLE);
                txStatus.setText("No transactions found.");
            }
            return;
        }

        if (txStatus != null) {
            txStatus.setVisibility(View.GONE);
        }

        for (JSONObject tx : filtered) {
            View card = createTransactionCard(tx);
            transactionsContainer.addView(card);
        }
    }

    private View createTransactionCard(JSONObject tx) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_rounded);
        int pad = dpToPx(16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.bottomMargin = dpToPx(12);
        card.setLayoutParams(cardLp);

        // Header row: Title (left) & Amount (right)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        String period = tx.optString("payment_period", "");
        if (period.isEmpty() || period.matches("\\d+")) {
            tvTitle.setText("Monthly Rent");
        } else {
            tvTitle.setText(period);
        }
        tvTitle.setTextColor(0xFF1A1A1A);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvTitle.setLayoutParams(titleLp);
        headerRow.addView(tvTitle);

        TextView tvAmount = new TextView(this);
        double amt = tx.optDouble("amount", 0.0);
        tvAmount.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(amt));
        tvAmount.setTextColor(0xFF1A1A1A);
        tvAmount.setTextSize(15);
        tvAmount.setTypeface(null, Typeface.BOLD);
        headerRow.addView(tvAmount);

        card.addView(headerRow);

        // House & Room Info
        TextView tvHouse = new TextView(this);
        String houseName = tx.optString("house_name", "Boarding House");
        String roomNumber = tx.optString("room_number", "");
        String roomType = tx.optString("room_type", "");
        StringBuilder houseInfo = new StringBuilder(houseName);
        if (!roomNumber.isEmpty()) {
            houseInfo.append(" · Room ").append(roomNumber);
        }
        if (!roomType.isEmpty()) {
            houseInfo.append(" (").append(roomType).append(")");
        }
        tvHouse.setText(houseInfo.toString());
        tvHouse.setTextColor(0xFF9A9A9E);
        tvHouse.setTextSize(13);
        LinearLayout.LayoutParams houseLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        houseLp.topMargin = dpToPx(4);
        tvHouse.setLayoutParams(houseLp);
        card.addView(tvHouse);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(0xFFEFEFEF);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        divLp.topMargin = dpToPx(16);
        divider.setLayoutParams(divLp);
        card.addView(divider);

        // Bottom row: Info (date & ref) + Status badge
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bRowLp.topMargin = dpToPx(16);
        bottomRow.setLayoutParams(bRowLp);

        // Left info column
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoCol.setLayoutParams(infoColLp);

        TextView tvDate = new TextView(this);
        String date = tx.optString("payment_date", tx.optString("due_date", "Recent"));
        String method = tx.optString("payment_method", "Payment");
        tvDate.setText(date + " · " + method);
        tvDate.setTextColor(0xFF9A9A9E);
        tvDate.setTextSize(12);
        infoCol.addView(tvDate);

        TextView tvRef = new TextView(this);
        String ref = tx.optString("transaction_ref", "BHF-" + tx.optInt("payment_id"));
        tvRef.setText("Ref: " + ref);
        tvRef.setTextColor(0xFF9A9A9E);
        tvRef.setTextSize(12);
        LinearLayout.LayoutParams refLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        refLp.topMargin = dpToPx(2);
        tvRef.setLayoutParams(refLp);
        infoCol.addView(tvRef);

        bottomRow.addView(infoCol);

        // Right status badge
        TextView tvStatusBadge = new TextView(this);
        String status = tx.optString("status", "PENDING").toUpperCase();
        tvStatusBadge.setText(status);
        tvStatusBadge.setTextSize(11);
        tvStatusBadge.setTypeface(null, Typeface.BOLD);
        tvStatusBadge.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        tvStatusBadge.setCompoundDrawablePadding(dpToPx(6));

        if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
            tvStatusBadge.setBackgroundColor(0xFFF2F9F7);
            tvStatusBadge.setTextColor(0xFF1B5E4C);
            tvStatusBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_verified, 0, 0, 0);
            tvStatusBadge.getCompoundDrawables()[0].setTint(0xFF1B5E4C);
        } else if ("PENDING".equals(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_info_box);
            tvStatusBadge.setTextColor(0xFF6E6E73);
            tvStatusBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bell, 0, 0, 0);
            tvStatusBadge.getCompoundDrawables()[0].setTint(0xFF6E6E73);
        } else {
            tvStatusBadge.setBackgroundColor(0xFFFEEAEA);
            tvStatusBadge.setTextColor(0xFFC53030);
        }

        bottomRow.addView(tvStatusBadge);
        card.addView(bottomRow);

        return card;
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_explore);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_chats) {
                Intent intent = new Intent(this, ChatHistoryActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return true;
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
