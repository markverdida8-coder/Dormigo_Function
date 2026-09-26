package com.dormigo;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class LandlordTransactionHistoryActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private int landlordId;

    private TextView totalRevenueText;
    private TextView txStatus;
    private LinearLayout transactionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_transaction_history);

        apiClient = new ApiClient();

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        landlordId = prefs.getInt("userId", -1);

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

        totalRevenueText = findViewById(R.id.totalRevenueText);
        txStatus = findViewById(R.id.txStatus);
        transactionsContainer = findViewById(R.id.transactionsContainer);

        loadLandlordTransactions();
    }

    private void loadLandlordTransactions() {
        if (landlordId <= 0) {
            if (txStatus != null) {
                txStatus.setVisibility(View.VISIBLE);
                txStatus.setText("Landlord account not found. Please log in.");
            }
            return;
        }

        if (txStatus != null) {
            txStatus.setVisibility(View.VISIBLE);
            txStatus.setText("Loading incoming transactions...");
        }

        apiClient.getPaymentsForLandlord(landlordId, new Callback() {
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
                    double totalRevenue = 0.0;

                    if (success && data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            loaded.add(obj);

                            String status = obj.optString("status", "").toUpperCase();
                            if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                                totalRevenue += obj.optDouble("amount", 0.0);
                            }
                        }
                    }

                    final double finalRevenue = totalRevenue;
                    runOnUiThread(() -> {
                        if (totalRevenueText != null) {
                            totalRevenueText.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(finalRevenue));
                        }

                        renderLandlordTransactions(loaded);
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

    private void renderLandlordTransactions(List<JSONObject> transactions) {
        if (transactionsContainer == null) return;
        transactionsContainer.removeAllViews();

        if (transactions.isEmpty()) {
            if (txStatus != null) {
                txStatus.setVisibility(View.VISIBLE);
                txStatus.setText("No incoming transactions found.");
            }
            return;
        }

        if (txStatus != null) {
            txStatus.setVisibility(View.GONE);
        }

        for (int i = 0; i < transactions.size(); i++) {
            JSONObject tx = transactions.get(i);
            View itemView = createLandlordTransactionItem(tx);
            transactionsContainer.addView(itemView);

            if (i < transactions.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFEFEFEF);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                );
                divider.setLayoutParams(divLp);
                transactionsContainer.addView(divider);
            }
        }
    }

    private View createLandlordTransactionItem(JSONObject tx) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dpToPx(14), 0, dpToPx(14));

        // Row 1: Tenant name (left) & +₱Amount (right)
        RelativeLayout row1 = new RelativeLayout(this);
        RelativeLayout.LayoutParams row1Lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row1.setLayoutParams(row1Lp);

        TextView tvTenant = new TextView(this);
        String tenantName = tx.optString("tenant_name", "Tenant");
        tvTenant.setText(tenantName);
        tvTenant.setTextColor(0xFF1A1A1A);
        tvTenant.setTextSize(14);
        tvTenant.setTypeface(null, Typeface.BOLD);
        row1.addView(tvTenant);

        TextView tvAmount = new TextView(this);
        double amt = tx.optDouble("amount", 0.0);
        tvAmount.setText("+₱" + NumberFormat.getNumberInstance(Locale.US).format(amt));
        tvAmount.setTextColor(0xFF1B5E4C);
        tvAmount.setTextSize(15);
        tvAmount.setTypeface(null, Typeface.BOLD);
        RelativeLayout.LayoutParams amtLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        amtLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvAmount.setLayoutParams(amtLp);
        row1.addView(tvAmount);

        item.addView(row1);

        // Row 2: Room & period / house
        TextView tvDetail = new TextView(this);
        String house = tx.optString("house_name", "");
        String room = tx.optString("room_number", "");
        String period = tx.optString("payment_period", "Rent");
        StringBuilder desc = new StringBuilder();
        if (!room.isEmpty()) {
            desc.append("Room ").append(room);
        } else if (!house.isEmpty()) {
            desc.append(house);
        }
        if (!period.isEmpty()) {
            if (desc.length() > 0) desc.append(" · ");
            if (period.matches("\\d+")) {
                desc.append("Period ").append(period);
            } else {
                desc.append(period);
            }
        }
        tvDetail.setText(desc.toString());
        tvDetail.setTextColor(0xFF9A9A9E);
        tvDetail.setTextSize(12);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descLp.topMargin = dpToPx(2);
        tvDetail.setLayoutParams(descLp);
        item.addView(tvDetail);

        // Row 3: Reference & Date
        RelativeLayout row3 = new RelativeLayout(this);
        RelativeLayout.LayoutParams row3Lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row3Lp.topMargin = dpToPx(6);
        row3.setLayoutParams(row3Lp);

        TextView tvRef = new TextView(this);
        String ref = tx.optString("transaction_ref", "BHF-" + tx.optInt("payment_id"));
        tvRef.setText("Ref: " + ref);
        tvRef.setTextColor(0xFF1B5E4C);
        tvRef.setTextSize(12);
        tvRef.setTypeface(null, Typeface.BOLD);
        row3.addView(tvRef);

        TextView tvDate = new TextView(this);
        String date = tx.optString("payment_date", tx.optString("due_date", ""));
        tvDate.setText(date);
        tvDate.setTextColor(0xFF9A9A9E);
        tvDate.setTextSize(11);
        RelativeLayout.LayoutParams dateLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvDate.setLayoutParams(dateLp);
        row3.addView(tvDate);

        item.addView(row3);

        return item;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
