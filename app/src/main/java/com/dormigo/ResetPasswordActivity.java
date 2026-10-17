package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

public class ResetPasswordActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private String email = "";
    private String otpCode = "";

    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private ImageView togglePasswordVisibility;
    private ImageView toggleConfirmPasswordVisibility;
    private TextView btnSavePassword;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Intent intent = getIntent();
        email = intent.getStringExtra("EMAIL");
        otpCode = intent.getStringExtra("OTP_CODE");
        if (email == null) email = "";
        if (otpCode == null) otpCode = "";

        bindViews();
        setupUI();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupUI() {
        if (togglePasswordVisibility != null) {
            togglePasswordVisibility.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                togglePassword(passwordInput, togglePasswordVisibility, isPasswordVisible);
            });
        }

        if (toggleConfirmPasswordVisibility != null) {
            toggleConfirmPasswordVisibility.setOnClickListener(v -> {
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                togglePassword(confirmPasswordInput, toggleConfirmPasswordVisibility, isConfirmPasswordVisible);
            });
        }

        if (btnSavePassword != null) {
            btnSavePassword.setOnClickListener(v -> validateAndResetPassword());
        }
    }

    private void togglePassword(EditText editText, ImageView icon, boolean visible) {
        if (editText == null || icon == null) return;
        int selection = editText.getSelectionStart();
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_off);
        }
        editText.setSelection(selection);
    }

    private void validateAndResetPassword() {
        String newPassword = passwordInput != null ? passwordInput.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordInput != null ? confirmPasswordInput.getText().toString().trim() : "";

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter a new password.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            Toast.makeText(this, "Password must contain at least one uppercase letter.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.matches(".*[a-z].*")) {
            Toast.makeText(this, "Password must contain at least one lowercase letter.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.matches(".*[0-9].*")) {
            Toast.makeText(this, "Password must contain at least one number.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePassword.setEnabled(false);
        apiClient.resetPassword(email, otpCode, newPassword, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    btnSavePassword.setEnabled(true);
                    Toast.makeText(ResetPasswordActivity.this, "Network error resetting password.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    boolean success = json.optBoolean("success", false);
                    String message = json.optString("message", "Reset failed.");

                    runOnUiThread(() -> {
                        btnSavePassword.setEnabled(true);
                        if (success) {
                            new AlertDialog.Builder(ResetPasswordActivity.this)
                                    .setTitle("Password Reset Successful")
                                    .setMessage("Your password has been successfully reset. You can now log in with your new password.")
                                    .setPositiveButton("Sign In", (dialog, which) -> {
                                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnSavePassword.setEnabled(true);
                        Toast.makeText(ResetPasswordActivity.this, "Password reset failed.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
