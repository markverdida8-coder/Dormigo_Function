package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.forgot_password_page);

        // Adjust for system bars
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI components
        ImageView btnBack = findViewById(R.id.btnBack);
        EditText emailInput = findViewById(R.id.emailInput);
        TextView btnSendResetLink = findViewById(R.id.btnSendResetLink);
        TextView backToSignIn = findViewById(R.id.backToSignIn);

        // Back Button Logic
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        // Back to Sign In Link
        if (backToSignIn != null) backToSignIn.setOnClickListener(v -> finish());

        // Send Reset Link Logic
        if (btnSendResetLink != null) {
            btnSendResetLink.setOnClickListener(v -> {
                String email = emailInput != null ? emailInput.getText().toString().trim() : "";
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSendResetLink.setEnabled(false);
                apiClient.forgotPassword(email, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            btnSendResetLink.setEnabled(true);
                            Toast.makeText(ForgotPasswordActivity.this, "Network error sending verification code.", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.body() == null) return;
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            boolean success = json.optBoolean("success", false);
                            String message = json.optString("message", "Unable to send code.");

                            runOnUiThread(() -> {
                                btnSendResetLink.setEnabled(true);
                                if (success) {
                                    Toast.makeText(ForgotPasswordActivity.this, "Verification code sent!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                                    intent.putExtra("EMAIL", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                btnSendResetLink.setEnabled(true);
                                Toast.makeText(ForgotPasswordActivity.this, "Request failed.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            });
        }
    }
}
