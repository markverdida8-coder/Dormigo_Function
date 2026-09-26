package com.dormigo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LandlordPaymentMethodsActivity extends AppCompatActivity {

    private ImageView qrPreview;
    private View qrPlaceholder;
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

        // Initialize image picker result launcher for GCash QR
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && qrPreview != null && qrPlaceholder != null) {
                            qrPreview.setImageURI(imageUri);
                            qrPreview.setVisibility(View.VISIBLE);
                            qrPlaceholder.setVisibility(View.GONE);
                            Toast.makeText(this, "GCash QR Code uploaded successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        setupUI();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        qrUploadBox_setup();

        View btnSavePayment = findViewById(R.id.btnSavePayment);
        if (btnSavePayment != null) {
            btnSavePayment.setOnClickListener(v -> {
                Toast.makeText(this, "Payment settings saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    private void qrUploadBox_setup() {
        View qrBox = findViewById(R.id.qrUploadBox);
        qrPlaceholder = findViewById(R.id.qrPlaceholder);
        qrPreview = findViewById(R.id.qrPreview);

        if (qrBox != null) {
            qrBox.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                imagePickerLauncher.launch(Intent.createChooser(intent, "Select GCash QR Code"));
            });
        }
    }
}
