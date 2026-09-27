package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout requestsContainer;
    private TextView tabPending, tabVerified, tabRejected;
    private TextView btnTypeProperties, btnTypeStudents;
    private String currentStatus = "PENDING";
    private String currentType = "PROPERTIES"; // PROPERTIES or STUDENTS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindViews();
        setupTypeSelector();
        setupTabs();
        setupLogout();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadVerifications);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVerifications();
    }

    private void bindViews() {
        requestsContainer = findViewById(R.id.requestsContainer);
        tabPending = findViewById(R.id.tabPending);
        tabVerified = findViewById(R.id.tabVerified);
        tabRejected = findViewById(R.id.tabRejected);
        btnTypeProperties = findViewById(R.id.btnTypeProperties);
        btnTypeStudents = findViewById(R.id.btnTypeStudents);
    }

    private void setupTypeSelector() {
        btnTypeProperties.setOnClickListener(v -> selectType("PROPERTIES"));
        btnTypeStudents.setOnClickListener(v -> selectType("STUDENTS"));
    }

    private void selectType(String type) {
        currentType = type;
        updateTypeStyles();
        loadVerifications();
    }

    private void updateTypeStyles() {
        boolean isProps = currentType.equals("PROPERTIES");
        btnTypeProperties.setBackgroundResource(isProps ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        btnTypeProperties.setTextColor(isProps ? Color.WHITE : Color.parseColor("#1A1A1A"));

        btnTypeStudents.setBackgroundResource(!isProps ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        btnTypeStudents.setTextColor(!isProps ? Color.WHITE : Color.parseColor("#1A1A1A"));
    }

    private void setupTabs() {
        tabPending.setOnClickListener(v -> selectTab("PENDING"));
        tabVerified.setOnClickListener(v -> selectTab("VERIFIED"));
        tabRejected.setOnClickListener(v -> selectTab("REJECTED"));
    }

    private void selectTab(String status) {
        currentStatus = status;
        updateTabStyles();
        loadVerifications();
    }

    private void updateTabStyles() {
        tabPending.setBackgroundResource(currentStatus.equals("PENDING") ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        tabPending.setTextColor(currentStatus.equals("PENDING") ? Color.WHITE : Color.parseColor("#1A1A1A"));

        tabVerified.setBackgroundResource(currentStatus.equals("VERIFIED") ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        tabVerified.setTextColor(currentStatus.equals("VERIFIED") ? Color.WHITE : Color.parseColor("#1A1A1A"));

        tabRejected.setBackgroundResource(currentStatus.equals("REJECTED") ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        tabRejected.setTextColor(currentStatus.equals("REJECTED") ? Color.WHITE : Color.parseColor("#1A1A1A"));
    }

    private void setupLogout() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences.Editor editor = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadVerifications() {
        String endpoint = currentType.equals("PROPERTIES") ? "get_verifications.php" : "get_student_verifications.php";
        String url = "http://10.129.224.109/Dormigo_Backend/api/" + endpoint + "?status=" + currentStatus;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        runOnUiThread(() -> {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            renderRequests(data != null ? data : new JSONArray());
                        });
                    }
                } catch (Exception ignored) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void renderRequests(JSONArray requests) {
        if (requestsContainer == null) return;
        requestsContainer.removeAllViews();

        if (requests.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No " + currentStatus.toLowerCase() + " " + (currentType.equals("PROPERTIES") ? "boarding house" : "student") + " verification requests.");
            empty.setTextColor(Color.parseColor("#6E6E73"));
            empty.setTextSize(14);
            empty.setPadding(dp(16), dp(32), dp(16), dp(32));
            requestsContainer.addView(empty);
            return;
        }

        for (int i = 0; i < requests.length(); i++) {
            try {
                JSONObject r = requests.getJSONObject(i);
                int verificationId = r.optInt("verification_id", 0);
                String status = r.optString("verification_status", "PENDING");
                String reason = r.optString("rejection_reason", "");

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

                if (currentType.equals("PROPERTIES")) {
                    int houseId = r.optInt("house_id", 0);
                    String houseName = r.optString("house_name", "Boarding House");
                    String address = r.optString("address", "");
                    String landlordName = r.optString("landlord_name", "Landlord");

                    TextView tvHouse = new TextView(this);
                    tvHouse.setText(houseName);
                    tvHouse.setTextColor(Color.parseColor("#1A1A1A"));
                    tvHouse.setTextSize(16);
                    tvHouse.setTypeface(null, Typeface.BOLD);

                    TextView tvLandlord = new TextView(this);
                    tvLandlord.setText("Landlord: " + landlordName);
                    tvLandlord.setTextColor(Color.parseColor("#6E6E73"));
                    tvLandlord.setTextSize(13);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    lp.topMargin = dp(4);
                    tvLandlord.setLayoutParams(lp);

                    TextView tvAddress = new TextView(this);
                    tvAddress.setText(address);
                    tvAddress.setTextColor(Color.parseColor("#6E6E73"));
                    tvAddress.setTextSize(12);
                    tvAddress.setLayoutParams(lp);

                    card.addView(tvHouse);
                    card.addView(tvLandlord);
                    card.addView(tvAddress);

                    if (status.equals("REJECTED") && !reason.isEmpty()) {
                        TextView tvReason = new TextView(this);
                        tvReason.setText("Reason: " + reason);
                        tvReason.setTextColor(Color.parseColor("#D32F2F"));
                        tvReason.setTextSize(12);
                        tvReason.setLayoutParams(lp);
                        card.addView(tvReason);
                    }

                    card.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AdminVerificationDetailActivity.class);
                        intent.putExtra("VERIFICATION_ID", verificationId);
                        intent.putExtra("HOUSE_ID", houseId);
                        intent.putExtra("HOUSE_NAME", houseName);
                        intent.putExtra("HOUSE_ADDRESS", address);
                        intent.putExtra("LANDLORD_NAME", landlordName);
                        intent.putExtra("LANDLORD_EMAIL", r.optString("landlord_email", ""));
                        intent.putExtra("LANDLORD_PHONE", r.optString("landlord_phone", ""));
                        intent.putExtra("VALID_ID_PATH", r.optString("valid_id_path", ""));
                        intent.putExtra("PROOF_PATH", r.optString("proof_document_path", ""));
                        intent.putExtra("STATUS", status);
                        startActivity(intent);
                    });

                } else {
                    String studentName = r.optString("full_name", "Student");
                    String email = r.optString("email", "");
                    String phone = r.optString("phone", "");

                    TextView tvStudent = new TextView(this);
                    tvStudent.setText(studentName);
                    tvStudent.setTextColor(Color.parseColor("#1A1A1A"));
                    tvStudent.setTextSize(16);
                    tvStudent.setTypeface(null, Typeface.BOLD);

                    TextView tvContact = new TextView(this);
                    tvContact.setText("Email: " + email + " | Phone: " + phone);
                    tvContact.setTextColor(Color.parseColor("#6E6E73"));
                    tvContact.setTextSize(13);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    lp.topMargin = dp(4);
                    tvContact.setLayoutParams(lp);

                    card.addView(tvStudent);
                    card.addView(tvContact);

                    if (status.equals("REJECTED") && !reason.isEmpty()) {
                        TextView tvReason = new TextView(this);
                        tvReason.setText("Reason: " + reason);
                        tvReason.setTextColor(Color.parseColor("#D32F2F"));
                        tvReason.setTextSize(12);
                        tvReason.setLayoutParams(lp);
                        card.addView(tvReason);
                    }

                    card.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AdminStudentVerificationDetailActivity.class);
                        intent.putExtra("VERIFICATION_ID", verificationId);
                        intent.putExtra("STUDENT_NAME", studentName);
                        intent.putExtra("STUDENT_EMAIL", email);
                        intent.putExtra("STUDENT_PHONE", phone);
                        intent.putExtra("STUDENT_ID_PATH", r.optString("student_id_path", ""));
                        intent.putExtra("STATUS", status);
                        startActivity(intent);
                    });
                }

                requestsContainer.addView(card);
            } catch (Exception ignored) {}
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
