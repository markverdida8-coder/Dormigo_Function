package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TenantProfileViewActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private int studentId = 0;
    private String studentName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tenant_profile_view);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        readIntentData();
        setupUI();
        loadTenantData();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        studentId = intent.getIntExtra("STUDENT_ID", 0);
        String name = intent.getStringExtra("STUDENT_NAME");
        if (name != null && !name.trim().isEmpty()) {
            studentName = name.trim();
        }
    }

    private void setupUI() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvName = findViewById(R.id.textName);
        if (tvName != null && studentName != null && !studentName.isEmpty()) {
            tvName.setText(studentName);
            TextView tvAvatar = findViewById(R.id.textAvatar);
            if (tvAvatar != null) tvAvatar.setText(getInitials(studentName));
        }

        TextView btnMessage = findViewById(R.id.btnMessage);
        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> finish());
        }
    }

    private void loadTenantData() {
        if (studentId <= 0) return;

        apiClient.getUserById(studentId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONObject userObj = json.optJSONObject("data");
                        if (userObj == null) {
                            JSONArray arr = json.optJSONArray("data");
                            if (arr != null && arr.length() > 0) {
                                userObj = arr.optJSONObject(0);
                            }
                        }
                        if (userObj != null) {
                            String name = userObj.optString("full_name", studentName);
                            String phone = userObj.optString("phone", "");
                            String email = userObj.optString("email", "");
                            String createdAt = userObj.optString("created_at", "");

                            runOnUiThread(() -> {
                                TextView tvName = findViewById(R.id.textName);
                                TextView tvAvatar = findViewById(R.id.textAvatar);
                                TextView tvPhone = findViewById(R.id.textDetailPhone);
                                TextView tvEmail = findViewById(R.id.textDetailEmail);
                                TextView tvMember = findViewById(R.id.textDetailMemberSince);

                                if (tvName != null && !name.isEmpty()) tvName.setText(name);
                                if (tvAvatar != null && !name.isEmpty()) tvAvatar.setText(getInitials(name));
                                if (tvPhone != null) {
                                    if (!phone.trim().isEmpty() && !"null".equalsIgnoreCase(phone.trim())) {
                                        tvPhone.setText("📞 Phone: " + phone.trim());
                                    } else {
                                        tvPhone.setText("📞 Phone: Contact Available via Chat");
                                    }
                                }
                                if (tvEmail != null) {
                                    if (!email.trim().isEmpty() && !"null".equalsIgnoreCase(email.trim())) {
                                        tvEmail.setText("✉️ Email: " + email.trim());
                                    } else {
                                        tvEmail.setText("✉️ Email: Contact Available via Chat");
                                    }
                                }
                                if (tvMember != null && !createdAt.isEmpty()) tvMember.setText("📅 Member Since: " + formatMemberSince(createdAt));
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        apiClient.getBookingsForStudent(studentId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null && data.length() > 0) {
                            JSONObject b = data.optJSONObject(0);
                            if (b != null) {
                                String status = b.optString("status", "ACTIVE").toUpperCase(Locale.ROOT);
                                String houseName = b.optString("house_name", "Boarding House");
                                String roomNumber = b.optString("room_number", "");
                                String moveInDate = b.optString("move_in_date", "2026-09-01");
                                int duration = b.optInt("duration_months", 3);

                                String statusLabel;
                                if ("ACTIVE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                                    statusLabel = "Active Tenant";
                                } else if ("APPROVED".equalsIgnoreCase(status)) {
                                    statusLabel = "Approved";
                                } else if ("PENDING".equalsIgnoreCase(status)) {
                                    statusLabel = "Pending Request";
                                } else {
                                    statusLabel = status;
                                }

                                final String finalStatusLabel = statusLabel;
                                final String finalHouseRoom = houseName + (roomNumber.isEmpty() ? "" : " · Room " + roomNumber);
                                final String finalMoveIn = moveInDate;
                                final int finalDuration = duration;

                                runOnUiThread(() -> {
                                    TextView tvStatus = findViewById(R.id.textDetailBookingStatus);
                                    TextView tvHouseRoom = findViewById(R.id.textDetailHouseRoom);
                                    TextView tvMoveIn = findViewById(R.id.textDetailMoveInDate);
                                    TextView tvDuration = findViewById(R.id.textStatDuration);
                                    TextView tvBookingsCount = findViewById(R.id.textStatBookings);

                                    if (tvStatus != null) tvStatus.setText("📌 Booking Status: " + finalStatusLabel);
                                    if (tvHouseRoom != null) tvHouseRoom.setText("🏠 Property: " + finalHouseRoom);
                                    if (tvMoveIn != null) tvMoveIn.setText("🗓 Move-in Date: " + finalMoveIn);
                                    if (tvDuration != null) tvDuration.setText("📅 " + finalDuration + " Mos");
                                    if (tvBookingsCount != null) tvBookingsCount.setText("📋 " + data.length());
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.US);
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase(Locale.US);
        }
        return parts[0].toUpperCase(Locale.US);
    }

    private String formatMemberSince(String createdAt) {
        if (createdAt == null || createdAt.length() < 7) return "September 2026";
        try {
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM yyyy", Locale.US);
            Date date = sdf1.parse(createdAt);
            if (date != null) return sdf2.format(date);
        } catch (Exception ignored) {}
        return "September 2026";
    }
}
