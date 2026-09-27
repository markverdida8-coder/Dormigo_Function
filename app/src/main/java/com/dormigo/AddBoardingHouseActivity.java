package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.Gravity;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.io.IOException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddBoardingHouseActivity extends AppCompatActivity {

    private boolean saving;
    private boolean amenitiesLoaded;
    private boolean amenitiesLoading;
    private final Set<Integer> selectedAmenityIds = new LinkedHashSet<>();
    private final List<Uri> photoUris = new ArrayList<>();
    private final List<RoomViewHolder> roomHolders = new ArrayList<>();
    private ActivityResultLauncher<String[]> imagePickerLauncher;

    private static class RoomViewHolder {
        View rootView;
        TextView textRoomTitle;
        TextView btnRemoveRoom;
        AutoCompleteTextView inputRoomType;
        EditText inputRoomName;
        EditText inputRoomRent;
        EditText inputRoomCapacity;
        AutoCompleteTextView inputAdvance;
        AutoCompleteTextView inputDeposit;
        CheckBox checkboxOtherFees;
        View layoutOtherFeesContainer;
        EditText inputOtherFees;
        EditText inputOtherFeesDesc;
        EditText inputRefundPolicy;
        TextView textCalculatedTotal;

        int getAdvanceMonths() {
            String val = inputAdvance.getText().toString();
            if (val.contains("3")) return 3;
            if (val.contains("2")) return 2;
            if (val.contains("1")) return 1;
            return 0;
        }

        int getDepositMonths() {
            String val = inputDeposit.getText().toString();
            if (val.contains("3")) return 3;
            if (val.contains("2")) return 2;
            if (val.contains("1")) return 1;
            return 0;
        }

        void updateCalculation() {
            try {
                String rentStr = inputRoomRent.getText().toString().trim();
                BigDecimal rent = rentStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(rentStr);
                int adv = getAdvanceMonths();
                int dep = getDepositMonths();
                boolean hasFees = checkboxOtherFees.isChecked();
                String feesStr = hasFees ? inputOtherFees.getText().toString().trim() : "0";
                BigDecimal fees = feesStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(feesStr);

                BigDecimal advTotal = rent.multiply(new BigDecimal(adv));
                BigDecimal depTotal = rent.multiply(new BigDecimal(dep));
                BigDecimal total = rent.add(advTotal).add(depTotal).add(fees);

                textCalculatedTotal.setText("Total Move-in Payment: ₱" + total.setScale(2, RoundingMode.HALF_UP));
            } catch (Exception e) {
                textCalculatedTotal.setText("Total Move-in Payment: ₱0.00");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_boarding_house);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), imageUri -> {
                    if (imageUri == null) return;
                    for (Uri u : photoUris) {
                        if (u.equals(imageUri)) {
                            Toast.makeText(this, "Choose a different photo for each slot.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    try {
                        getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    photoUris.add(imageUri);
                    renderDynamicPhotos();
                });

        if (savedInstanceState != null) {
            ArrayList<String> savedUris = savedInstanceState.getStringArrayList("photoUris");
            if (savedUris != null) {
                photoUris.clear();
                for (String u : savedUris) photoUris.add(Uri.parse(u));
            }
            if (savedInstanceState.getBoolean("saveWasRunning", false)) {
                new AlertDialog.Builder(this).setTitle("Check the previous save")
                        .setMessage("The screen was recreated during a save. Check your house list before submitting again to avoid a duplicate.")
                        .setPositiveButton("OK", null).show();
            }
            int[] savedIds = savedInstanceState.getIntArray("selectedAmenityIds");
            if (savedIds != null) {
                for (int id : savedIds) selectedAmenityIds.add(id);
            }
        }
        setupUI();
        loadAmenities();
        renderDynamicPhotos();
    }

    private void setupUI() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAddRoom = findViewById(R.id.btnAddRoom);
        if (btnAddRoom != null) {
            btnAddRoom.setOnClickListener(v -> addNewRoomCard(null));
        }

        if (roomHolders.isEmpty()) {
            addNewRoomCard(null);
        }

        findViewById(R.id.amenitiesStatus).setOnClickListener(v -> {
            if (!amenitiesLoaded && !saving) loadAmenities();
        });

        setupUtilitySwitch(R.id.switchFreeElectricity, R.id.inputElectricityRate);
        setupUtilitySwitch(R.id.switchFreeWater, R.id.inputWaterRate);
        setupPaymentSettingsUI();

        View btnPublish = findViewById(R.id.btnPublish);
        if (btnPublish != null) {
            ((TextView) btnPublish).setText("Save listing");
            btnPublish.setOnClickListener(v -> confirmSave());
        }
    }

    private void setupPaymentSettingsUI() {
        AutoCompleteTextView dueDay = findViewById(R.id.inputPaymentDueDay);
        if (dueDay != null) {
            String[] days = new String[28];
            for (int i = 1; i <= 28; i++) days[i - 1] = String.valueOf(i);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, days
            );
            dueDay.setAdapter(adapter);
            dueDay.setText("1", false);
            dueDay.setOnClickListener(v -> dueDay.showDropDown());
            dueDay.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) dueDay.showDropDown();
            });
            dueDay.setOnItemClickListener((parent, view, position, id) -> updateDueDayHelper(days[position]));
        }
    }

    private void updateDueDayHelper(String dayStr) {
        TextView helper = findViewById(R.id.textDueDayHelper);
        if (helper != null) {
            int day = Integer.parseInt(dayStr);
            helper.setText("Monthly rent is due every " + day + getOrdinalSuffix(day) + " day of the month.");
        }
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

    private void addNewRoomCard(JSONObject savedData) {
        LinearLayout container = findViewById(R.id.roomsContainer);
        if (container == null) return;

        View card = getLayoutInflater().inflate(R.layout.room_item_card, container, false);
        RoomViewHolder holder = new RoomViewHolder();
        holder.rootView = card;
        holder.textRoomTitle = card.findViewById(R.id.textRoomTitle);
        holder.btnRemoveRoom = card.findViewById(R.id.btnRemoveRoom);
        holder.inputRoomType = card.findViewById(R.id.inputRoomType);
        holder.inputRoomName = card.findViewById(R.id.inputRoomName);
        holder.inputRoomRent = card.findViewById(R.id.inputRoomRent);
        holder.inputRoomCapacity = card.findViewById(R.id.inputRoomCapacity);
        holder.inputAdvance = card.findViewById(R.id.inputAdvance);
        holder.inputDeposit = card.findViewById(R.id.inputDeposit);
        holder.checkboxOtherFees = card.findViewById(R.id.checkboxOtherFees);
        holder.layoutOtherFeesContainer = card.findViewById(R.id.layoutOtherFeesContainer);
        holder.inputOtherFees = card.findViewById(R.id.inputOtherFees);
        holder.inputOtherFeesDesc = card.findViewById(R.id.inputOtherFeesDesc);
        holder.inputRefundPolicy = card.findViewById(R.id.inputRefundPolicy);
        holder.textCalculatedTotal = card.findViewById(R.id.textCalculatedTotal);

        // Setup Room Types
        String[] roomTypes = new String[]{"Bedspace", "Shared room", "Solo room", "Dormitory Type", "Studio Type", "Apartment Type"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, roomTypes
        );
        holder.inputRoomType.setAdapter(typeAdapter);
        holder.inputRoomType.setOnClickListener(v -> holder.inputRoomType.showDropDown());
        holder.inputRoomType.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) holder.inputRoomType.showDropDown();
        });

        // Setup Advance Options
        String[] advanceOptions = new String[]{"1 Month", "2 Months", "3 Months"};
        ArrayAdapter<String> advAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, advanceOptions
        );
        holder.inputAdvance.setAdapter(advAdapter);
        holder.inputAdvance.setText("1 Month", false);
        holder.inputAdvance.setOnClickListener(v -> holder.inputAdvance.showDropDown());
        holder.inputAdvance.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) holder.inputAdvance.showDropDown();
        });

        // Setup Deposit Options
        String[] depositOptions = new String[]{"1 Month", "2 Months", "3 Months"};
        ArrayAdapter<String> depAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, depositOptions
        );
        holder.inputDeposit.setAdapter(depAdapter);
        holder.inputDeposit.setText("1 Month", false);
        holder.inputDeposit.setOnClickListener(v -> holder.inputDeposit.showDropDown());
        holder.inputDeposit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) holder.inputDeposit.showDropDown();
        });

        holder.checkboxOtherFees.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.layoutOtherFeesContainer != null) {
                holder.layoutOtherFeesContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            holder.updateCalculation();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                holder.updateCalculation();
            }
        };
        holder.inputRoomRent.addTextChangedListener(watcher);
        holder.inputOtherFees.addTextChangedListener(watcher);

        holder.inputAdvance.setOnItemClickListener((parent, view, position, id) -> holder.updateCalculation());
        holder.inputDeposit.setOnItemClickListener((parent, view, position, id) -> holder.updateCalculation());

        if (savedData != null) {
            holder.inputRoomName.setText(savedData.optString("room_number", ""));
            holder.inputRoomType.setText(savedData.optString("room_type", "Bedspace"), false);
            holder.inputRoomCapacity.setText(String.valueOf(savedData.optInt("capacity", 1)));
            holder.inputRoomRent.setText(savedData.optString("monthly_rent", ""));
            int adv = savedData.optInt("advance_months", 1);
            holder.inputAdvance.setText(adv + " Month" + (adv > 1 ? "s" : ""), false);
            int dep = savedData.optInt("deposit_months", 1);
            holder.inputDeposit.setText(dep + " Month" + (dep > 1 ? "s" : ""), false);
            double fees = savedData.optDouble("other_fees", 0);
            if (fees > 0) {
                holder.checkboxOtherFees.setChecked(true);
                holder.layoutOtherFeesContainer.setVisibility(View.VISIBLE);
                holder.inputOtherFees.setText(String.valueOf(fees));
                holder.inputOtherFeesDesc.setText(savedData.optString("other_fees_description", ""));
            }
            holder.inputRefundPolicy.setText(savedData.optString("deposit_refund_policy", ""));
        }

        holder.btnRemoveRoom.setOnClickListener(v -> {
            if (roomHolders.size() > 1) {
                container.removeView(card);
                roomHolders.remove(holder);
                updateRoomTitles();
            } else {
                Toast.makeText(this, "At least one room is required.", Toast.LENGTH_SHORT).show();
            }
        });

        roomHolders.add(holder);
        container.addView(card);
        updateRoomTitles();
        holder.updateCalculation();
    }

    private void updateRoomTitles() {
        for (int i = 0; i < roomHolders.size(); i++) {
            RoomViewHolder h = roomHolders.get(i);
            h.textRoomTitle.setText("Room " + (i + 1));
            h.btnRemoveRoom.setVisibility(roomHolders.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void renderDynamicPhotos() {
        LinearLayout container = findViewById(R.id.photosContainer);
        if (container == null) return;
        container.removeAllViews();

        for (int i = 0; i < photoUris.size(); i++) {
            final int index = i;
            Uri uri = photoUris.get(i);

            FrameLayout box = new FrameLayout(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(84), dp(84));
            if (i > 0) params.setMarginStart(dp(8));
            box.setLayoutParams(params);
            box.setBackgroundResource(R.drawable.bg_upload_dropzone);

            ImageView imgPreview = new ImageView(this);
            imgPreview.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            box.addView(imgPreview);

            loadThumbnailAsync(uri, imgPreview);

            box.setOnLongClickListener(v -> {
                if (saving) return true;
                photoUris.remove(index);
                renderDynamicPhotos();
                return true;
            });

            container.addView(box);
        }

        if (photoUris.size() < 10) {
            FrameLayout addBox = new FrameLayout(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(84), dp(84));
            if (!photoUris.isEmpty()) params.setMarginStart(dp(8));
            addBox.setLayoutParams(params);
            addBox.setBackgroundResource(R.drawable.bg_upload_dropzone);

            LinearLayout placeholder = new LinearLayout(this);
            placeholder.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            placeholder.setOrientation(LinearLayout.VERTICAL);
            placeholder.setGravity(Gravity.CENTER);

            ImageView ic = new ImageView(this);
            ic.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
            ic.setImageResource(R.drawable.ic_upload_cloud);
            ic.setColorFilter(0xFF1B5E4C);

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setPadding(0, dp(4), 0, 0);
            tv.setText("Add");
            tv.setTextColor(0xFF1B5E4C);
            tv.setTextSize(11);
            tv.setTypeface(null, Typeface.BOLD);

            placeholder.addView(ic);
            placeholder.addView(tv);
            addBox.addView(placeholder);

            addBox.setOnClickListener(v -> {
                if (saving) return;
                imagePickerLauncher.launch(new String[]{"image/jpeg", "image/png", "image/webp"});
            });

            container.addView(addBox);
        }
    }

    private void loadThumbnailAsync(Uri uri, ImageView imageView) {
        new Thread(() -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(input, null, options);
                }
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 256
                        || options.outHeight / options.inSampleSize > 256) options.inSampleSize *= 2;
                final Bitmap bitmap;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, options);
                }
                if (bitmap != null) {
                    runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void setupUtilitySwitch(int switchId, int rateId) {
        SwitchCompat toggle = findViewById(switchId);
        EditText input = findViewById(rateId);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            input.setEnabled(!checked);
            input.setAlpha(checked ? 0.4f : 1.0f);
        });
        input.setEnabled(!toggle.isChecked());
        input.setAlpha(toggle.isChecked() ? 0.4f : 1.0f);
    }

    private String inputText(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }

    private void confirmSave() {
        if (saving) return;
        if (!amenitiesLoaded) {
            Toast.makeText(this, "Wait for amenities to load, or tap the amenities message to retry.", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int landlordId = prefs.getInt("userId", -1);
        if (!prefs.getBoolean("isLoggedIn", false)
                || prefs.getBoolean("isStudent", true) || landlordId <= 0) {
            Toast.makeText(this, "Please sign in with your landlord account.", Toast.LENGTH_LONG).show();
            return;
        }
        String name = inputText(R.id.inputHouseName);
        String address = inputText(R.id.inputAddress);
        if (name.isEmpty()) {
            EditText field = findViewById(R.id.inputHouseName);
            field.setError("House name is required.");
            field.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            EditText field = findViewById(R.id.inputAddress);
            field.setError("Complete address is required.");
            field.requestFocus();
            return;
        }
        if (!validateRooms() || !validateListingDetails()) return;
        new AlertDialog.Builder(this)
                .setTitle("Save listing?")
                .setMessage("Save this house with its rooms, selected amenities, photos, and utility rates? It will remain inactive until you activate it.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Save details", (dialog, which) -> saveHouse(landlordId))
                .show();
    }

    private void saveHouse(int landlordId) {
        if (saving) return;
        if (!validateRooms() || !validateListingDetails()) return;
        saving = true;
        TextView button = findViewById(R.id.btnPublish);
        button.setEnabled(false);
        button.setText("Saving...");
        findViewById(R.id.btnBack).setEnabled(false);
        JSONObject payload;
        List<Uri> photos = new ArrayList<>(photoUris);
        try {
            payload = new JSONObject();
            payload.put("landlord_id", landlordId);
            payload.put("house_name", inputText(R.id.inputHouseName));
            payload.put("description", inputText(R.id.inputDescription));
            payload.put("address", inputText(R.id.inputAddress));
            payload.put("house_rules", inputText(R.id.inputHouseRules));
            payload.put("status", "ACTIVE");
            payload.put("barangay", inputText(R.id.inputBarangay));
            payload.put("distance_km", inputText(R.id.inputDistance));
            boolean freeElectricity = ((SwitchCompat) findViewById(R.id.switchFreeElectricity)).isChecked();
            boolean freeWater = ((SwitchCompat) findViewById(R.id.switchFreeWater)).isChecked();
            payload.put("free_electricity", freeElectricity);
            payload.put("electricity_rate", freeElectricity ? "0.00" : inputText(R.id.inputElectricityRate));
            payload.put("free_water", freeWater);
            payload.put("water_rate", freeWater ? "0.00" : inputText(R.id.inputWaterRate));

            payload.put("payment_due_day", inputText(R.id.inputPaymentDueDay).isEmpty() ? 1 : Integer.parseInt(inputText(R.id.inputPaymentDueDay)));

            JSONArray rooms = new JSONArray();
            for (RoomViewHolder h : roomHolders) {
                JSONObject room = new JSONObject();
                room.put("room_number", h.inputRoomName.getText().toString().trim());
                room.put("room_type", h.inputRoomType.getText().toString().trim());
                room.put("capacity", Integer.parseInt(h.inputRoomCapacity.getText().toString().trim()));
                room.put("monthly_rent", h.inputRoomRent.getText().toString().trim());
                room.put("advance_months", h.getAdvanceMonths());
                room.put("deposit_months", h.getDepositMonths());
                boolean hasFees = h.checkboxOtherFees.isChecked();
                String fees = hasFees ? h.inputOtherFees.getText().toString().trim() : "0.00";
                room.put("other_fees", fees.isEmpty() ? "0.00" : fees);
                room.put("other_fees_description", hasFees ? h.inputOtherFeesDesc.getText().toString().trim() : "");
                room.put("deposit_refund_policy", h.inputRefundPolicy.getText().toString().trim());
                room.put("status", "AVAILABLE");
                rooms.put(room);
            }
            payload.put("rooms", rooms);

            JSONArray amenities = new JSONArray();
            for (int id : selectedAmenityIds) amenities.put(id);
            payload.put("amenity_ids", amenities);
        } catch (Exception e) {
            showSaveResult(false, -1, "Please check your listing details.");
            return;
        }
        new ApiClient().uploadBoardingHouse(this, payload, photos,
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showSaveResult(false, -1,
                                e.getMessage() == null ? "Unable to save listing." : e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (Response closedResponse = response) {
                            String body = closedResponse.body() == null ? "" : closedResponse.body().string();
                            boolean isSuccess = false;
                            int houseId = -1;
                            String message = "Unable to save house details.";
                            try {
                                JSONObject json = new JSONObject(body);
                                isSuccess = closedResponse.isSuccessful() && json.optBoolean("success", false);
                                houseId = json.optInt("house_id", -1);
                                if (houseId == -1 && json.has("data")) {
                                    JSONObject houseObj = json.optJSONObject("data");
                                    if (houseObj != null) houseId = houseObj.optInt("house_id", -1);
                                }
                                if (json.has("message")) {
                                    message = json.optString("message");
                                }
                            } catch (Exception jsonEx) {
                                if (closedResponse.isSuccessful() && !body.trim().isEmpty()) {
                                    isSuccess = true;
                                    message = "Listing saved successfully!";
                                } else {
                                    message = "Server response: " + (body.isEmpty() ? "Empty response" : body);
                                }
                            }
                            showSaveResult(isSuccess, houseId, message);
                        } catch (Exception e) {
                            showSaveResult(false, -1,
                                    "Network error: " + (e.getMessage() != null ? e.getMessage() : "Unable to reach server."));
                        }
                    }
                });
    }

    private void showSaveResult(boolean success, int houseId, String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            saving = false;
            TextView button = findViewById(R.id.btnPublish);
            button.setEnabled(true);
            button.setText("Save listing");
            findViewById(R.id.btnBack).setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                Intent intent = new Intent(AddBoardingHouseActivity.this, LandlordHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private boolean fieldError(int id, String message) {
        EditText field = findViewById(id);
        field.setError(message);
        field.requestFocus();
        return false;
    }

    private boolean validateRooms() {
        if (roomHolders.isEmpty()) {
            Toast.makeText(this, "At least one room is required.", Toast.LENGTH_LONG).show();
            return false;
        }
        for (int i = 0; i < roomHolders.size(); i++) {
            RoomViewHolder h = roomHolders.get(i);
            String name = h.inputRoomName.getText().toString().trim();
            String type = h.inputRoomType.getText().toString().trim();
            String cap = h.inputRoomCapacity.getText().toString().trim();
            String rent = h.inputRoomRent.getText().toString().trim();

            if (name.isEmpty()) {
                h.inputRoomName.setError("Room name is required.");
                h.inputRoomName.requestFocus();
                return false;
            }
            if (type.isEmpty()) {
                Toast.makeText(this, "Room " + (i + 1) + ": Select room type.", Toast.LENGTH_SHORT).show();
                h.inputRoomType.requestFocus();
                return false;
            }
            try {
                if (!cap.matches("[0-9]+") || Integer.parseInt(cap) <= 0) {
                    h.inputRoomCapacity.setError("Enter whole number > 0.");
                    h.inputRoomCapacity.requestFocus();
                    return false;
                }
            } catch (Exception e) {
                h.inputRoomCapacity.setError("Invalid capacity.");
                h.inputRoomCapacity.requestFocus();
                return false;
            }
            try {
                if (!rent.matches("[0-9]+(\\.[0-9]{1,2})?") || new BigDecimal(rent).signum() <= 0) {
                    h.inputRoomRent.setError("Enter valid monthly rent.");
                    h.inputRoomRent.requestFocus();
                    return false;
                }
            } catch (Exception e) {
                h.inputRoomRent.setError("Invalid monthly rent.");
                h.inputRoomRent.requestFocus();
                return false;
            }
        }
        return true;
    }

    private int[] selectedAmenityArray() {
        int[] ids = new int[selectedAmenityIds.size()];
        int index = 0;
        for (int id : selectedAmenityIds) ids[index++] = id;
        return ids;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putIntArray("selectedAmenityIds", selectedAmenityArray());
        outState.putBoolean("saveWasRunning", saving);
        ArrayList<String> uris = new ArrayList<>();
        for (Uri u : photoUris) {
            if (u != null) uris.add(u.toString());
        }
        outState.putStringArrayList("photoUris", uris);
        super.onSaveInstanceState(outState);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadAmenities() {
        if (amenitiesLoading || saving) return;
        amenitiesLoading = true;
        amenitiesLoaded = false;
        TextView status = findViewById(R.id.amenitiesStatus);
        status.setText("Loading amenities...");
        status.setEnabled(false);
        new ApiClient().getAmenities(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                amenityLoadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response closedResponse = response) {
                    String body = closedResponse.body() == null ? "" : closedResponse.body().string();
                    JSONObject json = new JSONObject(body);
                    if (!closedResponse.isSuccessful() || !json.optBoolean("success", false)) {
                        amenityLoadFailed();
                        return;
                    }
                    JSONArray rows = json.getJSONArray("data");
                    Set<Integer> uniqueIds = new LinkedHashSet<>();
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        int id = row.getInt("amenity_id");
                        if (id <= 0 || !uniqueIds.add(id)
                                || !(row.get("amenity_name") instanceof String)
                                || row.getString("amenity_name").trim().isEmpty()) {
                            throw new JSONException("Invalid amenity row");
                        }
                    }
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        renderAmenities(rows);
                    });
                } catch (Exception e) {
                    amenityLoadFailed();
                }
            }
        });
    }

    private void amenityLoadFailed() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            amenitiesLoading = false;
            amenitiesLoaded = false;
            TextView status = findViewById(R.id.amenitiesStatus);
            status.setText("Could not load amenities. Tap here to retry.");
            status.setEnabled(true);
        });
    }

    private void renderAmenities(JSONArray rows) {
        LinearLayout container = findViewById(R.id.amenitiesContainer);
        container.removeAllViews();
        Set<Integer> availableIds = new LinkedHashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            final int id = row.optInt("amenity_id");
            final String name = row.optString("amenity_name").trim();
            availableIds.add(id);
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
            params.setMarginEnd(dp(8));
            chip.setLayoutParams(params);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setTextSize(12);
            chip.setFocusable(true);
            updateAmenityChip(chip, id, name);
            chip.setOnClickListener(v -> {
                if (saving) return;
                if (!selectedAmenityIds.remove(id)) selectedAmenityIds.add(id);
                updateAmenityChip(chip, id, name);
            });
            container.addView(chip);
        }
        selectedAmenityIds.retainAll(availableIds);
        amenitiesLoading = false;
        amenitiesLoaded = true;
        TextView status = findViewById(R.id.amenitiesStatus);
        status.setEnabled(false);
        status.setText(rows.length() == 0 ? "No amenities available yet. You can save without amenities."
                : "Tap to select the amenities your house offers.");
    }

    private void updateAmenityChip(TextView chip, int id, String name) {
        boolean selected = selectedAmenityIds.contains(id);
        chip.setSelected(selected);
        chip.setText(selected ? "✓ " + name : name);
        chip.setBackgroundResource(selected ? R.drawable.bg_button_filled : R.drawable.bg_chip_white);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF1A1A1A);
        chip.setContentDescription(name + (selected ? ", selected" : ", not selected"));
    }

    private boolean validateListingDetails() {
        if (inputText(R.id.inputBarangay).isEmpty()) {
            return fieldError(R.id.inputBarangay, "Barangay is required.");
        }
        if (!validateDecimalField(R.id.inputDistance, 3, "99999.999", false, "Enter a distance of zero or more, with up to 3 decimal places.")) return false;
        boolean freeElectricity = ((SwitchCompat) findViewById(R.id.switchFreeElectricity)).isChecked();
        boolean freeWaterSwitch = ((SwitchCompat) findViewById(R.id.switchFreeWater)).isChecked();
        if (!freeElectricity && !validateDecimalField(R.id.inputElectricityRate, 2, "99999999.99", true, "Enter the electricity rate, or enable Free Electricity.")) return false;
        if (!freeWaterSwitch && !validateDecimalField(R.id.inputWaterRate, 2, "99999999.99", true, "Enter the monthly water rate per tenant, or enable Free Water.")) return false;
        if (photoUris.isEmpty()) {
            Toast.makeText(this, "Choose at least 1 photo.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean validateDecimalField(int id, int places, String maximum, boolean positive, String message) {
        String value = inputText(id);
        try {
            if (!value.matches("[0-9]+(\\.[0-9]{1," + places + "})?")) return fieldError(id, message);
            BigDecimal number = new BigDecimal(value);
            if ((positive ? number.signum() <= 0 : number.signum() < 0)
                    || number.compareTo(new BigDecimal(maximum)) > 0) return fieldError(id, message);
        } catch (NumberFormatException e) {
            return fieldError(id, message);
        }
        return true;
    }
}
