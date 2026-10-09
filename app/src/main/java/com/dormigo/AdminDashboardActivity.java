package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    
    // Stats Views
    private TextView statStudents, statLandlords, statBoardingHouses, statAvailableRooms;
    private TextView statPendingStudents, statPendingLandlords;
    private TextView statVerifiedStudents, statVerifiedLandlords;
    private TextView statRejectedStudents, statRejectedLandlords;
    private TextView textPendingSummaryBadge;

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadDashboardStats();
            autoRefreshHandler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        bindViews();
        setupActions();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadDashboardStats);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    private void bindViews() {
        statStudents = findViewById(R.id.statStudents);
        statLandlords = findViewById(R.id.statLandlords);
        statBoardingHouses = findViewById(R.id.statBoardingHouses);
        statAvailableRooms = findViewById(R.id.statAvailableRooms);
        
        statPendingStudents = findViewById(R.id.statPendingStudents);
        statPendingLandlords = findViewById(R.id.statPendingLandlords);
        statVerifiedStudents = findViewById(R.id.statVerifiedStudents);
        statVerifiedLandlords = findViewById(R.id.statVerifiedLandlords);
        statRejectedStudents = findViewById(R.id.statRejectedStudents);
        statRejectedLandlords = findViewById(R.id.statRejectedLandlords);

        textPendingSummaryBadge = findViewById(R.id.textPendingSummaryBadge);
    }

    private void setupActions() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences.Editor editor = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        View cardVerificationCenter = findViewById(R.id.cardVerificationCenter);
        View btnOpenVerificationCenter = findViewById(R.id.btnOpenVerificationCenter);
        View.OnClickListener openCenterListener = v -> {
            Intent intent = new Intent(this, AdminVerificationActivity.class);
            startActivity(intent);
        };
        if (cardVerificationCenter != null) cardVerificationCenter.setOnClickListener(openCenterListener);
        if (btnOpenVerificationCenter != null) btnOpenVerificationCenter.setOnClickListener(openCenterListener);
    }

    private void loadDashboardStats() {
        String url = "http://172.20.10.3/Dormigo_Backend/api/get_admin_dashboard_stats.php";
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(AdminDashboardActivity.this, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            runOnUiThread(() -> {
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                updateDashboardStats(data);
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void updateDashboardStats(JSONObject data) {
        if (statStudents != null) statStudents.setText(String.valueOf(data.optInt("students", 0)));
        if (statLandlords != null) statLandlords.setText(String.valueOf(data.optInt("landlords", 0)));
        if (statBoardingHouses != null) statBoardingHouses.setText(String.valueOf(data.optInt("boarding_houses", 0)));
        if (statAvailableRooms != null) statAvailableRooms.setText(String.valueOf(data.optInt("available_rooms", 0)));

        int pStudents = data.optInt("pending_students", 0);
        int pLandlords = data.optInt("pending_landlords", 0);
        int totalPending = pStudents + pLandlords;

        if (statPendingStudents != null) statPendingStudents.setText(String.valueOf(pStudents));
        if (statPendingLandlords != null) statPendingLandlords.setText(String.valueOf(pLandlords));
        if (statVerifiedStudents != null) statVerifiedStudents.setText(String.valueOf(data.optInt("verified_students", 0)));
        if (statVerifiedLandlords != null) statVerifiedLandlords.setText(String.valueOf(data.optInt("verified_landlords", 0)));
        if (statRejectedStudents != null) statRejectedStudents.setText(String.valueOf(data.optInt("rejected_students", 0)));
        if (statRejectedLandlords != null) statRejectedLandlords.setText(String.valueOf(data.optInt("rejected_landlords", 0)));

        if (textPendingSummaryBadge != null) {
            if (totalPending > 0) {
                textPendingSummaryBadge.setText("🔔 " + totalPending + " Pending Requests");
                textPendingSummaryBadge.setTextColor(Color.parseColor("#D97706"));
            } else {
                textPendingSummaryBadge.setText("✅ No Pending Requests");
                textPendingSummaryBadge.setTextColor(Color.parseColor("#1B5E4C"));
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
