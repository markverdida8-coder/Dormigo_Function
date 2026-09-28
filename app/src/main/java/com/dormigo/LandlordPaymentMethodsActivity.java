package com.dormigo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordPaymentMethodsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();
    private int houseId = 0;
    private String houseName = "Boarding House";
    private String gcashQrCodePath = "";
    private boolean cashEnabled = true;

    private TextView textSummaryHouseName;
    private TextView textSummaryCashStatus;
    private TextView textSummaryGcashStatus;
    private TextView textSummaryLastUpdated;

    private SwitchCompat cashToggle;
    private TextView textCashDesc;

    private View qrUploadBox;
    private View qrPlaceholder;
    private ImageView qrPreview;
    private View qrActionButtons;
    private TextView btnReplaceQr;
    private TextView btnPreviewQr;
    private TextView btnRemoveQr;
    private TextView btnSavePayment;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_payment_methods);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

                View bottomAction = findViewById(R.id.bottomAction);
                if (bottomAction != null) {
                    bottomAction.setPadding(0, 0, 0, systemBars.bottom);
                }
                return insets;
            });
        }

        readIntentData();
        bindViews();
        setupImagePicker();
        setupUI();
        loadHousePaymentSettings();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        houseId = intent.getIntExtra("HOUSE_ID", 0);
        String name = intent.getStringExtra("HOUSE_NAME");
        if (name != null && !name.trim().isEmpty()) {
            houseName = name.trim();
        }
    }

    private void bindViews() {
        textSummaryHouseName = findViewById(R.id.textSummaryHouseName);
        textSummaryCashStatus = findViewById(R.id.textSummaryCashStatus);
        textSummaryGcashStatus = findViewById(R.id.textSummaryGcashStatus);
        textSummaryLastUpdated = findViewById(R.id.textSummaryLastUpdated);

        cashToggle = findViewById(R.id.cashToggle);
        textCashDesc = findViewById(R.id.textCashDesc);

        qrUploadBox = findViewById(R.id.qrUploadBox);
        qrPlaceholder = findViewById(R.id.qrPlaceholder);
        qrPreview = findViewById(R.id.qrPreview);
        qrActionButtons = findViewById(R.id.qrActionButtons);

        btnReplaceQr = findViewById(R.id.btnReplaceQr);
        btnPreviewQr = findViewById(R.id.btnPreviewQr);
        btnRemoveQr = findViewById(R.id.btnRemoveQr);
        btnSavePayment = findViewById(R.id.btnSavePayment);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && houseId > 0) {
                            uploadQrImage(imageUri);
                        }
                    }
                }
        );
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (textSummaryHouseName != null) {
            textSummaryHouseName.setText(houseName);
        }

        if (cashToggle != null) {
            cashToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cashEnabled = isChecked;
                if (textCashDesc != null) {
                    textCashDesc.setText(isChecked ? "Students may pay rent directly to you in person." : "Cash payments are disabled.");
                }
                updateSummaryCard();
            });
        }

        if (qrUploadBox != null) {
            qrUploadBox.setOnClickListener(v -> openImagePicker());
        }

        if (btnReplaceQr != null) {
            btnReplaceQr.setOnClickListener(v -> openImagePicker());
        }

        if (btnPreviewQr != null) {
            btnPreviewQr.setOnClickListener(v -> showFullscreenQrPreview());
        }

        if (qrPreview != null) {
            qrPreview.setOnClickListener(v -> showFullscreenQrPreview());
        }

        if (btnRemoveQr != null) {
            btnRemoveQr.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Remove GCash QR Code?")
                        .setMessage("Students will no longer be able to pay via GCash for this property until a new QR is uploaded.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove", (dialog, which) -> {
                            gcashQrCodePath = "";
                            updateQrDisplay();
                            updateSummaryCard();
                        })
                        .show();
            });
        }

        if (btnSavePayment != null) {
            btnSavePayment.setOnClickListener(v -> savePaymentSettings());
        }
    }

    private void openImagePicker() {
        if (houseId <= 0) {
            Toast.makeText(this, "Please select a valid boarding house first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select GCash QR Code"));
    }

    private void uploadQrImage(Uri imageUri) {
        Toast.makeText(this, "Uploading QR code...", Toast.LENGTH_SHORT).show();
        apiClient.uploadGcashQrCode(this, houseId, imageUri, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LandlordPaymentMethodsActivity.this, "Failed to upload QR image.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        String webPath = json.optString("gcash_qr_code", "");
                        runOnUiThread(() -> {
                            gcashQrCodePath = webPath;
                            updateQrDisplay();
                            updateSummaryCard();
                            Toast.makeText(LandlordPaymentMethodsActivity.this, "GCash QR Code uploaded successfully!", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void loadHousePaymentSettings() {
        if (houseId <= 0) return;

        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject h = data.optJSONObject(i);
                                if (h != null && h.optInt("house_id", 0) == houseId) {
                                    cashEnabled = h.optBoolean("cash_enabled", true);
                                    gcashQrCodePath = h.optString("gcash_qr_code", "");
                                    String updatedAt = h.optString("gcash_updated_at", "Today");

                                    runOnUiThread(() -> {
                                        if (cashToggle != null) cashToggle.setChecked(cashEnabled);
                                        updateQrDisplay();
                                        updateSummaryCard();
                                        if (textSummaryLastUpdated != null && !updatedAt.isEmpty()) {
                                            textSummaryLastUpdated.setText("Last Updated: " + updatedAt);
                                        }
                                    });
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void updateQrDisplay() {
        if (gcashQrCodePath != null && !gcashQrCodePath.trim().isEmpty() && !"null".equalsIgnoreCase(gcashQrCodePath.trim())) {
            if (qrPlaceholder != null) qrPlaceholder.setVisibility(View.GONE);
            if (qrPreview != null) {
                qrPreview.setVisibility(View.VISIBLE);
                String fullUrl = gcashQrCodePath.startsWith("http") ? gcashQrCodePath : "http://10.149.229.109/Dormigo_Backend/" + gcashQrCodePath;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(qrPreview);
            }
            if (qrActionButtons != null) qrActionButtons.setVisibility(View.VISIBLE);
        } else {
            if (qrPlaceholder != null) qrPlaceholder.setVisibility(View.VISIBLE);
            if (qrPreview != null) {
                qrPreview.setImageDrawable(null);
                qrPreview.setVisibility(View.GONE);
            }
            if (qrActionButtons != null) qrActionButtons.setVisibility(View.GONE);
        }
    }

    private void updateSummaryCard() {
        if (textSummaryCashStatus != null) {
            textSummaryCashStatus.setText("Cash: " + (cashEnabled ? "Enabled" : "Disabled"));
        }
        if (textSummaryGcashStatus != null) {
            boolean hasGcash = gcashQrCodePath != null && !gcashQrCodePath.trim().isEmpty() && !"null".equalsIgnoreCase(gcashQrCodePath.trim());
            textSummaryGcashStatus.setText("GCash: " + (hasGcash ? "Configured" : "Not Configured"));
        }
    }

    private void showFullscreenQrPreview() {
        if (gcashQrCodePath == null || gcashQrCodePath.trim().isEmpty()) {
            Toast.makeText(this, "No QR Code uploaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView fullImageView = new ImageView(this);
        fullImageView.setPadding(24, 24, 24, 24);
        String fullUrl = gcashQrCodePath.startsWith("http") ? gcashQrCodePath : "http://10.149.229.109/Dormigo_Backend/" + gcashQrCodePath;
        Glide.with(this).load(fullUrl).into(fullImageView);

        builder.setView(fullImageView)
                .setTitle("GCash QR Code")
                .setPositiveButton("Close", null)
                .show();
    }

    private void savePaymentSettings() {
        if (houseId <= 0) {
            Toast.makeText(this, "Please select a valid boarding house.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSavePayment.setEnabled(false);
        boolean currentCashEnabled = cashToggle != null ? cashToggle.isChecked() : cashEnabled;

        apiClient.updateBoardingHousePaymentSettings(houseId, currentCashEnabled, gcashQrCodePath, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    btnSavePayment.setEnabled(true);
                    Toast.makeText(LandlordPaymentMethodsActivity.this, "Failed to update payment settings.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    btnSavePayment.setEnabled(true);
                    Toast.makeText(LandlordPaymentMethodsActivity.this, "✓ Payment settings updated successfully.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
}
