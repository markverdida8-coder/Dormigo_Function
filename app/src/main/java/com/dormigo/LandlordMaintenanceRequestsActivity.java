package com.dormigo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordMaintenanceRequestsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout requestsContainer;
    private View layoutEmptyState;
    private boolean isUpdatingRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_maintenance_requests);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        requestsContainer = findViewById(R.id.requestsContainer);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadRequests);
        }

        loadRequests();
    }

    private void loadRequests() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        apiClient.getLandlordMaintenanceRequests(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(LandlordMaintenanceRequestsActivity.this, "Unable to load maintenance requests. Check your connection.", Toast.LENGTH_SHORT).show();
                    if (requestsContainer != null && requestsContainer.getChildCount() == 0) {
                        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean successful = response.isSuccessful();
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    try {
                        JSONObject json = new JSONObject(body);
                        if (successful && json.optBoolean("success", false)) {
                            JSONArray data = json.optJSONArray("data");
                            renderRequests(data);
                        } else {
                            String msg = json.optString("message", "Failed to load requests.");
                            Toast.makeText(LandlordMaintenanceRequestsActivity.this, msg, Toast.LENGTH_LONG).show();
                            if (requestsContainer != null && requestsContainer.getChildCount() == 0) {
                                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(LandlordMaintenanceRequestsActivity.this, "Invalid server response.", Toast.LENGTH_SHORT).show();
                        if (requestsContainer != null && requestsContainer.getChildCount() == 0) {
                            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void renderRequests(JSONArray requests) {
        if (requestsContainer == null) return;
        requestsContainer.removeAllViews();

        if (requests == null || requests.length() == 0) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);

        for (int i = 0; i < requests.length(); i++) {
            JSONObject req = requests.optJSONObject(i);
            if (req != null) {
                int requestId = req.optInt("request_id", 0);
                String studentName = req.optString("student_name", "Student");
                String houseName = req.optString("house_name", "Boarding House");
                String roomNumber = req.optString("room_number", "Room");
                String category = req.optString("category", "General");
                String description = req.optString("description", "");
                String status = req.optString("status", "PENDING").toUpperCase(Locale.ROOT);
                String submissionDate = req.optString("submitted_at", "");
                String resolutionNotes = req.optString("resolution_notes", "");

                LinearLayout card = new LinearLayout(this);
                card.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(16), dp(16), dp(16), dp(16));
                card.setBackgroundResource(R.drawable.bg_card_rounded);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                params.bottomMargin = dp(12);
                card.setLayoutParams(params);

                // Header: Request ID + Status Badge
                LinearLayout header = new LinearLayout(this);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvId = new TextView(this);
                tvId.setText("Request #" + requestId);
                tvId.setTextColor(Color.parseColor("#1A1A1A"));
                tvId.setTextSize(15);
                tvId.setTypeface(null, Typeface.BOLD);
                header.addView(tvId);

                TextView tvStatus = new TextView(this);
                tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                tvStatus.setGravity(Gravity.END);
                tvStatus.setText(status.replace("_", " "));
                tvStatus.setTextSize(12);
                tvStatus.setTypeface(null, Typeface.BOLD);
                setStatusBadgeStyle(tvStatus, status);
                header.addView(tvStatus);

                card.addView(header);

                // Details: Student, House, Room, Category
                addDetailRow(card, "Tenant", studentName);
                addDetailRow(card, "Location", houseName + " · Room " + roomNumber);
                addDetailRow(card, "Category", category);

                // Description
                if (!description.isEmpty() && !"null".equalsIgnoreCase(description)) {
                    TextView tvDesc = new TextView(this);
                    tvDesc.setText("“" + description + "”");
                    tvDesc.setTextColor(Color.parseColor("#1A1A1A"));
                    tvDesc.setTextSize(14);
                    LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    dParams.topMargin = dp(8);
                    tvDesc.setLayoutParams(dParams);
                    card.addView(tvDesc);
                }

                // Resolution Notes if present
                if (!resolutionNotes.isEmpty() && !"null".equalsIgnoreCase(resolutionNotes)) {
                    TextView tvNotes = new TextView(this);
                    tvNotes.setText("Resolution Notes: " + resolutionNotes);
                    tvNotes.setTextColor(Color.parseColor("#1B5E4C"));
                    tvNotes.setTextSize(13);
                    LinearLayout.LayoutParams nParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    nParams.topMargin = dp(8);
                    tvNotes.setLayoutParams(nParams);
                    card.addView(tvNotes);
                }

                // Submission Date
                if (!submissionDate.isEmpty()) {
                    TextView tvDate = new TextView(this);
                    tvDate.setText("Submitted: " + formatDate(submissionDate));
                    tvDate.setTextColor(Color.parseColor("#9A9A9E"));
                    tvDate.setTextSize(11);
                    LinearLayout.LayoutParams dtParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    dtParams.topMargin = dp(8);
                    tvDate.setLayoutParams(dtParams);
                    card.addView(tvDate);
                }

                // Action Buttons based on status
                LinearLayout actionRow = new LinearLayout(this);
                actionRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams aParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                aParams.topMargin = dp(12);
                actionRow.setLayoutParams(aParams);

                if ("PENDING".equals(status)) {
                    TextView btnInProgress = createActionButton("Mark In Progress", 0xFF0D6EFD);
                    btnInProgress.setOnClickListener(v -> confirmAndUpdateStatus(requestId, "IN_PROGRESS", false));
                    actionRow.addView(btnInProgress);

                    TextView btnReject = createActionButton("Reject", 0xFFC53030);
                    LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
                    rParams.setMarginStart(dp(8));
                    btnReject.setLayoutParams(rParams);
                    btnReject.setOnClickListener(v -> confirmAndUpdateStatus(requestId, "REJECTED", true));
                    actionRow.addView(btnReject);

                    card.addView(actionRow);
                } else if ("IN_PROGRESS".equals(status)) {
                    TextView btnCompleted = createActionButton("Mark Completed", 0xFF1B5E4C);
                    btnCompleted.setOnClickListener(v -> confirmAndUpdateStatus(requestId, "COMPLETED", true));
                    actionRow.addView(btnCompleted);

                    TextView btnReject = createActionButton("Reject", 0xFFC53030);
                    LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
                    rParams.setMarginStart(dp(8));
                    btnReject.setLayoutParams(rParams);
                    btnReject.setOnClickListener(v -> confirmAndUpdateStatus(requestId, "REJECTED", true));
                    actionRow.addView(btnReject);

                    card.addView(actionRow);
                }

                requestsContainer.addView(card);
            }
        }
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + ": " + value);
        tv.setTextColor(Color.parseColor("#6E6E73"));
        tv.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(4);
        tv.setLayoutParams(params);
        parent.addView(tv);
    }

    private TextView createActionButton(String text, int bgColor) {
        TextView btn = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1f);
        btn.setLayoutParams(params);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(bgColor);
        return btn;
    }

    private void setStatusBadgeStyle(TextView tv, String status) {
        if ("PENDING".equals(status)) {
            tv.setTextColor(Color.parseColor("#D97706"));
        } else if ("IN_PROGRESS".equals(status)) {
            tv.setTextColor(Color.parseColor("#0D6EFD"));
        } else if ("COMPLETED".equals(status)) {
            tv.setTextColor(Color.parseColor("#1B5E4C"));
        } else if ("REJECTED".equals(status)) {
            tv.setTextColor(Color.parseColor("#C53030"));
        }
    }

    private void confirmAndUpdateStatus(int requestId, String targetStatus, boolean requireNotes) {
        if (isUpdatingRequest) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String actionName = targetStatus.equals("IN_PROGRESS") ? "Mark In Progress" : (targetStatus.equals("COMPLETED") ? "Mark Completed" : "Reject");
        builder.setTitle(actionName + " Request");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("Are you sure you want to update request #" + requestId + " to " + targetStatus.replace("_", " ") + "?");
        tvMsg.setTextColor(Color.parseColor("#1A1A1A"));
        tvMsg.setTextSize(14);
        layout.addView(tvMsg);

        final EditText inputNotes = new EditText(this);
        inputNotes.setHint(requireNotes ? "Enter resolution notes (Required, max 2000 chars)..." : "Enter resolution notes (Optional)...");
        inputNotes.setPadding(dp(12), dp(12), dp(12), dp(12));
        inputNotes.setBackgroundResource(R.drawable.bg_input_field);
        LinearLayout.LayoutParams edParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        edParams.topMargin = dp(12);
        inputNotes.setLayoutParams(edParams);
        layout.addView(inputNotes);

        builder.setView(layout);
        builder.setPositiveButton("Confirm", null); // Handled manually to prevent auto-dismissal on validation failure
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String notes = inputNotes.getText().toString().trim();
                if (requireNotes && notes.isEmpty()) {
                    Toast.makeText(this, "Resolution notes are required for this status.", Toast.LENGTH_LONG).show();
                    return; // Keep dialog open
                }
                if (notes.length() > 2000) {
                    Toast.makeText(this, "Resolution notes must not exceed 2000 characters.", Toast.LENGTH_LONG).show();
                    return; // Keep dialog open
                }

                isUpdatingRequest = true;
                positiveButton.setEnabled(false); // Disable during submission

                // Execute PATCH request
                apiClient.updateLandlordMaintenanceRequestStatus(requestId, targetStatus, notes.isEmpty() ? null : notes, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            isUpdatingRequest = false;
                            positiveButton.setEnabled(true); // Restore button on failure
                            Toast.makeText(LandlordMaintenanceRequestsActivity.this, "Network error updating request.", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        boolean successful = response.isSuccessful();
                        String body = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() -> {
                            isUpdatingRequest = false;
                            try {
                                JSONObject json = new JSONObject(body);
                                if (successful && json.optBoolean("success", false)) {
                                    Toast.makeText(LandlordMaintenanceRequestsActivity.this, "Maintenance request updated successfully.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss(); // Dismiss only on success
                                    loadRequests();
                                } else {
                                    positiveButton.setEnabled(true); // Restore button on failure
                                    String msg = json.optString("message", "Failed to update request.");
                                    Toast.makeText(LandlordMaintenanceRequestsActivity.this, msg, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                positiveButton.setEnabled(true); // Restore button on failure
                                Toast.makeText(LandlordMaintenanceRequestsActivity.this, "Failed to parse update response.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            });
        });

        dialog.show();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            if (rawDate.length() >= 10) {
                return rawDate.substring(0, 10);
            }
        } catch (Exception ignored) {}
        return rawDate;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
