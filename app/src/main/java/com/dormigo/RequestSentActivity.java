package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
                return insets;
            });
        }

        setupUI();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToHome();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
            btnBack.setOnClickListener(v -> navigateToHome());
        }

        View btnViewRequests = findViewById(R.id.btnViewRequests);
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                Intent requestsIntent = new Intent(this, BookingRequestsActivity.class);
                startActivity(requestsIntent);
                finish();
            });
        }

        View btnBackHome = findViewById(R.id.btnBackHome);
        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> navigateToHome());
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
