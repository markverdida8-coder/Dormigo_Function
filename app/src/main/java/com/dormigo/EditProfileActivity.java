package com.dormigo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText inputFullName;
    private EditText inputContactNumber;
    private EditText inputEmail;
    private EditText inputSchool;
    private TextView profileAvatarText;
    private View btnSubmitSave;
    private View btnSaveProfile;

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        apiClient = new ApiClient();

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindViews();
        setupUI();
        loadCurrentProfile();
    }

    private void bindViews() {
        inputFullName = findViewById(R.id.inputFullName);
        inputContactNumber = findViewById(R.id.inputContactNumber);
        inputEmail = findViewById(R.id.inputEmail);
        inputSchool = findViewById(R.id.inputSchool);
        profileAvatarText = findViewById(R.id.profileAvatarText);
        btnSubmitSave = findViewById(R.id.btnSubmitSave);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View.OnClickListener saveListener = v -> saveProfileChanges();

        if (btnSubmitSave != null) btnSubmitSave.setOnClickListener(saveListener);
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(saveListener);
    }

    private void loadCurrentProfile() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        String name = prefs.getString("fullName", "User");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        String school = prefs.getString("school", "Southwestern University Phinma");

        if (inputFullName != null) inputFullName.setText(name);
        if (inputContactNumber != null) inputContactNumber.setText(phone);
        if (inputEmail != null) inputEmail.setText(email);
        if (inputSchool != null) inputSchool.setText(school);

        if (profileAvatarText != null) {
            profileAvatarText.setText(getInitials(name));
        }
    }

    private void saveProfileChanges() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId <= 0) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = inputFullName != null ? inputFullName.getText().toString().trim() : "";
        String newPhone = inputContactNumber != null ? inputContactNumber.getText().toString().trim() : "";
        String newEmail = inputEmail != null ? inputEmail.getText().toString().trim() : "";
        String newSchool = inputSchool != null ? inputSchool.getText().toString().trim() : "";

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Name and Email cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiClient.updateUser(userId, newName, newEmail, newPhone, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Unable to update profile.", Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putString("fullName", newName);
                        ed.putString("email", newEmail);
                        ed.putString("phone", newPhone);
                        ed.putString("school", newSchool);
                        ed.apply();

                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        String msg = json.optString("message", "Unable to update profile.");
                        runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Update error.", Toast.LENGTH_SHORT).show());
                }
            }
        });
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