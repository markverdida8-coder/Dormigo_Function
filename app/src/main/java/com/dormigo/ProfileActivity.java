package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_settings);

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

        setupBottomNavigation();
        setupClickListeners();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        TextView userNameText = findViewById(R.id.userNameText);
        TextView userAvatarText = findViewById(R.id.userAvatarText);
        TextView userSubText = findViewById(R.id.userSubText);

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        String cachedName = prefs.getString("fullName", "User");
        String cachedEmail = prefs.getString("email", "");
        int userId = prefs.getInt("userId", -1);

        if (userNameText != null) {
            userNameText.setText(cachedName);
        }
        if (userAvatarText != null) {
            userAvatarText.setText(getInitials(cachedName));
        }
        if (userSubText != null) {
            userSubText.setText(!cachedEmail.isEmpty() ? cachedEmail : "Student Account");
        }

        if (userId > 0) {
            loadStudentVerificationStatus(userId);

            new ApiClient().getUserById(userId, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) return;
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            JSONObject u = json.optJSONObject("data");
                            if (u != null) {
                                String name = u.optString("full_name", cachedName);
                                String email = u.optString("email", cachedEmail);
                                String phone = u.optString("phone", "");

                                SharedPreferences.Editor ed = prefs.edit();
                                ed.putString("fullName", name);
                                ed.putString("email", email);
                                ed.putString("phone", phone);
                                ed.apply();

                                runOnUiThread(() -> {
                                    if (userNameText != null) userNameText.setText(name);
                                    if (userAvatarText != null) userAvatarText.setText(getInitials(name));
                                    if (userSubText != null) userSubText.setText(!email.isEmpty() ? email : "Student Account");
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void loadStudentVerificationStatus(int userId) {
        TextView tvStatus = findViewById(R.id.textVerificationStatus);
        if (tvStatus == null) return;

        String url = "http://10.129.224.109/Dormigo_Backend/api/get_student_verification_status.php?user_id=" + userId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            String status = data.optString("verification_status", "NOT_SUBMITTED");
                            runOnUiThread(() -> {
                                if (status.equals("VERIFIED")) {
                                    tvStatus.setText("✓ Verified");
                                    tvStatus.setTextColor(0xFF2E7D32);
                                } else if (status.equals("PENDING")) {
                                    tvStatus.setText("Pending Verification");
                                    tvStatus.setTextColor(0xFF8A6D0B);
                                } else if (status.equals("REJECTED")) {
                                    tvStatus.setText("Rejected - Tap to Resubmit");
                                    tvStatus.setTextColor(0xFFD32F2F);
                                } else {
                                    tvStatus.setText("Not Verified");
                                    tvStatus.setTextColor(0xFF9A9A9E);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_explore) {
                Intent intent = new Intent(ProfileActivity.this, BoardingHouseListingsActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_chats) {
                Intent intent = new Intent(this, ChatHistoryActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_requests) {
                Intent intent = new Intent(this, BookingRequestsActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    @SuppressWarnings("deprecation")
    private void setupClickListeners() {
        LinearLayout btnTransactionHistory = findViewById(R.id.btnTransactionHistory);
        LinearLayout btnNotifications = findViewById(R.id.btnNotifications);
        LinearLayout btnAccountDetails = findViewById(R.id.btnAccountDetails);
        LinearLayout btnVerification = findViewById(R.id.btnVerification);
        LinearLayout btnMyReviews = findViewById(R.id.btnMyReviews);
        LinearLayout btnSignOut = findViewById(R.id.btnSignOut);

        if (btnTransactionHistory != null) {
            btnTransactionHistory.setOnClickListener(v -> {
                Intent intent = new Intent(this, TransactionHistoryActivity.class);
                startActivity(intent);
            });
        }
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
        if (btnAccountDetails != null) {
            btnAccountDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, AccountDetailsActivity.class);
                startActivity(intent);
            });
        }
        if (btnVerification != null) {
            btnVerification.setOnClickListener(v -> {
                Intent intent = new Intent(this, VerificationActivity.class);
                startActivity(intent);
            });
        }
        if (btnMyReviews != null) {
            btnMyReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, MyReviewsActivity.class);
                startActivity(intent);
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                // Clear login state
                SharedPreferences.Editor editor = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();

                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}