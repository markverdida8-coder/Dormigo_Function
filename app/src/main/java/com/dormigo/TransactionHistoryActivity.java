package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class TransactionHistoryActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {

    private ApiClient apiClient;
    private int userId;
    private final List<JSONObject> transactionList = new ArrayList<>();
    private final List<JSONObject> filteredList = new ArrayList<>();
    private String currentFilter = "ALL";

    private TextView totalPaidAmount;
    private TextView textSummaryCount;
    private TextView textSummaryLastDate;
    private TextView txStatus;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction_history);

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

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

        bindViews();
        setupUI();
        setupRecyclerView();
        setupBottomNavigation();
        setupFilters();
        loadTransactions();
    }

    private void bindViews() {
        totalPaidAmount = findViewById(R.id.totalPaidAmount);
        textSummaryCount = findViewById(R.id.textSummaryCount);
        textSummaryLastDate = findViewById(R.id.textSummaryLastDate);
        txStatus = findViewById(R.id.txStatus);
        recyclerView = findViewById(R.id.recyclerViewTransactions);
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

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new TransactionAdapter(this, filteredList, this);
        recyclerView.setAdapter(adapter);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (txStatus != null) {
                        txStatus.setVisibility(View.VISIBLE);
                        txStatus.setText("Failed to load transactions. Check your network.");
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
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
                    int successCount = 0;
                    String lastDate = "";

                    if (success && data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            loaded.add(obj);

                            String status = obj.optString("status", "").toUpperCase(Locale.ROOT);
                            if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                                totalPaid += obj.optDouble("amount", 0.0);
                                successCount++;
                                if (lastDate.isEmpty()) {
                                    lastDate = obj.optString("payment_date", obj.optString("due_date", "Recent"));
                                }
                            }
                        }
                    }

                    final double finalTotal = totalPaid;
                    final int finalCount = successCount;
                    final String finalLastDate = lastDate;

                    runOnUiThread(() -> {
                        transactionList.clear();
                        transactionList.addAll(loaded);

                        if (totalPaidAmount != null) {
                            totalPaidAmount.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(finalTotal));
                        }
                        if (textSummaryCount != null) {
                            textSummaryCount.setText(finalCount + " successful payment" + (finalCount == 1 ? "" : "s"));
                        }
                        if (textSummaryLastDate != null) {
                            textSummaryLastDate.setText("Last payment: " + (finalLastDate.isEmpty() ? "None" : TransactionAdapter.getRelativeTimeSpanString(finalLastDate)));
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
        filteredList.clear();

        for (JSONObject obj : transactionList) {
            String status = obj.optString("status", "").toUpperCase(Locale.ROOT);
            if ("ALL".equals(currentFilter)) {
                filteredList.add(obj);
            } else if ("PAID".equals(currentFilter)) {
                if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                    filteredList.add(obj);
                }
            } else if ("PENDING".equals(currentFilter)) {
                if ("PENDING".equals(status) || "SUBMITTED".equals(status)) {
                    filteredList.add(obj);
                }
            } else if ("FAILED".equals(currentFilter)) {
                if ("FAILED".equals(status) || "CANCELLED".equals(status) || "REJECTED".equals(status)) {
                    filteredList.add(obj);
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (filteredList.isEmpty()) {
            if (txStatus != null) {
                txStatus.setVisibility(View.VISIBLE);
                txStatus.setText("No transactions found.");
            }
        } else {
            if (txStatus != null) {
                txStatus.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onTransactionClick(JSONObject tx, int position) {
        showPaymentDetailsDialog(tx);
    }

    private void showPaymentDetailsDialog(JSONObject tx) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_payment_details, findViewById(R.id.mainLayout), false);
        bottomSheetDialog.setContentView(view);

        TextView dialogAmountText = view.findViewById(R.id.dialogAmountText);
        TextView dialogStatusBadge = view.findViewById(R.id.dialogStatusBadge);
        TextView dialogTitleText = view.findViewById(R.id.dialogTitleText);
        TextView dialogHouseRoomText = view.findViewById(R.id.dialogHouseRoomText);
        TextView dialogMethodText = view.findViewById(R.id.dialogMethodText);
        TextView dialogDateText = view.findViewById(R.id.dialogDateText);
        TextView dialogRefText = view.findViewById(R.id.dialogRefText);
        TextView btnCloseDialog = view.findViewById(R.id.btnCloseDialog);

        double amt = tx.optDouble("amount", 0.0);
        String period = tx.optString("payment_description", tx.optString("payment_period", "Monthly Rent"));
        String houseName = tx.optString("house_name", "Boarding House");
        String roomNumber = tx.optString("room_number", "");
        String method = tx.optString("payment_method", "Payment Method");
        String dateStr = tx.optString("payment_date", tx.optString("due_date", "Recent"));
        String ref = tx.optString("transaction_ref", "");
        String status = tx.optString("status", "PENDING").toUpperCase(Locale.ROOT);

        if (dialogAmountText != null) {
            dialogAmountText.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(amt));
        }

        if (dialogStatusBadge != null) {
            dialogStatusBadge.setText(status);
            if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                dialogStatusBadge.setBackgroundResource(R.drawable.bg_circle_green_light);
                dialogStatusBadge.setTextColor(0xFF1B5E4C);
            } else if ("PENDING".equals(status)) {
                dialogStatusBadge.setBackgroundResource(R.drawable.bg_circle_orange);
                dialogStatusBadge.setTextColor(0xFFFD7E14);
            } else {
                dialogStatusBadge.setBackgroundColor(0xFFFEEAEA);
                dialogStatusBadge.setTextColor(0xFFC53030);
            }
        }

        if (dialogTitleText != null) dialogTitleText.setText(period);
        if (dialogHouseRoomText != null) {
            dialogHouseRoomText.setText(houseName + (roomNumber.isEmpty() ? "" : " · Room " + roomNumber));
        }
        if (dialogMethodText != null) dialogMethodText.setText(method);
        if (dialogDateText != null) dialogDateText.setText(dateStr);

        if (dialogRefText != null) {
            if (ref == null || ref.trim().isEmpty() || ref.equalsIgnoreCase("null")) {
                dialogRefText.setText("Reference unavailable");
                dialogRefText.setTextColor(0xFF9A9A9E);
            } else {
                dialogRefText.setText(ref.trim());
                dialogRefText.setTextColor(0xFF1B5E4C);
            }
        }

        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
