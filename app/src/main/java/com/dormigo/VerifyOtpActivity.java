package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private String email = "";
    private EditText otpInput;
    private TextView timerText;
    private TextView btnResendCode;
    private TextView btnVerifyOtp;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_otp);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        email = getIntent().getStringExtra("EMAIL");
        if (email == null) email = "";

        bindViews();
        setupUI();
        startCountdownTimer();
    }

    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        otpInput = findViewById(R.id.otpInput);
        timerText = findViewById(R.id.timerText);
        btnResendCode = findViewById(R.id.btnResendCode);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupUI() {
        if (btnResendCode != null) {
            btnResendCode.setOnClickListener(v -> resendOtpCode());
        }

        if (btnVerifyOtp != null) {
            btnVerifyOtp.setOnClickListener(v -> verifyOtpCode());
        }
    }

    private void startCountdownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(300000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                if (timerText != null) {
                    timerText.setText(String.format(Locale.US, "Code expires in %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (timerText != null) {
                    timerText.setText("Code has expired. Please resend.");
                }
            }
        }.start();
    }

    private void resendOtpCode() {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnResendCode.setEnabled(false);
        apiClient.forgotPassword(email, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    btnResendCode.setEnabled(true);
                    Toast.makeText(VerifyOtpActivity.this, "Failed to resend code.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    btnResendCode.setEnabled(true);
                    Toast.makeText(VerifyOtpActivity.this, "New verification code sent.", Toast.LENGTH_SHORT).show();
                    startCountdownTimer();
                });
            }
        });
    }

    private void verifyOtpCode() {
        String otp = otpInput != null ? otpInput.getText().toString().trim() : "";
        if (otp.length() != 6) {
            Toast.makeText(this, "Please enter a valid 6-digit code.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnVerifyOtp.setEnabled(false);
        apiClient.verifyOtp(email, otp, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    btnVerifyOtp.setEnabled(true);
                    Toast.makeText(VerifyOtpActivity.this, "Network error verifying code.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    boolean success = json.optBoolean("success", false);
                    String message = json.optString("message", "Invalid code.");

                    runOnUiThread(() -> {
                        btnVerifyOtp.setEnabled(true);
                        if (success) {
                            Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("OTP_CODE", otp);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(VerifyOtpActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnVerifyOtp.setEnabled(true);
                        Toast.makeText(VerifyOtpActivity.this, "Verification failed.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
