package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_success);

        // Adjust for system bars
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
    }

    private void setupUI() {
        Intent intent = getIntent();
        String amount = intent.getStringExtra("AMOUNT");
        String displayAmount = (amount != null) ? amount : getString(R.string.total_due_amount);
        String paymentDesc = intent.getStringExtra("PAYMENT_DESC");
        if (paymentDesc == null || paymentDesc.isEmpty()) {
            paymentDesc = "Initial Move-in Payment";
        }

        TextView titleTypeLabel = findViewById(R.id.titleTypeLabel);
        if (titleTypeLabel != null) {
            titleTypeLabel.setText(paymentDesc);
        }

        TextView amountSentLabel = findViewById(R.id.amountSentLabel);
        if (amountSentLabel != null) {
            amountSentLabel.setText(displayAmount);
        }

        // Generate dummy reference
        String timeStamp = new SimpleDateFormat("yyyy-MMdd-SSS", Locale.US).format(new Date());
        String reference = "BHF-" + timeStamp;
        TextView referenceLabel = findViewById(R.id.referenceLabel);
        if (referenceLabel != null) {
            referenceLabel.setText(getString(R.string.reference_label, reference));
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateHome());
        }

        View btnViewHistory = findViewById(R.id.btnViewHistory);
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> {
                Intent historyIntent = new Intent(this, TransactionHistoryActivity.class);
                startActivity(historyIntent);
            });
        }

        View btnBackHome = findViewById(R.id.btnBackHome);
        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> navigateHome());
        }
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("deprecation")
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_explore);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                navigateHome();
                return true;
            } else if (id == R.id.nav_chats) {
                Intent intent = new Intent(this, ChatHistoryActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return true;
        });
    }
}