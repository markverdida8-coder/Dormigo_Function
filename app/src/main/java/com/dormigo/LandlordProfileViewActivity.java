package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordProfileViewActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private int landlordId = 0;
    private int houseId = 0;
    private String landlordName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_profile_view);

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
        loadLandlordData();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        landlordId = intent.getIntExtra("LANDLORD_ID", 0);
        houseId = intent.getIntExtra("HOUSE_ID", 0);
        landlordName = intent.getStringExtra("LANDLORD_NAME");
    }

    private void setupUI() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvName = findViewById(R.id.textName);
        if (tvName != null && landlordName != null && !landlordName.isEmpty()) {
            tvName.setText(landlordName);
            TextView tvAvatar = findViewById(R.id.textAvatar);
            if (tvAvatar != null) tvAvatar.setText(getInitials(landlordName));
        }

        TextView btnMessage = findViewById(R.id.btnMessage);
        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> finish());
        }
    }

    private void loadLandlordData() {
        if (landlordId <= 0) return;

        apiClient.getUserById(landlordId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONObject u = json.optJSONObject("data");
                        if (u != null) {
                            String name = u.optString("full_name", landlordName);
                            String phone = u.optString("phone", "");
                            String createdAt = u.optString("created_at", "");

                            runOnUiThread(() -> {
                                TextView tvName = findViewById(R.id.textName);
                                TextView tvAvatar = findViewById(R.id.textAvatar);
                                TextView tvPhone = findViewById(R.id.textDetailPhone);
                                TextView tvMember = findViewById(R.id.textDetailMemberSince);

                                if (tvName != null && !name.isEmpty()) tvName.setText(name);
                                if (tvAvatar != null && !name.isEmpty()) tvAvatar.setText(getInitials(name));
                                if (tvPhone != null && !phone.isEmpty()) tvPhone.setText("📞 Phone: " + (!phone.isEmpty() ? phone : "Contact Available via Chat"));
                                if (tvMember != null && !createdAt.isEmpty()) tvMember.setText("📅 Member Since: " + formatMemberSince(createdAt));
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        if (houseId > 0) {
            apiClient.getReviewsForHouse(houseId, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) return;
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            double avg = json.optDouble("average_rating", 0.0);
                            int count = json.optInt("review_count", 0);
                            runOnUiThread(() -> {
                                TextView tvRating = findViewById(R.id.textStatRating);
                                TextView tvReviews = findViewById(R.id.textStatReviews);
                                if (tvRating != null) tvRating.setText("⭐ " + String.format(Locale.US, "%.1f", avg));
                                if (tvReviews != null) tvReviews.setText("📝 " + count);
                            });
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "L";
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
