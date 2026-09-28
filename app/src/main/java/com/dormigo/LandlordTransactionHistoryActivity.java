package com.dormigo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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

public class LandlordTransactionHistoryActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {

    private ApiClient apiClient;
    private int landlordId;
    private int pendingOpenPaymentId = -1;

    private TextView totalRevenueText;
    private TextView txStatus;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private final List<JSONObject> transactionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_transaction_history);

        apiClient = new ApiClient();

        pendingOpenPaymentId = getIntent().getIntExtra("openPaymentId", -1);

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

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        totalRevenueText = findViewById(R.id.totalRevenueText);
        txStatus = findViewById(R.id.txStatus);
        recyclerView = findViewById(R.id.recyclerViewTransactions);

        setupRecyclerView();
        loadLandlordTransactions();
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new TransactionAdapter(this, transactionList, this);
        recyclerView.setAdapter(adapter);
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
                    double totalRevenue = 0.0;

                    if (success && data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            loaded.add(obj);

                            String status = obj.optString("status", "").toUpperCase(Locale.ROOT);
                            if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                                totalRevenue += obj.optDouble("amount", 0.0);
                            }
                        }
                    }

                    final double finalRevenue = totalRevenue;
                    runOnUiThread(() -> {
                        transactionList.clear();
                        transactionList.addAll(loaded);

                        if (totalRevenueText != null) {
                            totalRevenueText.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(finalRevenue));
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        if (loaded.isEmpty()) {
                            if (txStatus != null) {
                                txStatus.setVisibility(View.VISIBLE);
                                txStatus.setText("No incoming transactions found.");
                            }
                        } else {
                            if (txStatus != null) {
                                txStatus.setVisibility(View.GONE);
                            }
                        }

                        if (pendingOpenPaymentId != -1) {
                            JSONObject targetTx = null;
                            for (JSONObject tx : loaded) {
                                if (tx.optInt("payment_id", -1) == pendingOpenPaymentId) {
                                    targetTx = tx;
                                    break;
                                }
                            }
                            if (targetTx != null) {
                                final JSONObject found = targetTx;
                                pendingOpenPaymentId = -1;
                                showPaymentDetailsDialog(found);
                            } else {
                                pendingOpenPaymentId = -1;
                                Toast.makeText(LandlordTransactionHistoryActivity.this, "Payment could not be found.", Toast.LENGTH_SHORT).show();
                            }
                        }
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
        View btnCloseDialog = view.findViewById(R.id.btnCloseDialog);

        View layoutProofContainer = view.findViewById(R.id.layoutProofContainer);
        ImageView dialogProofImage = view.findViewById(R.id.dialogProofImage);
        View layoutRejectionContainer = view.findViewById(R.id.layoutRejectionContainer);
        TextView dialogRejectionReasonText = view.findViewById(R.id.dialogRejectionReasonText);
        View layoutVerificationActions = view.findViewById(R.id.layoutVerificationActions);
        TextView btnConfirmPayment = view.findViewById(R.id.btnConfirmPayment);
        TextView btnRejectPayment = view.findViewById(R.id.btnRejectPayment);

        int paymentId = tx.optInt("payment_id", 0);
        double amt = tx.optDouble("amount", 0.0);
        String period = tx.optString("payment_description", tx.optString("payment_period", "Monthly Rent"));
        String houseName = tx.optString("house_name", "Boarding House");
        String roomNumber = tx.optString("room_number", "");
        String method = tx.optString("payment_method", "Payment Method");
        String dateStr = tx.optString("payment_date", tx.optString("due_date", "Recent"));
        String ref = tx.optString("transaction_ref", "");
        String status = tx.optString("status", "PENDING").toUpperCase(Locale.ROOT);
        String proofImage = tx.optString("proof_image", "");
        String rejectionReason = tx.optString("rejection_reason", "");

        if (dialogAmountText != null) {
            dialogAmountText.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(amt));
        }

        if (dialogStatusBadge != null) {
            if ("SUBMITTED".equals(status)) {
                dialogStatusBadge.setText("Awaiting Verification");
                dialogStatusBadge.setBackgroundResource(R.drawable.bg_circle_orange);
                dialogStatusBadge.setTextColor(0xFFFD7E14);
            } else if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
                dialogStatusBadge.setText("Paid");
                dialogStatusBadge.setBackgroundResource(R.drawable.bg_circle_green_light);
                dialogStatusBadge.setTextColor(0xFF1B5E4C);
            } else if ("REJECTED".equals(status)) {
                dialogStatusBadge.setText("Rejected");
                dialogStatusBadge.setBackgroundColor(0xFFFEEAEA);
                dialogStatusBadge.setTextColor(0xFFC53030);
            } else {
                dialogStatusBadge.setText("Pending");
                dialogStatusBadge.setBackgroundResource(R.drawable.bg_circle_orange);
                dialogStatusBadge.setTextColor(0xFFFD7E14);
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

        // Receipt Proof Display
        if (layoutProofContainer != null && dialogProofImage != null) {
            if (proofImage != null && !proofImage.trim().isEmpty() && !"null".equalsIgnoreCase(proofImage.trim())) {
                layoutProofContainer.setVisibility(View.VISIBLE);
                String fullUrl = proofImage.startsWith("http") ? proofImage : "http://10.149.229.109/Dormigo_Backend/" + proofImage;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.ic_receipt)
                        .into(dialogProofImage);

                dialogProofImage.setOnClickListener(v -> {
                    Dialog zoomDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    ImageView zoomView = new ImageView(this);
                    zoomView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    zoomView.setBackgroundColor(0xFF000000);
                    Glide.with(this).load(fullUrl).into(zoomView);
                    zoomView.setOnClickListener(z -> zoomDialog.dismiss());
                    zoomDialog.setContentView(zoomView);
                    zoomDialog.show();
                });
            } else {
                layoutProofContainer.setVisibility(View.GONE);
            }
        }

        // Rejection Reason Display
        if (layoutRejectionContainer != null && dialogRejectionReasonText != null) {
            if ("REJECTED".equals(status) && rejectionReason != null && !rejectionReason.trim().isEmpty() && !"null".equalsIgnoreCase(rejectionReason.trim())) {
                layoutRejectionContainer.setVisibility(View.VISIBLE);
                dialogRejectionReasonText.setText(rejectionReason.trim());
            } else {
                layoutRejectionContainer.setVisibility(View.GONE);
            }
        }

        // Verification Actions for SUBMITTED
        if (layoutVerificationActions != null) {
            if ("SUBMITTED".equals(status)) {
                layoutVerificationActions.setVisibility(View.VISIBLE);

                if (btnConfirmPayment != null) {
                    btnConfirmPayment.setOnClickListener(v -> {
                        new AlertDialog.Builder(this)
                                .setTitle("Confirm Payment")
                                .setMessage("Confirm this payment? Please ensure that the transaction reference and receipt match the payment received in your actual payment account.")
                                .setPositiveButton("Confirm", (d, w) -> {
                                    apiClient.verifyPayment(paymentId, "PAID", null, new Callback() {
                                        @Override
                                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                            runOnUiThread(() -> Toast.makeText(LandlordTransactionHistoryActivity.this, "Network error confirming payment.", Toast.LENGTH_SHORT).show());
                                        }

                                        @Override
                                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                            String body = response.body() != null ? response.body().string() : "";
                                            runOnUiThread(() -> {
                                                try {
                                                    JSONObject j = new JSONObject(body);
                                                    if (j.optBoolean("success", false)) {
                                                        Toast.makeText(LandlordTransactionHistoryActivity.this, "Payment confirmed successfully.", Toast.LENGTH_SHORT).show();
                                                        bottomSheetDialog.dismiss();
                                                        loadLandlordTransactions();
                                                    } else {
                                                        Toast.makeText(LandlordTransactionHistoryActivity.this, j.optString("message", "Failed to confirm payment."), Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (Exception e) {
                                                    Toast.makeText(LandlordTransactionHistoryActivity.this, "Failed to parse confirmation response.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }

                if (btnRejectPayment != null) {
                    btnRejectPayment.setOnClickListener(v -> {
                        EditText inputReason = new EditText(this);
                        inputReason.setHint("Enter reason (e.g., Reference mismatch)");
                        inputReason.setPadding(32, 32, 32, 32);

                        new AlertDialog.Builder(this)
                                .setTitle("Reject Payment")
                                .setMessage("Please enter the reason for rejecting this payment proof:")
                                .setView(inputReason)
                                .setPositiveButton("Reject", (d, w) -> {
                                    String reason = inputReason.getText().toString().trim();
                                    if (reason.isEmpty()) {
                                        Toast.makeText(this, "Rejection reason cannot be empty.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    apiClient.verifyPayment(paymentId, "REJECTED", reason, new Callback() {
                                        @Override
                                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                            runOnUiThread(() -> Toast.makeText(LandlordTransactionHistoryActivity.this, "Network error rejecting payment.", Toast.LENGTH_SHORT).show());
                                        }

                                        @Override
                                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                            String body = response.body() != null ? response.body().string() : "";
                                            runOnUiThread(() -> {
                                                try {
                                                    JSONObject j = new JSONObject(body);
                                                    if (j.optBoolean("success", false)) {
                                                        Toast.makeText(LandlordTransactionHistoryActivity.this, "Payment rejected.", Toast.LENGTH_SHORT).show();
                                                        bottomSheetDialog.dismiss();
                                                        loadLandlordTransactions();
                                                    } else {
                                                        Toast.makeText(LandlordTransactionHistoryActivity.this, j.optString("message", "Failed to reject payment."), Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (Exception e) {
                                                    Toast.makeText(LandlordTransactionHistoryActivity.this, "Failed to parse rejection response.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }

            } else {
                layoutVerificationActions.setVisibility(View.GONE);
            }
        }

        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }
}
