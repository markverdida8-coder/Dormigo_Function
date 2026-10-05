package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminStudentVerificationDetailActivity extends AppCompatActivity {

    private int verificationId;
    private int adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_student_verification_detail);

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
        String studentName = intent.getStringExtra("STUDENT_NAME");
        String email = intent.getStringExtra("STUDENT_EMAIL");
        String phone = intent.getStringExtra("STUDENT_PHONE");
        String studentIdPath = intent.getStringExtra("STUDENT_ID_PATH");
        String status = intent.getStringExtra("STATUS");

        TextView tvName = findViewById(R.id.textStudentName);
        TextView tvContact = findViewById(R.id.textStudentContact);
        TextView tvStatus = findViewById(R.id.textStatus);
        ImageView imgId = findViewById(R.id.imgStudentId);

        if (tvName != null) tvName.setText(studentName);
        if (tvContact != null) tvContact.setText("Email: " + email + " | Phone: " + phone);
        if (tvStatus != null) tvStatus.setText("Status: " + status);

        if (studentIdPath != null && !studentIdPath.isEmpty()) {
            String url = "http://10.209.52.109/Dormigo_Backend/" + studentIdPath;
            if (imgId != null) {
                Glide.with(this).load(url).placeholder(R.drawable.bg_image_placeholder).into(imgId);
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
                .setTitle("Approve Student Verification")
                .setMessage("Are you sure you want to approve this student verification?")
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
                    .url("http://10.209.52.109/Dormigo_Backend/api/approve_student_verification.php")
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminStudentVerificationDetailActivity.this, "Network error.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(bodyStr);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(AdminStudentVerificationDetailActivity.this, "Student verification approved!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AdminStudentVerificationDetailActivity.this, res.optString("message", "Approval failed."), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AdminStudentVerificationDetailActivity.this, "Error processing response.", Toast.LENGTH_SHORT).show();
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
        input.setHint("Enter rejection reason (e.g. Unclear student ID)");
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Reject Student Verification")
                .setMessage("Please provide a reason for rejection:")
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
                    .url("http://10.209.52.109/Dormigo_Backend/api/reject_student_verification.php")
                    .post(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(AdminStudentVerificationDetailActivity.this, "Network error.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(bodyStr);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(AdminStudentVerificationDetailActivity.this, "Student verification rejected.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AdminStudentVerificationDetailActivity.this, res.optString("message", "Rejection failed."), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(AdminStudentVerificationDetailActivity.this, "Error processing response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
