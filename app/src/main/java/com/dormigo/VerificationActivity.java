package com.dormigo;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VerificationActivity extends AppCompatActivity {

    private Uri selectedFileUri = null;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    updateUploadUI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification);

        // Adjust for system bars
        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                View bottomActions = findViewById(R.id.bottomActions);
                if (bottomActions != null) {
                    bottomActions.setPadding(bottomActions.getPaddingLeft(), 
                        bottomActions.getPaddingTop(), 
                        bottomActions.getPaddingRight(), 
                        systemBars.bottom);
                }
                return insets;
            });
        }

        setupUI();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnUploadDropzone = findViewById(R.id.btnUploadDropzone);
        if (btnUploadDropzone != null) {
            btnUploadDropzone.setOnClickListener(v -> {
                // Launch file picker for images and pdfs
                filePickerLauncher.launch(new String[]{"image/*", "application/pdf"});
            });
        }

        View btnSubmitVerification = findViewById(R.id.btnSubmitVerification);
        if (btnSubmitVerification != null) {
            btnSubmitVerification.setOnClickListener(v -> submitStudentVerification());
        }
    }

    private void submitStudentVerification() {
        if (selectedFileUri == null) {
            showToast("Please upload your student ID first.");
            return;
        }

        int userId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).getInt("userId", -1);
        if (userId <= 0) {
            showToast("Please log in again.");
            return;
        }

        new Thread(() -> {
            try {
                File file = File.createTempFile("student_id_", ".tmp", getCacheDir());
                try (InputStream input = getContentResolver().openInputStream(selectedFileUri);
                     OutputStream output = new FileOutputStream(file)) {
                    if (input == null) throw new IOException("Cannot read selected file.");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }

                MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("user_id", String.valueOf(userId))
                        .addFormDataPart("student_id", "student_id.jpg",
                                RequestBody.create(file, MediaType.get("application/octet-stream")));

                Request request = new Request.Builder()
                        .url("http://10.129.224.109/Dormigo_Backend/api/submit_student_verification.php")
                        .post(builder.build())
                        .build();

                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> showToast("Failed to submit student ID."));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() -> {
                            try {
                                JSONObject res = new JSONObject(body);
                                if (res.optBoolean("success", false)) {
                                    showToast("Student ID submitted for verification successfully!");
                                    finish();
                                } else {
                                    showToast(res.optString("message", "Submission failed."));
                                }
                            } catch (Exception e) {
                                showToast("Submission failed.");
                            }
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Error preparing file for upload."));
            }
        }).start();
    }

    private void updateUploadUI(Uri uri) {
        ImageView uploadIcon = findViewById(R.id.uploadIcon);
        TextView uploadTitle = findViewById(R.id.uploadTitle);
        TextView uploadSubtitle = findViewById(R.id.uploadSubtitle);

        if (uploadIcon != null) {
            uploadIcon.setImageResource(R.drawable.ic_document); // Change to document icon
        }
        if (uploadTitle != null) {
            uploadTitle.setText(getString(R.string.file_selected));
            uploadTitle.setTextColor(0xFF1B5E4C); // Green text
        }
        if (uploadSubtitle != null) {
            String fileName = uri.getLastPathSegment();
            if (fileName != null) {
                // A very basic way to clean up the name if it comes from SAF (e.g. "document:1234")
                if (fileName.contains(":")) {
                    fileName = fileName.substring(fileName.lastIndexOf(":") + 1);
                }
                uploadSubtitle.setText(fileName);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}