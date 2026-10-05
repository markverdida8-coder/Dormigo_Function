package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
            loadLandlordVerificationStatus(landlordId);

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

    private void loadLandlordVerificationStatus(int landlordId) {
        TextView tvStatus = findViewById(R.id.textVerificationStatus);
        View btnVerification = findViewById(R.id.btnVerification);
        if (tvStatus == null) return;

        String url = "http://10.209.52.109/Dormigo_Backend/api/get_landlord_account_verification_status.php?landlord_id=" + landlordId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            String status = data.optString("verification_status", "NOT_SUBMITTED").toUpperCase(Locale.ROOT);
                            runOnUiThread(() -> {
                                if ("VERIFIED".equals(status) || "APPROVED".equals(status)) {
                                    tvStatus.setText("🟢 Verified Landlord");
                                    tvStatus.setTextColor(0xFF2E7D32);
                                    if (btnVerification != null) {
                                        btnVerification.setOnClickListener(null);
                                        btnVerification.setClickable(false);
                                        btnVerification.setFocusable(false);
                                        btnVerification.setBackground(null);
                                    }
                                } else if ("PENDING".equals(status)) {
                                    tvStatus.setText("🟡 Verification Pending");
                                    tvStatus.setTextColor(0xFF8A6D0B);
                                    if (btnVerification != null) {
                                        btnVerification.setOnClickListener(null);
                                        btnVerification.setClickable(false);
                                        btnVerification.setFocusable(false);
                                        btnVerification.setBackground(null);
                                    }
                                } else {
                                    tvStatus.setText("🔴 Not Verified - Tap to Verify");
                                    tvStatus.setTextColor(0xFFD32F2F);
                                    if (btnVerification != null) {
                                        btnVerification.setClickable(true);
                                        btnVerification.setFocusable(true);
                                        btnVerification.setBackgroundResource(R.drawable.bg_card_rounded);
                                        btnVerification.setOnClickListener(v -> {
                                            Intent intent = new Intent(LandlordProfileActivity.this, VerificationActivity.class);
                                            startActivity(intent);
                                        });
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void openPaymentMethodsSelection() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int landlordId = prefs.getInt("userId", -1);
        if (landlordId <= 0) return;

        new ApiClient().getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LandlordProfileActivity.this, "Unable to load properties.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray houses = json.optJSONArray("data");
                        JSONArray myHouses = new JSONArray();
                        if (houses != null) {
                            for (int i = 0; i < houses.length(); i++) {
                                JSONObject h = houses.optJSONObject(i);
                                if (h != null && h.optInt("landlord_id", 0) == landlordId) {
                                    myHouses.put(h);
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            if (myHouses.length() == 0) {
                                Toast.makeText(LandlordProfileActivity.this, "You don't have any boarding houses yet. Add a boarding house before configuring payment methods.", Toast.LENGTH_LONG).show();
                            } else if (myHouses.length() == 1) {
                                JSONObject h = myHouses.optJSONObject(0);
                                int houseId = h.optInt("house_id", 0);
                                String houseName = h.optString("house_name", "Boarding House");
                                Intent intent = new Intent(LandlordProfileActivity.this, LandlordPaymentMethodsActivity.class);
                                intent.putExtra("HOUSE_ID", houseId);
                                intent.putExtra("HOUSE_NAME", houseName);
                                startActivity(intent);
                            } else {
                                showPropertySelectionBottomSheet(myHouses);
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void showPropertySelectionBottomSheet(JSONArray houses) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(24));
        layout.setBackgroundResource(R.drawable.bg_bottom_sheet);

        TextView title = new TextView(this);
        title.setText("Choose a boarding house");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFF1A1A1A);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Select a property to configure its payment methods");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF6E6E73);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        layout.addView(subtitle);

        for (int i = 0; i < houses.length(); i++) {
            JSONObject h = houses.optJSONObject(i);
            if (h == null) continue;
            int houseId = h.optInt("house_id", 0);
            String name = h.optString("house_name", "Boarding House");
            String address = h.optString("address", "Near Campus");

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(16), dp(12), dp(16), dp(12));
            item.setBackgroundResource(R.drawable.bg_card_rounded);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            item.setLayoutParams(lp);

            TextView tvName = new TextView(this);
            tvName.setText("🏠 " + name);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF1A1A1A);

            TextView tvAddr = new TextView(this);
            tvAddr.setText(address);
            tvAddr.setTextSize(12);
            tvAddr.setTextColor(0xFF6E6E73);
            tvAddr.setPadding(dp(20), dp(2), 0, 0);

            item.addView(tvName);
            item.addView(tvAddr);

            item.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, LandlordPaymentMethodsActivity.class);
                intent.putExtra("HOUSE_ID", houseId);
                intent.putExtra("HOUSE_NAME", name);
                startActivity(intent);
            });

            layout.addView(item);
        }

        dialog.setContentView(layout);
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
            btnPaymentMethods.setOnClickListener(v -> openPaymentMethodsSelection());
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
