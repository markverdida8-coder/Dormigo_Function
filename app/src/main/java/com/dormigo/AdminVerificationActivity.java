package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminVerificationActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView statPendingTotal, statApprovedTotal, statRejectedTotal, statAllTotal;
    private TextView tabStudents, tabLandlords;
    private TextView chipFilterAll, chipFilterPending, chipFilterApproved, chipFilterRejected;
    private EditText searchVerificationsInput;
    private LinearLayout verificationsListContainer;

    private boolean isStudentTab = true; // true for Students, false for Landlords
    private String currentStatusFilter = "PENDING"; // ALL, PENDING, APPROVED, REJECTED
    private String searchQuery = "";

    private JSONArray rawStudentData = new JSONArray();
    private JSONArray rawLandlordData = new JSONArray();

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadVerificationsData();
            autoRefreshHandler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_verification);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindViews();
        setupListeners();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadVerificationsData);
        }

        loadVerificationsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVerificationsData();
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private void bindViews() {
        statPendingTotal = findViewById(R.id.statPendingTotal);
        statApprovedTotal = findViewById(R.id.statApprovedTotal);
        statRejectedTotal = findViewById(R.id.statRejectedTotal);
        statAllTotal = findViewById(R.id.statAllTotal);

        tabStudents = findViewById(R.id.tabStudents);
        tabLandlords = findViewById(R.id.tabLandlords);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterPending = findViewById(R.id.chipFilterPending);
        chipFilterApproved = findViewById(R.id.chipFilterApproved);
        chipFilterRejected = findViewById(R.id.chipFilterRejected);

        searchVerificationsInput = findViewById(R.id.searchVerificationsInput);
        verificationsListContainer = findViewById(R.id.verificationsListContainer);
    }

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tabStudents != null) tabStudents.setOnClickListener(v -> selectRoleTab(true));
        if (tabLandlords != null) tabLandlords.setOnClickListener(v -> selectRoleTab(false));

        if (chipFilterAll != null) chipFilterAll.setOnClickListener(v -> selectFilterChip("ALL"));
        if (chipFilterPending != null) chipFilterPending.setOnClickListener(v -> selectFilterChip("PENDING"));
        if (chipFilterApproved != null) chipFilterApproved.setOnClickListener(v -> selectFilterChip("APPROVED"));
        if (chipFilterRejected != null) chipFilterRejected.setOnClickListener(v -> selectFilterChip("REJECTED"));

        if (searchVerificationsInput != null) {
            searchVerificationsInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    renderList();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void selectRoleTab(boolean student) {
        isStudentTab = student;
        if (tabStudents != null) {
            tabStudents.setBackgroundResource(student ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
            tabStudents.setTextColor(student ? Color.WHITE : Color.parseColor("#1A1A1A"));
        }
        if (tabLandlords != null) {
            tabLandlords.setBackgroundResource(!student ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
            tabLandlords.setTextColor(!student ? Color.WHITE : Color.parseColor("#1A1A1A"));
        }
        renderList();
    }

    private void selectFilterChip(String filter) {
        currentStatusFilter = filter;
        updateChipStyles();
        renderList();
    }

    private void updateChipStyles() {
        setChipStyle(chipFilterAll, "ALL".equals(currentStatusFilter));
        setChipStyle(chipFilterPending, "PENDING".equals(currentStatusFilter));
        setChipStyle(chipFilterApproved, "APPROVED".equals(currentStatusFilter));
        setChipStyle(chipFilterRejected, "REJECTED".equals(currentStatusFilter));
    }

    private void setChipStyle(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setBackgroundResource(active ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        chip.setTextColor(active ? Color.WHITE : Color.parseColor("#1A1A1A"));
    }

    private void loadVerificationsData() {
        String url = "http://10.209.52.109/Dormigo_Backend/api/get_admin_dashboard_stats.php";
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(AdminVerificationActivity.this, "Failed to load verifications data.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            rawStudentData = data.optJSONArray("recent_student_verifications");
                            rawLandlordData = data.optJSONArray("recent_landlord_verifications");

                            int pStudents = data.optInt("pending_students", 0);
                            int pLandlords = data.optInt("pending_landlords", 0);
                            int vStudents = data.optInt("verified_students", 0);
                            int vLandlords = data.optInt("verified_landlords", 0);
                            int rStudents = data.optInt("rejected_students", 0);
                            int rLandlords = data.optInt("rejected_landlords", 0);

                            final int totalPending = pStudents + pLandlords;
                            final int totalApproved = vStudents + vLandlords;
                            final int totalRejected = rStudents + rLandlords;
                            final int totalAll = totalPending + totalApproved + totalRejected;

                            runOnUiThread(() -> {
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                                if (statPendingTotal != null) statPendingTotal.setText(String.valueOf(totalPending));
                                if (statApprovedTotal != null) statApprovedTotal.setText(String.valueOf(totalApproved));
                                if (statRejectedTotal != null) statRejectedTotal.setText(String.valueOf(totalRejected));
                                if (statAllTotal != null) statAllTotal.setText(String.valueOf(totalAll));

                                renderList();
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void renderList() {
        if (verificationsListContainer == null) return;
        verificationsListContainer.removeAllViews();

        JSONArray source = isStudentTab ? rawStudentData : rawLandlordData;
        if (source == null || source.length() == 0) {
            verificationsListContainer.addView(createEmptyState("No verification requests found."));
            return;
        }

        int count = 0;
        for (int i = 0; i < source.length(); i++) {
            JSONObject obj = source.optJSONObject(i);
            if (obj != null) {
                String name = obj.optString("full_name", "");
                String email = obj.optString("email", "");
                String status = obj.optString("verification_status", "PENDING").toUpperCase();

                // Filter by status
                if (!"ALL".equals(currentStatusFilter)) {
                    if ("APPROVED".equals(currentStatusFilter) && !("VERIFIED".equals(status) || "APPROVED".equals(status))) continue;
                    if ("PENDING".equals(currentStatusFilter) && !"PENDING".equals(status)) continue;
                    if ("REJECTED".equals(currentStatusFilter) && !"REJECTED".equals(status)) continue;
                }

                // Filter by search query
                if (!searchQuery.isEmpty()) {
                    boolean matchName = name.toLowerCase().contains(searchQuery);
                    boolean matchEmail = email.toLowerCase().contains(searchQuery);
                    if (!matchName && !matchEmail) continue;
                }

                count++;
                verificationsListContainer.addView(createVerificationCard(obj, isStudentTab));
            }
        }

        if (count == 0) {
            verificationsListContainer.addView(createEmptyState("No matching verification requests."));
        }
    }

    private View createVerificationCard(JSONObject item, boolean isStudent) {
        int vId = item.optInt("verification_id", 0);
        String name = item.optString("full_name", isStudent ? "Student" : "Landlord");
        String email = item.optString("email", "");
        String phone = item.optString("phone", "");
        String date = item.optString("submitted_at", "");
        String status = item.optString("verification_status", "PENDING").toUpperCase();
        String docPath = isStudent ? item.optString("student_id_path", "") : item.optString("document_path", "");

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

        // Header Row (Avatar/Initials + Name & Email)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(getInitials(name));
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(16);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setBackgroundResource(R.drawable.bg_circle_green);

        LinearLayout textLayout = new LinearLayout(this);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textLp.setMarginStart(dp(12));
        textLayout.setLayoutParams(textLp);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvEmail = new TextView(this);
        tvEmail.setText(email);
        tvEmail.setTextColor(Color.parseColor("#6E6E73"));
        tvEmail.setTextSize(12);

        textLayout.addView(tvName);
        textLayout.addView(tvEmail);

        headerRow.addView(avatar);
        headerRow.addView(textLayout);

        // Status Chip
        TextView statusChip = new TextView(this);
        statusChip.setText("VERIFIED".equals(status) || "APPROVED".equals(status) ? "🟢 Verified" :
                ("REJECTED".equals(status) ? "🔴 Rejected" : "🟡 Pending"));
        statusChip.setTextColor("VERIFIED".equals(status) || "APPROVED".equals(status) ? Color.parseColor("#1B5E4C") :
                ("REJECTED".equals(status) ? Color.parseColor("#D32F2F") : Color.parseColor("#D97706")));
        statusChip.setTextSize(12);
        statusChip.setTypeface(null, Typeface.BOLD);

        headerRow.addView(statusChip);
        card.addView(headerRow);

        // Date Info
        if (!date.isEmpty()) {
            TextView tvDate = new TextView(this);
            tvDate.setText("Submitted: " + date);
            tvDate.setTextColor(Color.parseColor("#9A9A9E"));
            tvDate.setTextSize(11);
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dateLp.topMargin = dp(8);
            tvDate.setLayoutParams(dateLp);
            card.addView(tvDate);
        }

        // Action Buttons Row (View PDF / Student ID, Approve, Reject)
        LinearLayout actionsRow = new LinearLayout(this);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(12);
        actionsRow.setLayoutParams(actLp);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnViewDoc = new TextView(this);
        btnViewDoc.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1.2f));
        btnViewDoc.setGravity(Gravity.CENTER);
        btnViewDoc.setText(isStudent ? "📄 View Student ID" : "📄 View PDF");
        btnViewDoc.setTextColor(Color.parseColor("#1B5E4C"));
        btnViewDoc.setTextSize(12);
        btnViewDoc.setTypeface(null, Typeface.BOLD);
        btnViewDoc.setBackgroundResource(R.drawable.bg_chip_white);
        btnViewDoc.setOnClickListener(v -> {
            Intent intent = isStudent ? new Intent(this, AdminStudentVerificationDetailActivity.class) : new Intent(this, AdminVerificationDetailActivity.class);
            intent.putExtra("VERIFICATION_ID", vId);
            intent.putExtra("STUDENT_NAME", name);
            intent.putExtra("LANDLORD_NAME", name);
            intent.putExtra("STUDENT_EMAIL", email);
            intent.putExtra("LANDLORD_EMAIL", email);
            intent.putExtra("STUDENT_PHONE", phone);
            intent.putExtra("LANDLORD_PHONE", phone);
            intent.putExtra("STUDENT_ID_PATH", docPath);
            intent.putExtra("PROOF_PATH", docPath);
            intent.putExtra("STATUS", status);
            startActivity(intent);
        });

        actionsRow.addView(btnViewDoc);

        if ("PENDING".equalsIgnoreCase(status)) {
            TextView btnApprove = new TextView(this);
            LinearLayout.LayoutParams appLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            appLp.setMarginStart(dp(6));
            btnApprove.setLayoutParams(appLp);
            btnApprove.setGravity(Gravity.CENTER);
            btnApprove.setText("✅ Approve");
            btnApprove.setTextColor(Color.WHITE);
            btnApprove.setTextSize(12);
            btnApprove.setTypeface(null, Typeface.BOLD);
            btnApprove.setBackgroundResource(R.drawable.bg_button_filled);
            btnApprove.setOnClickListener(v -> executeApprove(vId, isStudent));

            TextView btnReject = new TextView(this);
            LinearLayout.LayoutParams rejLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            rejLp.setMarginStart(dp(6));
            btnReject.setLayoutParams(rejLp);
            btnReject.setGravity(Gravity.CENTER);
            btnReject.setText("❌ Reject");
            btnReject.setTextColor(Color.parseColor("#D32F2F"));
            btnReject.setTextSize(12);
            btnReject.setTypeface(null, Typeface.BOLD);
            btnReject.setBackgroundResource(R.drawable.bg_chip_white);
            btnReject.setOnClickListener(v -> showRejectDialog(vId, isStudent));

            actionsRow.addView(btnApprove);
            actionsRow.addView(btnReject);
        }

        card.addView(actionsRow);
        return card;
    }

    private void executeApprove(int verificationId, boolean isStudent) {
        int adminId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).getInt("userId", 3);
        String endpoint = isStudent ? "approve_student_verification.php" : "approve_verification.php";

        try {
            JSONObject json = new JSONObject();
            json.put("verification_id", verificationId);
            json.put("admin_id", adminId);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("http://10.209.52.109/Dormigo_Backend/api/" + endpoint)
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminVerificationActivity.this, "Failed to approve verification.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminVerificationActivity.this, "Verification Approved Successfully!", Toast.LENGTH_SHORT).show();
                        loadVerificationsData();
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRejectDialog(int verificationId, boolean isStudent) {
        EditText input = new EditText(this);
        input.setHint("Enter reason for rejection...");
        input.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("Reject Verification")
                .setView(input)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        executeReject(verificationId, isStudent, reason);
                    } else {
                        Toast.makeText(this, "Rejection reason is required.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeReject(int verificationId, boolean isStudent, String reason) {
        int adminId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).getInt("userId", 3);
        String endpoint = isStudent ? "reject_student_verification.php" : "reject_verification.php";

        try {
            JSONObject json = new JSONObject();
            json.put("verification_id", verificationId);
            json.put("admin_id", adminId);
            json.put("rejection_reason", reason);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("http://10.209.52.109/Dormigo_Backend/api/" + endpoint)
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminVerificationActivity.this, "Failed to reject verification.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminVerificationActivity.this, "Verification Rejected.", Toast.LENGTH_SHORT).show();
                        loadVerificationsData();
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TextView createEmptyState(String text) {
        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextColor(Color.parseColor("#9A9A9E"));
        empty.setTextSize(13);
        empty.setPadding(0, dp(24), 0, dp(24));
        empty.setGravity(Gravity.CENTER);
        return empty;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
