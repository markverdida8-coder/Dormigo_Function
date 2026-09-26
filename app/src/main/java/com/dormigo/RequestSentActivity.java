package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RequestSentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_request_sent);

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
        String landlordName = intent.getStringExtra("LANDLORD_NAME");
        String houseName = intent.getStringExtra("HOUSE_NAME");
        String roomName = intent.getStringExtra("ROOM_NAME");
        String moveInDate = intent.getStringExtra("MOVE_IN_DATE");
        String duration = intent.getStringExtra("DURATION");

        TextView sentToLabel = findViewById(R.id.sentToLabel);
        if (sentToLabel != null && landlordName != null) {
            sentToLabel.setText(getString(R.string.sent_to_landlord, landlordName));
        }

        TextView summaryHouse = findViewById(R.id.summaryHouse);
        if (summaryHouse != null && houseName != null) {
            summaryHouse.setText(houseName);
        }

        TextView summaryRoom = findViewById(R.id.summaryRoom);
        if (summaryRoom != null && roomName != null) {
            summaryRoom.setText(roomName);
        }

        TextView summaryMoveIn = findViewById(R.id.summaryMoveIn);
        if (summaryMoveIn != null && moveInDate != null) {
            summaryMoveIn.setText(moveInDate);
        }

        TextView summaryDuration = findViewById(R.id.summaryDuration);
        if (summaryDuration != null && duration != null) {
            summaryDuration.setText(duration);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnPay = findViewById(R.id.btnPayFee);
        if (btnPay != null) {
            btnPay.setOnClickListener(v -> {
                Intent payIntent = new Intent(this, PaymentActivity.class);
                payIntent.putExtra("HOUSE_NAME", houseName);
                payIntent.putExtra("ROOM_NAME", roomName);
                startActivity(payIntent);
            });
        }

        View btnViewRequests = findViewById(R.id.btnViewRequests);
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                Intent requestsIntent = new Intent(this, BookingRequestsActivity.class);
                startActivity(requestsIntent);
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_explore); // Highlight Explore for now

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            if (item.getTitle() != null) {
                showToast(item.getTitle().toString());
            }
            return true;
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}