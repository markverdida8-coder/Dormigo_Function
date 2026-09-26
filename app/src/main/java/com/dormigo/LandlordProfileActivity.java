package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_profile);

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

        setupUI();
        setupBottomNavigation();
        loadLandlordProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLandlordProfile();
    }

    private void loadLandlordProfile() {
        TextView landlordNameText = findViewById(R.id.landlordNameText);
        TextView landlordAvatarText = findViewById(R.id.landlordAvatarText);
        TextView landlordSubText = findViewById(R.id.landlordSubText);

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        String cachedName = prefs.getString("fullName", "Landlord");
        String cachedEmail = prefs.getString("email", "");
        int landlordId = prefs.getInt("userId", -1);

        if (landlordNameText != null) {
            landlordNameText.setText(cachedName);
        }
        if (landlordAvatarText != null) {
            landlordAvatarText.setText(getInitials(cachedName));
        }
        if (landlordSubText != null) {
            landlordSubText.setText(!cachedEmail.isEmpty() ? cachedEmail : "Landlord Account");
        }

        if (landlordId > 0) {
            new ApiClient().getUserById(landlordId, new Callback() {
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
                                    if (landlordNameText != null) landlordNameText.setText(name);
                                    if (landlordAvatarText != null) landlordAvatarText.setText(getInitials(name));
                                    if (landlordSubText != null) landlordSubText.setText(!email.isEmpty() ? email : "Landlord Account");
                                });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "L";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void setupUI() {
        LinearLayout btnAccountDetails = findViewById(R.id.btnAccountDetails);
        LinearLayout btnManageProperties = findViewById(R.id.btnManageProperties);
        LinearLayout btnTenants = findViewById(R.id.btnTenants);
        LinearLayout btnPendingRequests = findViewById(R.id.btnPendingRequests);
        LinearLayout btnPaymentMethods = findViewById(R.id.btnPaymentMethods);
        LinearLayout btnTransactionHistory = findViewById(R.id.btnTransactionHistory);
        LinearLayout btnSignOut = findViewById(R.id.btnSignOut);

        if (btnAccountDetails != null) {
            btnAccountDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, AccountDetailsActivity.class);
                startActivity(intent);
            });
        }

        if (btnManageProperties != null) {
            btnManageProperties.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordPropertiesActivity.class);
                startActivity(intent);
            });
        }

        if (btnTenants != null) {
            btnTenants.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordTenantsActivity.class);
                startActivity(intent);
            });
        }

        if (btnPendingRequests != null) {
            btnPendingRequests.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordRequestsActivity.class);
                startActivity(intent);
            });
        }

        if (btnPaymentMethods != null) {
            btnPaymentMethods.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordPaymentMethodsActivity.class);
                startActivity(intent);
            });
        }

        if (btnTransactionHistory != null) {
            btnTransactionHistory.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordTransactionHistoryActivity.class);
                startActivity(intent);
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                SharedPreferences.Editor editor = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_landlord_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_landlord_home) {
                startActivity(new Intent(this, LandlordHomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_landlord_add_house) {
                startActivity(new Intent(this, AddBoardingHouseActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_landlord_chats) {
                startActivity(new Intent(this, LandlordChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_landlord_profile) {
                return true;
            }
            return false;
        });
    }
}
