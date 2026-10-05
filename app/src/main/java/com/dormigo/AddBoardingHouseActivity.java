package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
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
import java.util.Locale;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import androidx.annotation.NonNull;
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

    private final ApiClient apiClient = new ApiClient();
    private boolean saving;
    private boolean amenitiesLoaded;
    private boolean amenitiesLoading;
    private final Set<Integer> selectedAmenityIds = new LinkedHashSet<>();
    private final List<Uri> photoUris = new ArrayList<>();
    private final List<RoomViewHolder> roomHolders = new ArrayList<>();
    private ActivityResultLauncher<String[]> imagePickerLauncher;

    private static class RoomFee {
        String name;
        double amount;
        String feeType;
        String description;

        RoomFee(String name, double amount, String feeType, String description) {
            this.name = name;
            this.amount = amount;
            this.feeType = feeType != null ? feeType : "ONE_TIME";
            this.description = description;
        }
    }

    private static class RoomViewHolder {
        View rootView;
        TextView textRoomTitle;
        TextView btnRemoveRoom;
        AutoCompleteTextView inputRoomType;
        EditText inputRoomName;
        EditText inputRoomRent;
        EditText inputRoomCapacity;
        TextView btnAddRoomFee;
        LinearLayout roomFeesContainer;
        EditText inputRefundPolicy;
        TextView textCalculatedTotal;

        List<RoomFee> roomFees = new ArrayList<>();

        void updateCalculation() {
            try {
                String rentStr = inputRoomRent.getText().toString().trim();
                BigDecimal rent = rentStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(rentStr);

                BigDecimal oneTimeFeesTotal = BigDecimal.ZERO;
                BigDecimal monthlyFeesTotal = BigDecimal.ZERO;
                for (RoomFee f : roomFees) {
                    if ("MONTHLY".equalsIgnoreCase(f.feeType) || "Monthly Recurring Fee".equalsIgnoreCase(f.feeType)) {
                        monthlyFeesTotal = monthlyFeesTotal.add(new BigDecimal(String.valueOf(f.amount)));
                    } else {
                        oneTimeFeesTotal = oneTimeFeesTotal.add(new BigDecimal(String.valueOf(f.amount)));
                    }
                }

                BigDecimal moveInTotal = rent.add(oneTimeFeesTotal);
                BigDecimal monthlyTotal = rent.add(monthlyFeesTotal);

                textCalculatedTotal.setText("Move-In: ₱" + moveInTotal.setScale(2, RoundingMode.HALF_UP) + " | Monthly: ₱" + monthlyTotal.setScale(2, RoundingMode.HALF_UP) + "/mo");
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
        setupAmenitiesSearchAndCustom();

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
        holder.btnAddRoomFee = card.findViewById(R.id.btnAddRoomFee);
        holder.roomFeesContainer = card.findViewById(R.id.roomFeesContainer);
        holder.inputRefundPolicy = card.findViewById(R.id.inputRefundPolicy);
        holder.textCalculatedTotal = card.findViewById(R.id.textCalculatedTotal);

        if (holder.btnAddRoomFee != null) {
            holder.btnAddRoomFee.setOnClickListener(v -> showAddRoomFeeDialog(holder, null, -1));
        }

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

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                holder.updateCalculation();
            }
        };
        holder.inputRoomRent.addTextChangedListener(watcher);

        if (savedData != null) {
            holder.inputRoomName.setText(savedData.optString("room_number", ""));
            holder.inputRoomType.setText(savedData.optString("room_type", "Bedspace"), false);
            holder.inputRoomCapacity.setText(String.valueOf(savedData.optInt("capacity", 1)));
            holder.inputRoomRent.setText(savedData.optString("monthly_rent", ""));
            double fees = savedData.optDouble("other_fees", 0);
            if (fees > 0) {
                holder.roomFees.add(new RoomFee("Additional Fee", fees, "ONE_TIME", savedData.optString("other_fees_description", "")));
                renderRoomFees(holder);
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

    private void showAddRoomFeeDialog(RoomViewHolder holder, RoomFee existingFee, int editIndex) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));

        Spinner spinnerType = new Spinner(this);
        String[] feeTypes = new String[]{"Advance Payment", "Security Deposit", "Reservation Fee", "Utility Deposit", "Cleaning Deposit", "One-Time Fee", "Monthly Recurring Fee"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, feeTypes);
        spinnerType.setAdapter(typeAdapter);

        LinearLayout.LayoutParams amtLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        amtLp.topMargin = dp(10);
        spinnerType.setLayoutParams(amtLp);
        layout.addView(spinnerType);

        EditText inputName = new EditText(this);
        inputName.setHint("Fee Name");
        inputName.setText(existingFee != null ? existingFee.name : "Advance Payment");
        inputName.setLayoutParams(amtLp);
        layout.addView(inputName);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedType = feeTypes[position];
                if (!"One-Time Fee".equalsIgnoreCase(selectedType) && !"Monthly Recurring Fee".equalsIgnoreCase(selectedType)) {
                    inputName.setText(selectedType);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (existingFee != null) {
            for (int i = 0; i < feeTypes.length; i++) {
                if (feeTypes[i].equalsIgnoreCase(existingFee.feeType)) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        }

        EditText inputAmt = new EditText(this);
        inputAmt.setHint("Amount (₱)");
        inputAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputAmt.setText(existingFee != null ? String.valueOf(existingFee.amount) : "");
        inputAmt.setLayoutParams(amtLp);
        layout.addView(inputAmt);

        EditText inputDesc = new EditText(this);
        inputDesc.setHint("Description / Details (Optional)");
        inputDesc.setText(existingFee != null ? existingFee.description : "");
        inputDesc.setLayoutParams(amtLp);
        layout.addView(inputDesc);

        new AlertDialog.Builder(this)
                .setTitle(existingFee != null ? "Edit Fee" : "+ Add Fee")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String amtStr = inputAmt.getText().toString().trim();
                    String feeType = spinnerType.getSelectedItem().toString();
                    String canonicalType = "Monthly Recurring Fee".equalsIgnoreCase(feeType) ? "MONTHLY" : "ONE_TIME";
                    String desc = inputDesc.getText().toString().trim();

                    if (!name.isEmpty() && !amtStr.isEmpty()) {
                        double amt = Double.parseDouble(amtStr);
                        if (editIndex >= 0 && editIndex < holder.roomFees.size()) {
                            holder.roomFees.set(editIndex, new RoomFee(name, amt, canonicalType, desc));
                        } else {
                            holder.roomFees.add(new RoomFee(name, amt, canonicalType, desc));
                        }
                        renderRoomFees(holder);
                        holder.updateCalculation();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderRoomFees(RoomViewHolder holder) {
        if (holder.roomFeesContainer == null) return;
        holder.roomFeesContainer.removeAllViews();

        for (int i = 0; i < holder.roomFees.size(); i++) {
            final int index = i;
            RoomFee f = holder.roomFees.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12), dp(8), dp(12), dp(8));
            card.setBackgroundResource(R.drawable.bg_card_rounded);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            card.setLayoutParams(lp);

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            textLayout.setOrientation(LinearLayout.VERTICAL);

            TextView tvName = new TextView(this);
            String typeTag = "MONTHLY".equalsIgnoreCase(f.feeType) ? "Monthly Recurring Fee · ₱" + String.format(Locale.US, "%,.2f", f.amount) + "/mo" : "One-Time Fee · ₱" + String.format(Locale.US, "%,.2f", f.amount);
            tvName.setText(f.name + "\n" + typeTag);
            tvName.setTextColor(Color.parseColor("#1A1A1A"));
            tvName.setTextSize(13);
            tvName.setTypeface(null, Typeface.BOLD);

            textLayout.addView(tvName);
            if (!f.description.isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(f.description);
                tvDesc.setTextColor(Color.parseColor("#6E6E73"));
                tvDesc.setTextSize(11);
                textLayout.addView(tvDesc);
            }
            card.addView(textLayout);

            TextView btnEdit = new TextView(this);
            btnEdit.setText("Edit");
            btnEdit.setTextColor(Color.parseColor("#1B5E4C"));
            btnEdit.setTextSize(12);
            btnEdit.setPadding(dp(6), dp(4), dp(6), dp(4));
            btnEdit.setOnClickListener(v -> showAddRoomFeeDialog(holder, f, index));
            card.addView(btnEdit);

            TextView btnDelete = new TextView(this);
            btnDelete.setText("Delete");
            btnDelete.setTextColor(Color.parseColor("#D32F2F"));
            btnDelete.setTextSize(12);
            btnDelete.setPadding(dp(6), dp(4), dp(6), dp(4));
            btnDelete.setOnClickListener(v -> {
                holder.roomFees.remove(index);
                renderRoomFees(holder);
                holder.updateCalculation();
            });
            card.addView(btnDelete);

            holder.roomFeesContainer.addView(card);
        }
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
        if (button != null) {
            button.setEnabled(false);
            button.setText("Saving...");
        }
        findViewById(R.id.btnBack).setEnabled(false);

        String url = "http://10.242.38.109/Dormigo_Backend/api/get_landlord_account_verification_status.php?landlord_id=" + landlordId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddBoardingHouseActivity.this, "Your landlord account must be verified before your boarding house can be published. Saved as draft.", Toast.LENGTH_LONG).show();
                    executeSaveHouse(landlordId, "INACTIVE");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String vStatus = "NOT_SUBMITTED";
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            JSONObject data = json.optJSONObject("data");
                            if (data != null) {
                                vStatus = data.optString("verification_status", "NOT_SUBMITTED").toUpperCase();
                            }
                        }
                    } catch (Exception ignored) {}
                }

                final String finalStatus = vStatus;
                runOnUiThread(() -> {
                    if ("VERIFIED".equals(finalStatus) || "APPROVED".equals(finalStatus)) {
                        executeSaveHouse(landlordId, "ACTIVE");
                    } else if ("REJECTED".equals(finalStatus)) {
                        saving = false;
                        if (button != null) { button.setEnabled(true); button.setText("Save listing"); }
                        findViewById(R.id.btnBack).setEnabled(true);
                        Toast.makeText(AddBoardingHouseActivity.this, "Your landlord verification was rejected. Please resubmit your verification documents.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AddBoardingHouseActivity.this, "Your landlord account must be verified before your boarding house can be published. Saved as draft.", Toast.LENGTH_LONG).show();
                        executeSaveHouse(landlordId, "INACTIVE");
                    }
                });
            }
        });
    }

    private void executeSaveHouse(int landlordId, String targetStatus) {
        JSONObject payload;
        List<Uri> photos = new ArrayList<>(photoUris);
        try {
            payload = new JSONObject();
            payload.put("landlord_id", landlordId);
            payload.put("house_name", inputText(R.id.inputHouseName));
            payload.put("description", inputText(R.id.inputDescription));
            payload.put("address", inputText(R.id.inputAddress));
            payload.put("house_rules", inputText(R.id.inputHouseRules));
            payload.put("status", targetStatus);
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
                room.put("advance_months", 0);
                room.put("deposit_months", 0);
                double totalOtherFees = 0;
                StringBuilder otherFeesDesc = new StringBuilder();
                for (RoomFee f : h.roomFees) {
                    totalOtherFees += f.amount;
                    if (otherFeesDesc.length() > 0) otherFeesDesc.append("; ");
                    otherFeesDesc.append(f.name).append(": ₱").append(f.amount);
                }
                room.put("other_fees", totalOtherFees);
                room.put("other_fees_description", otherFeesDesc.toString());
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
                        masterAmenitiesRows = rows;
                        filterAndRenderAmenities("");
                        renderSelectedAmenitiesChips();
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
                renderSelectedAmenitiesChips();
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

    private JSONArray masterAmenitiesRows = new JSONArray();

    private void setupAmenitiesSearchAndCustom() {
        EditText searchInput = findViewById(R.id.searchAmenitiesInput);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterAndRenderAmenities(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        View btnAddCustom = findViewById(R.id.btnAddCustomAmenity);
        if (btnAddCustom != null) {
            btnAddCustom.setOnClickListener(v -> showAddCustomAmenityDialog());
        }
    }

    private void showAddCustomAmenityDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g. Balcony, Study Desk, Hot Shower");
        input.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("+ Add Custom Amenity")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        apiClient.addCustomAmenity(name, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> Toast.makeText(AddBoardingHouseActivity.this, "Failed to add amenity.", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                runOnUiThread(() -> {
                                    Toast.makeText(AddBoardingHouseActivity.this, "Amenity added successfully!", Toast.LENGTH_SHORT).show();
                                    loadAmenities();
                                });
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterAndRenderAmenities(String query) {
        if (masterAmenitiesRows == null) return;
        JSONArray filtered = new JSONArray();
        String q = query.toLowerCase(Locale.ROOT);
        for (int i = 0; i < masterAmenitiesRows.length(); i++) {
            JSONObject row = masterAmenitiesRows.optJSONObject(i);
            if (row != null) {
                String name = row.optString("amenity_name", "").toLowerCase(Locale.ROOT);
                if (q.isEmpty() || name.contains(q)) {
                    filtered.put(row);
                }
            }
        }
        renderAmenities(filtered);
    }

    private void renderSelectedAmenitiesChips() {
        LinearLayout container = findViewById(R.id.selectedAmenitiesContainer);
        if (container == null) return;
        container.removeAllViews();

        for (int i = 0; i < masterAmenitiesRows.length(); i++) {
            JSONObject row = masterAmenitiesRows.optJSONObject(i);
            if (row != null) {
                int id = row.optInt("amenity_id");
                String name = row.optString("amenity_name");
                if (selectedAmenityIds.contains(id)) {
                    TextView chip = new TextView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
                    params.setMarginEnd(dp(8));
                    chip.setLayoutParams(params);
                    chip.setGravity(Gravity.CENTER);
                    chip.setPadding(dp(12), 0, dp(12), 0);
                    chip.setTextSize(12);
                    chip.setText("✓ " + name + " ✕");
                    chip.setBackgroundResource(R.drawable.bg_button_filled);
                    chip.setTextColor(Color.WHITE);
                    chip.setOnClickListener(v -> {
                        selectedAmenityIds.remove(id);
                        renderSelectedAmenitiesChips();
                        filterAndRenderAmenities(inputText(R.id.searchAmenitiesInput));
                    });
                    container.addView(chip);
                }
            }
        }
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
