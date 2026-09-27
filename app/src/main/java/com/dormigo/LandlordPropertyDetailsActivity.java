package com.dormigo;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LandlordPropertyDetailsActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();

    private int houseId;
    private int landlordId;
    private String propertyName = "";
    private String propertyAddress = "";
    private String propertyDescription = "";
    private String propertyRules = "";
    private String propertyStatus = "ACTIVE";
    private String totalUnits = "0";
    private String occupiedUnits = "0";
    private String availableUnits = "0";

    private TextView detailPropertyName;
    private TextView detailPropertyAddress;
    private TextView propertyNameTitle;
    private TextView detailTotalUnits;
    private TextView detailOccupiedUnits;
    private TextView detailAvailableUnits;
    private TextView roomsStatus;
    private LinearLayout roomsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_property_details);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        bindViews();
        readIntentData();
        setupUI();
        loadVerificationStatus();
        loadPaymentSettingsSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (houseId > 0) {
            loadRooms();
            loadVerificationStatus();
            loadPaymentSettingsSummary();
        }
    }

    private void bindViews() {
        propertyNameTitle = findViewById(R.id.propertyNameTitle);
        detailPropertyName = findViewById(R.id.detailPropertyName);
        detailPropertyAddress = findViewById(R.id.detailPropertyAddress);
        detailTotalUnits = findViewById(R.id.detailTotalUnits);
        detailOccupiedUnits = findViewById(R.id.detailOccupiedUnits);
        detailAvailableUnits = findViewById(R.id.detailAvailableUnits);
        roomsStatus = findViewById(R.id.roomsStatus);
        roomsContainer = findViewById(R.id.roomsContainer);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            houseId = intent.getIntExtra("HOUSE_ID", 0);
            landlordId = intent.getIntExtra("LANDLORD_ID", 0);
            propertyName = intent.getStringExtra("PROPERTY_NAME") != null ? intent.getStringExtra("PROPERTY_NAME") : "";
            propertyAddress = intent.getStringExtra("PROPERTY_ADDRESS") != null ? intent.getStringExtra("PROPERTY_ADDRESS") : "";
            propertyDescription = intent.getStringExtra("PROPERTY_DESCRIPTION") != null ? intent.getStringExtra("PROPERTY_DESCRIPTION") : "";
            propertyRules = intent.getStringExtra("PROPERTY_RULES") != null ? intent.getStringExtra("PROPERTY_RULES") : "";
            propertyStatus = intent.getStringExtra("PROPERTY_STATUS") != null ? intent.getStringExtra("PROPERTY_STATUS") : "ACTIVE";
            totalUnits = intent.getStringExtra("TOTAL_UNITS") != null ? intent.getStringExtra("TOTAL_UNITS") : "0";
            occupiedUnits = intent.getStringExtra("OCCUPIED_UNITS") != null ? intent.getStringExtra("OCCUPIED_UNITS") : "0";
            availableUnits = intent.getStringExtra("AVAILABLE_UNITS") != null ? intent.getStringExtra("AVAILABLE_UNITS") : "0";
        }
    }

    private void setupUI() {
        if (!propertyName.isEmpty()) {
            if (propertyNameTitle != null) propertyNameTitle.setText(propertyName);
            if (detailPropertyName != null) detailPropertyName.setText(propertyName);
        }

        if (!propertyAddress.isEmpty() && detailPropertyAddress != null) {
            detailPropertyAddress.setText(propertyAddress);
        }

        if (detailTotalUnits != null) detailTotalUnits.setText(totalUnits);
        if (detailOccupiedUnits != null) detailOccupiedUnits.setText(occupiedUnits);
        if (detailAvailableUnits != null) detailAvailableUnits.setText(availableUnits);

        View btnViewTenants = findViewById(R.id.btnViewTenants);
        if (btnViewTenants != null) {
            btnViewTenants.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordTenantsActivity.class);
                intent.putExtra("HOUSE_ID", houseId);
                intent.putExtra("PROPERTY_NAME", propertyName);
                startActivity(intent);
            });
        }

        View btnEditListing = findViewById(R.id.btnEditListing);
        if (btnEditListing != null) {
            btnEditListing.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, LandlordEditPropertyActivity.class);
                editIntent.putExtra("HOUSE_ID", houseId);
                editIntent.putExtra("LANDLORD_ID", landlordId);
                editIntent.putExtra("PROPERTY_NAME", propertyName);
                editIntent.putExtra("PROPERTY_ADDRESS", propertyAddress);
                editIntent.putExtra("PROPERTY_DESCRIPTION", propertyDescription);
                editIntent.putExtra("PROPERTY_RULES", propertyRules);
                editIntent.putExtra("PROPERTY_STATUS", propertyStatus);
                editIntent.putExtra("TOTAL_UNITS", totalUnits);
                startActivity(editIntent);
            });
        }

        View btnAddRoom = findViewById(R.id.btnAddRoom);
        if (btnAddRoom != null) {
            btnAddRoom.setOnClickListener(v -> showAddRoomDialog());
        }
    }

    // =========================================================
    // LOAD ROOMS
    // =========================================================

    private void loadRooms() {
        if (roomsStatus != null) {
            roomsStatus.setVisibility(View.VISIBLE);
            roomsStatus.setText("Loading rooms...");
        }

        apiClient.getRoomsForHouse(houseId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (roomsStatus != null) {
                        roomsStatus.setText("Unable to load rooms. Tap to retry.");
                        roomsStatus.setOnClickListener(v -> loadRooms());
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray rooms = json.optJSONArray("data");
                        if (rooms == null) {
                            rooms = new JSONArray();
                        }
                        renderRooms(rooms);
                    } catch (Exception e) {
                        if (roomsStatus != null) {
                            roomsStatus.setText("Error parsing room data.");
                        }
                    }
                });
            }
        });
    }

    private void renderRooms(JSONArray rooms) {
        if (roomsContainer == null) return;
        roomsContainer.removeAllViews();

        int total = rooms.length();
        int occupied = 0;
        int available = 0;

        for (int i = 0; i < rooms.length(); i++) {
            JSONObject room = rooms.optJSONObject(i);
            if (room == null) continue;

            String status = room.optString("status", "AVAILABLE").toUpperCase(Locale.ROOT);
            if ("OCCUPIED".equals(status)) {
                occupied++;
            } else if ("AVAILABLE".equals(status)) {
                available++;
            }

            View card = createRoomCard(room);
            roomsContainer.addView(card);
        }

        // Update counters
        if (detailTotalUnits != null) detailTotalUnits.setText(String.valueOf(total));
        if (detailOccupiedUnits != null) detailOccupiedUnits.setText(String.valueOf(occupied));
        if (detailAvailableUnits != null) detailAvailableUnits.setText(String.valueOf(available));

        if (roomsStatus != null) {
            if (total == 0) {
                roomsStatus.setVisibility(View.VISIBLE);
                roomsStatus.setText("No rooms added yet. Tap '+ Add Room' to create one.");
            } else {
                roomsStatus.setVisibility(View.GONE);
            }
        }
    }

    private View createRoomCard(JSONObject room) {
        int roomId = room.optInt("room_id", 0);
        String roomNumber = room.optString("room_number", "Room");
        String roomType = room.optString("room_type", "Standard");
        int capacity = room.optInt("capacity", 1);
        double monthlyRent = room.optDouble("monthly_rent", 0.0);
        String status = room.optString("status", "AVAILABLE").toUpperCase(Locale.ROOT);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_room_card_selectable);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        card.setLayoutParams(params);

        // Header row: Room Number + Status Badge
        RelativeLayout headerRow = new RelativeLayout(this);
        headerRow.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView roomNameText = new TextView(this);
        roomNameText.setText(roomNumber);
        roomNameText.setTextColor(Color.parseColor("#1A1A1A"));
        roomNameText.setTextSize(15);
        roomNameText.setTypeface(null, Typeface.BOLD);
        headerRow.addView(roomNameText);

        TextView badge = new TextView(this);
        badge.setText(status);
        badge.setTextSize(11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(6));
        if ("AVAILABLE".equals(status)) {
            badgeBg.setColor(Color.parseColor("#E8F5E9"));
            badge.setTextColor(Color.parseColor("#2E7D32"));
        } else if ("OCCUPIED".equals(status)) {
            badgeBg.setColor(Color.parseColor("#E0F2F1"));
            badge.setTextColor(Color.parseColor("#1B5E4C"));
        } else {
            badgeBg.setColor(Color.parseColor("#FFF3E0"));
            badge.setTextColor(Color.parseColor("#E65100"));
        }
        badge.setBackground(badgeBg);

        RelativeLayout.LayoutParams badgeParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        badge.setLayoutParams(badgeParams);
        headerRow.addView(badge);

        card.addView(headerRow);

        // Info row: Type + Capacity + Rent
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = dp(6);
        infoRow.setLayoutParams(infoParams);

        TextView typeText = new TextView(this);
        typeText.setText(roomType + " · Capacity: " + capacity + (capacity == 1 ? " person" : " people"));
        typeText.setTextColor(Color.parseColor("#6E6E73"));
        typeText.setTextSize(12);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoRow.addView(typeText, typeParams);

        TextView rentText = new TextView(this);
        rentText.setText(String.format(Locale.getDefault(), "₱%,.2f/mo", monthlyRent));
        rentText.setTextColor(Color.parseColor("#1B5E4C"));
        rentText.setTextSize(13);
        rentText.setTypeface(null, Typeface.BOLD);
        infoRow.addView(rentText);

        card.addView(infoRow);

        // Click to edit/delete room
        card.setOnClickListener(v -> showEditRoomDialog(room));

        return card;
    }

    // =========================================================
    // ADD ROOM DIALOG
    // =========================================================

    private void showAddRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Room");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        final EditText inputNumber = new EditText(this);
        inputNumber.setHint("Room Name / Number (e.g. Room 102)");
        layout.addView(inputNumber);

        final EditText inputType = new EditText(this);
        inputType.setHint("Room Type (e.g. Single, Double, Bedspace)");
        inputType.setText("Single");
        layout.addView(inputType);

        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Capacity (number of persons)");
        inputCapacity.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCapacity.setText("1");
        layout.addView(inputCapacity);

        final EditText inputRent = new EditText(this);
        inputRent.setHint("Monthly Rent (e.g. 2500)");
        inputRent.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputRent);

        final Spinner statusSpinner = new Spinner(this);
        String[] statuses = {"AVAILABLE", "MAINTENANCE", "INACTIVE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        statusSpinner.setAdapter(adapter);
        layout.addView(statusSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Save Room", (dialog, which) -> {
            String roomNum = inputNumber.getText().toString().trim();
            String rType = inputType.getText().toString().trim();
            String capStr = inputCapacity.getText().toString().trim();
            String rentStr = inputRent.getText().toString().trim();
            String rStatus = statusSpinner.getSelectedItem().toString();

            if (roomNum.isEmpty()) {
                Toast.makeText(this, "Room number is required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (rType.isEmpty()) {
                rType = "Single";
            }
            int capacity;
            try {
                capacity = Integer.parseInt(capStr);
            } catch (Exception e) {
                capacity = 1;
            }
            double rent;
            try {
                rent = Double.parseDouble(rentStr);
            } catch (Exception e) {
                Toast.makeText(this, "Valid monthly rent is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            apiClient.createRoom(houseId, roomNum, rType, capacity, rent, rStatus, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LandlordPropertyDetailsActivity.this, "Failed to create room.", Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(body);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(LandlordPropertyDetailsActivity.this, "Room created successfully!", Toast.LENGTH_SHORT).show();
                                loadRooms();
                            } else {
                                Toast.makeText(LandlordPropertyDetailsActivity.this, res.optString("message", "Unable to create room."), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(LandlordPropertyDetailsActivity.this, "Room created.", Toast.LENGTH_SHORT).show();
                            loadRooms();
                        }
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }



    // =========================================================
    // EDIT / DELETE ROOM DIALOG
    // =========================================================

    private void showEditRoomDialog(JSONObject room) {
        int roomId = room.optInt("room_id", 0);
        String roomNumber = room.optString("room_number", "");
        String roomType = room.optString("room_type", "Single");
        int capacity = room.optInt("capacity", 1);
        double monthlyRent = room.optDouble("monthly_rent", 0.0);
        String currentStatus = room.optString("status", "AVAILABLE").toUpperCase(Locale.ROOT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + roomNumber);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(12));

        final EditText inputNumber = new EditText(this);
        inputNumber.setHint("Room Name / Number");
        inputNumber.setText(roomNumber);
        layout.addView(inputNumber);

        final EditText inputType = new EditText(this);
        inputType.setHint("Room Type");
        inputType.setText(roomType);
        layout.addView(inputType);

        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Capacity");
        inputCapacity.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCapacity.setText(String.valueOf(capacity));
        layout.addView(inputCapacity);

        final EditText inputRent = new EditText(this);
        inputRent.setHint("Monthly Rent");
        inputRent.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputRent.setText(String.valueOf(monthlyRent));
        layout.addView(inputRent);

        final Spinner statusSpinner = new Spinner(this);
        String[] statuses = {"AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        statusSpinner.setAdapter(adapter);
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(currentStatus)) {
                statusSpinner.setSelection(i);
                break;
            }
        }
        layout.addView(statusSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String updatedNum = inputNumber.getText().toString().trim();
            String updatedType = inputType.getText().toString().trim();
            int updatedCap;
            try {
                updatedCap = Integer.parseInt(inputCapacity.getText().toString().trim());
            } catch (Exception e) {
                updatedCap = 1;
            }
            double updatedRent;
            try {
                updatedRent = Double.parseDouble(inputRent.getText().toString().trim());
            } catch (Exception e) {
                updatedRent = monthlyRent;
            }
            String updatedStatus = statusSpinner.getSelectedItem().toString();

            apiClient.updateRoom(roomId, updatedNum, updatedType, updatedCap, updatedRent, updatedStatus, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LandlordPropertyDetailsActivity.this, "Failed to update room.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    runOnUiThread(() -> {
                        Toast.makeText(LandlordPropertyDetailsActivity.this, "Room updated successfully!", Toast.LENGTH_SHORT).show();
                        loadRooms();
                    });
                }
            });
        });

        builder.setNeutralButton("Delete", (dialog, which) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Room?")
                    .setMessage("Are you sure you want to delete " + roomNumber + "? This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        apiClient.deleteRoom(roomId, new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> Toast.makeText(LandlordPropertyDetailsActivity.this, "Failed to delete room.", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) {
                                runOnUiThread(() -> {
                                    Toast.makeText(LandlordPropertyDetailsActivity.this, "Room deleted.", Toast.LENGTH_SHORT).show();
                                    loadRooms();
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadVerificationStatus() {
        if (houseId <= 0) return;
        String url = "http://10.129.224.109/Dormigo_Backend/api/get_landlord_verification_status.php?house_id=" + houseId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> updateVerificationUI("NOT_SUBMITTED", ""));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> updateVerificationUI("NOT_SUBMITTED", ""));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            String status = data.optString("verification_status", "NOT_SUBMITTED");
                            String reason = data.optString("rejection_reason", "");
                            runOnUiThread(() -> updateVerificationUI(status, reason));
                        } else {
                            runOnUiThread(() -> updateVerificationUI("NOT_SUBMITTED", ""));
                        }
                    } else {
                        runOnUiThread(() -> updateVerificationUI("NOT_SUBMITTED", ""));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> updateVerificationUI("NOT_SUBMITTED", ""));
                }
            }
        });
    }

    private void updateVerificationUI(String status, String reason) {
        TextView tvStatus = findViewById(R.id.textVerificationStatus);
        TextView tvReason = findViewById(R.id.textRejectionReason);
        TextView btnSubmit = findViewById(R.id.btnSubmitVerification);

        if (tvStatus != null) {
            tvStatus.setText("Status: " + status);
        }
        if (tvReason != null) {
            if (status.equals("REJECTED") && !reason.isEmpty()) {
                tvReason.setVisibility(View.VISIBLE);
                tvReason.setText("Reason: " + reason);
            } else {
                tvReason.setVisibility(View.GONE);
            }
        }
        if (btnSubmit != null) {
            if (status.equals("VERIFIED")) {
                btnSubmit.setText("Verified Property");
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.6f);
            } else if (status.equals("PENDING")) {
                btnSubmit.setText("Verification Pending...");
                btnSubmit.setEnabled(false);
                btnSubmit.setAlpha(0.6f);
            } else {
                btnSubmit.setText(status.equals("REJECTED") ? "Resubmit for Verification" : "Submit for Verification");
                btnSubmit.setEnabled(true);
                btnSubmit.setAlpha(1.0f);
                btnSubmit.setOnClickListener(v -> submitVerificationDialog());
            }
        }
    }

    private void submitVerificationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Submit for Verification")
                .setMessage("Submit this boarding house for admin verification?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", (dialog, which) -> sendVerificationSubmission())
                .show();
    }

    private void sendVerificationSubmission() {
        try {
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("house_id", String.valueOf(houseId))
                    .addFormDataPart("landlord_id", String.valueOf(landlordId));

            RequestBody requestBody = builder.build();
            Request request = new Request.Builder()
                    .url("http://10.129.224.109/Dormigo_Backend/api/submit_verification.php")
                    .post(requestBody)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("VERIF_SUBMIT", "Failure", e);
                    runOnUiThread(() -> Toast.makeText(LandlordPropertyDetailsActivity.this, "Submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d("VERIF_SUBMIT", "Code: " + response.code() + ", Body: " + body);
                    runOnUiThread(() -> {
                        try {
                            JSONObject res = new JSONObject(body);
                            if (res.optBoolean("success", false)) {
                                Toast.makeText(LandlordPropertyDetailsActivity.this, "Submitted successfully! Status: PENDING", Toast.LENGTH_LONG).show();
                                loadVerificationStatus();
                            } else {
                                Toast.makeText(LandlordPropertyDetailsActivity.this, res.optString("message", "Submission failed."), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("VERIF_SUBMIT", "JSON parse error", e);
                            Toast.makeText(LandlordPropertyDetailsActivity.this, "Server response error: " + body, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadPaymentSettingsSummary() {
        if (houseId <= 0) return;
        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject h = data.getJSONObject(i);
                                if (h.optInt("house_id", 0) == houseId) {
                                    int adv = h.optInt("advance_months", 1);
                                    int dep = h.optInt("security_deposit_months", 1);
                                    double util = h.optDouble("utility_deposit", 0);
                                    double fees = h.optDouble("other_fees", 0);
                                    int dueDay = h.optInt("payment_due_day", 1);

                                    String summary = "Advance: " + adv + " Month" + (adv > 1 ? "s" : "") + "\n" +
                                                     "Security Deposit: " + dep + " Month" + (dep > 1 ? "s" : "") + "\n" +
                                                     "Utility Deposit: ₱" + String.format(Locale.US, "%.2f", util) + "\n" +
                                                     "Other Fees: ₱" + String.format(Locale.US, "%.2f", fees) + "\n" +
                                                     "Monthly Due Day: Every " + dueDay + getOrdinalSuffix(dueDay) + " day";

                                    runOnUiThread(() -> {
                                        TextView tv = findViewById(R.id.textPaymentSettingsSummary);
                                        if (tv != null) tv.setText(summary);
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

    private String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}
