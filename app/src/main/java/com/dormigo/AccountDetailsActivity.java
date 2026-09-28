package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AccountDetailsActivity extends AppCompatActivity {

    private TextView accountInitials;
    private TextView accountFullName;
    private TextView accountContactNumber;
    private TextView accountEmail;
    private TextView accountSchool;
    private TextView btnEditProfile;

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_details);

        apiClient = new ApiClient();

        // Adjust for system bars
        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        accountInitials = findViewById(R.id.accountInitials);
        accountFullName = findViewById(R.id.accountFullName);
        accountContactNumber = findViewById(R.id.accountContactNumber);
        accountEmail = findViewById(R.id.accountEmail);
        accountSchool = findViewById(R.id.accountSchool);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        setupUI();
        loadAccountData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccountData();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> openEditProfileScreen());
        }

        View btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
        }
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone and will remove all your data.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> executeDeleteAccount())
                .show();
    }

    private void executeDeleteAccount() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiClient.deleteUser(userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AccountDetailsActivity.this, "Failed to delete account.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    try {
                        String body = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(body);
                        if (response.isSuccessful() && json.optBoolean("success", false)) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();

                            Toast.makeText(AccountDetailsActivity.this, "Account deleted successfully.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(AccountDetailsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(AccountDetailsActivity.this, json.optString("message", "Failed to delete account."), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(AccountDetailsActivity.this, "Error deleting account.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void openEditProfileScreen() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void loadAccountData() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        boolean isStudent = prefs.getBoolean("isStudent", true);
        if (!isStudent) {
            View layoutSchool = findViewById(R.id.layoutSchool);
            View dividerSchool = findViewById(R.id.dividerSchool);

            if (layoutSchool != null) layoutSchool.setVisibility(View.GONE);
            if (dividerSchool != null) dividerSchool.setVisibility(View.GONE);
        }

        String name = prefs.getString("fullName", "User");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        String school = prefs.getString("school", "Southwestern University Phinma");
        int userId = prefs.getInt("userId", -1);

        displayInfo(name, email, phone, school);

        if (userId > 0) {
            apiClient.getUserById(userId, new Callback() {
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
                                String uName = u.optString("full_name", name);
                                String uEmail = u.optString("email", email);
                                String uPhone = u.optString("phone", phone);

                                SharedPreferences.Editor ed = prefs.edit();
                                ed.putString("fullName", uName);
                                ed.putString("email", uEmail);
                                ed.putString("phone", uPhone);
                                ed.apply();

                                runOnUiThread(() -> displayInfo(uName, uEmail, uPhone, school));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private void displayInfo(String name, String email, String phone, String school) {
        if (accountInitials != null) {
            accountInitials.setText(getInitials(name));
        }
        if (accountFullName != null) {
            accountFullName.setText(name);
        }
        if (accountContactNumber != null) {
            accountContactNumber.setText(phone.isEmpty() ? "None" : phone);
        }
        if (accountEmail != null) {
            accountEmail.setText(email.isEmpty() ? "None" : email);
        }
        if (accountSchool != null) {
            accountSchool.setText(school.isEmpty() ? "None" : school);
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
}