package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminVerificationDetailActivity extends AppCompatActivity {

    private int verificationId;
    private int adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_verification_detail);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        adminId = prefs.getInt("userId", 0);

        setupBackButton();
        readIntentAndDisplay();
        setupActions();
    }

    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void readIntentAndDisplay() {
        Intent intent = getIntent();
        verificationId = intent.getIntExtra("VERIFICATION_ID", 0);
        String landlordName = intent.getStringExtra("LANDLORD_NAME");
        String email = intent.getStringExtra("LANDLORD_EMAIL");
        String phone = intent.getStringExtra("LANDLORD_PHONE");
        String proofPath = intent.getStringExtra("PROOF_PATH");
        String status = intent.getStringExtra("STATUS");

        TextView tvLandlord = findViewById(R.id.textLandlordName);
        TextView tvContact = findViewById(R.id.textLandlordContact);
        TextView tvStatus = findViewById(R.id.textStatus);
        ImageView imgProof = findViewById(R.id.imgProof);

        if (tvLandlord != null) tvLandlord.setText("Landlord: " + landlordName);
        if (tvContact != null) tvContact.setText("Email: " + email + " | Phone: " + phone);
        if (tvStatus != null) tvStatus.setText("Status: " + status);

        if (proofPath != null && !proofPath.isEmpty()) {
            if (imgProof != null) {
                imgProof.setOnClickListener(v -> {
                    String url = "http://10.209.52.109/Dormigo_Backend/" + proofPath;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                });
            }
        }
    }

    private void setupActions() {
        View btnApprove = findViewById(R.id.btnApprove);
        View btnReject = findViewById(R.id.btnReject);

        if (btnApprove != null) {
            btnApprove.setOnClickListener(v -> confirmApprove());
        }

        if (btnReject != null) {
            btnReject.setOnClickListener(v -> promptRejectReason());
        }
    }

    private void confirmApprove() {
        new AlertDialog.Builder(this)
                .setTitle("Approve Verification")
                .setMessage("Are you sure you want to approve this boarding house? It will receive the Verified badge.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Approve", (dialog, which) -> sendApproval())
                .show();
    }

    private void sendApproval() {
        try {
            JSONObject json = new JSONObject();
            json.put("verification_id", verificationId);
            json.put("admin_id", adminId);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("http://10.209.52.109/Dormigo_Backend/api/approve_verification.php")
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminVerificationDetailActivity.this, "Network error.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(bodyStr);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(AdminVerificationDetailActivity.this, "Boarding house approved!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AdminVerificationDetailActivity.this, res.optString("message", "Approval failed."), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AdminVerificationDetailActivity.this, "Error processing response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void promptRejectReason() {
        final EditText input = new EditText(this);
        input.setHint("Enter rejection reason (e.g. Unclear document)");
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Reject Verification")
                .setMessage("Please provide a reason for rejection so the landlord can correct it:")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Rejection reason is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendRejection(reason);
                })
                .show();
    }

    private void sendRejection(String reason) {
        try {
            JSONObject json = new JSONObject();
            json.put("verification_id", verificationId);
            json.put("admin_id", adminId);
            json.put("rejection_reason", reason);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("http://10.209.52.109/Dormigo_Backend/api/reject_verification.php")
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminVerificationDetailActivity.this, "Network error.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(bodyStr);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(AdminVerificationDetailActivity.this, "Boarding house verification rejected.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AdminVerificationDetailActivity.this, res.optString("message", "Rejection failed."), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AdminVerificationDetailActivity.this, "Error processing response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
