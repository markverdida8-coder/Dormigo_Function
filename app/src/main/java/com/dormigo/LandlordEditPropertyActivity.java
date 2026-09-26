package com.dormigo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordEditPropertyActivity
        extends AppCompatActivity {

    private final ApiClient apiClient =
            new ApiClient();

    private int houseId;
    private int landlordId;

    private EditText editPropertyName;
    private EditText editPropertyAddress;
    private EditText editPropertyDescription;
    private EditText editPropertyRules;
    private EditText editPropertyStatus;

    private TextView btnSaveChanges;

    private LinearLayout amenitiesContainer;

    private String propertyName = "";
    private String propertyAddress = "";
    private String propertyDescription = "";
    private String propertyRules = "";
    private String propertyStatus = "ACTIVE";

    private final Map<Integer, CheckBox> amenityCheckBoxes =
            new LinkedHashMap<>();

    private final Set<Integer> originalAmenityIds =
            new HashSet<>();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_landlord_edit_property
        );

        bindViews();

        setupWindowInsets();

        setupBackButton();

        readIntentData();

        displayCurrentData();

        setupSaveButton();

        setupDeleteButton();

        if (houseId <= 0) {

            showToast(
                    "Invalid boarding house."
            );

            if (btnSaveChanges != null) {
                btnSaveChanges.setEnabled(false);
            }

            return;
        }

        loadPropertyFromDatabase();

        loadAmenities();
    }

    // =========================================================
    // BIND VIEWS
    // =========================================================

    private void bindViews() {

        editPropertyName =
                findViewById(
                        R.id.editPropertyName
                );

        editPropertyAddress =
                findViewById(
                        R.id.editPropertyAddress
                );

        editPropertyDescription =
                findViewById(
                        R.id.editPropertyDescription
                );

        editPropertyRules =
                findViewById(
                        R.id.editPropertyRules
                );

        editPropertyStatus =
                findViewById(
                        R.id.editPropertyStatus
                );

        btnSaveChanges =
                findViewById(
                        R.id.btnSaveChanges
                );

        amenitiesContainer =
                findViewById(
                        R.id.amenitiesContainer
                );
    }

    // =========================================================
    // WINDOW INSETS
    // =========================================================

    private void setupWindowInsets() {

        View mainLayout =
                findViewById(
                        R.id.mainLayout
                );

        if (mainLayout == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                mainLayout,
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars() | WindowInsetsCompat.Type.ime()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            0
                    );

                    View bottomAction =
                            findViewById(
                                    R.id.bottomAction
                            );

                    if (bottomAction != null) {

                        bottomAction.setPadding(
                                dp(16),
                                dp(16),
                                dp(16),
                                systemBars.bottom
                                        + dp(16)
                        );
                    }

                    return insets;
                }
        );
    }

    // =========================================================
    // BACK
    // =========================================================

    private void setupBackButton() {

        View btnBack =
                findViewById(
                        R.id.btnBack
                );

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }
    }

    // =========================================================
    // INTENT DATA
    // =========================================================

    private void readIntentData() {

        Intent intent =
                getIntent();

        houseId =
                intent.getIntExtra(
                        "HOUSE_ID",
                        0
                );

        landlordId =
                intent.getIntExtra(
                        "LANDLORD_ID",
                        0
                );

        propertyName =
                safeString(
                        intent.getStringExtra(
                                "PROPERTY_NAME"
                        )
                );

        propertyAddress =
                safeString(
                        intent.getStringExtra(
                                "PROPERTY_ADDRESS"
                        )
                );

        propertyDescription =
                safeString(
                        intent.getStringExtra(
                                "PROPERTY_DESCRIPTION"
                        )
                );

        propertyRules =
                safeString(
                        intent.getStringExtra(
                                "PROPERTY_RULES"
                        )
                );

        String status =
                safeString(
                        intent.getStringExtra(
                                "PROPERTY_STATUS"
                        )
                );

        if (!status.isEmpty()) {

            propertyStatus =
                    status.toUpperCase();
        }
    }

    // =========================================================
    // INITIAL DISPLAY
    // =========================================================

    private void displayCurrentData() {

        if (editPropertyName != null) {

            editPropertyName.setText(
                    propertyName
            );
        }

        if (editPropertyAddress != null) {

            editPropertyAddress.setText(
                    propertyAddress
            );
        }

        if (editPropertyDescription != null) {

            editPropertyDescription.setText(
                    propertyDescription
            );
        }

        if (editPropertyRules != null) {

            editPropertyRules.setText(
                    propertyRules
            );
        }

        if (editPropertyStatus != null) {

            editPropertyStatus.setText(
                    propertyStatus
            );
        }
    }

    // =========================================================
    // LOAD PROPERTY FROM DATABASE
    // =========================================================

    private void loadPropertyFromDatabase() {

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                showToast(
                                        "Unable to refresh property details."
                                )
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        String result = body;

                        runOnUiThread(() -> {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                result
                                        );

                                if (!json.optBoolean(
                                        "success",
                                        false
                                )) {

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (data == null) {
                                    return;
                                }

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject house =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int currentHouseId =
                                            house.optInt(
                                                    "house_id",
                                                    0
                                            );

                                    if (
                                            currentHouseId
                                                    != houseId
                                    ) {

                                        continue;
                                    }

                                    landlordId =
                                            house.optInt(
                                                    "landlord_id",
                                                    landlordId
                                            );

                                    propertyName =
                                            house.optString(
                                                    "house_name",
                                                    propertyName
                                            );

                                    propertyAddress =
                                            house.optString(
                                                    "address",
                                                    propertyAddress
                                            );

                                    propertyDescription =
                                            house.optString(
                                                    "description",
                                                    propertyDescription
                                            );

                                    propertyRules =
                                            house.optString(
                                                    "house_rules",
                                                    propertyRules
                                            );

                                    propertyStatus =
                                            house.optString(
                                                    "status",
                                                    propertyStatus
                                            );

                                    if (
                                            propertyStatus == null
                                                    ||
                                                    propertyStatus
                                                            .trim()
                                                            .isEmpty()
                                    ) {

                                        propertyStatus =
                                                "ACTIVE";
                                    }

                                    displayCurrentData();

                                    break;
                                }

                            } catch (Exception e) {

                                showToast(
                                        "Invalid property response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // LOAD ALL AMENITIES
    // =========================================================

    private void loadAmenities() {

        showAmenitiesLoading();

        apiClient.getAmenities(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                showAmenitiesMessage(
                                        "Unable to load amenities."
                                )
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        try {

                            JSONArray amenities =
                                    extractDataArray(
                                            body
                                    );

                            loadSelectedAmenities(
                                    amenities
                            );

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    showAmenitiesMessage(
                                            "Invalid amenities response."
                                    )
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD SELECTED AMENITIES FOR THIS HOUSE
    // =========================================================

    private void loadSelectedAmenities(
            JSONArray amenities
    ) {

        apiClient.getBoardingHouseAmenities(
                houseId,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            originalAmenityIds.clear();

                            renderAmenities(
                                    amenities,
                                    new HashSet<>()
                            );

                            showToast(
                                    "Unable to load selected amenities."
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        Set<Integer> selectedIds =
                                new HashSet<>();

                        try {

                            JSONArray selectedAmenities =
                                    extractDataArray(
                                            body
                                    );

                            for (
                                    int i = 0;
                                    i < selectedAmenities.length();
                                    i++
                            ) {

                                JSONObject item =
                                        selectedAmenities
                                                .getJSONObject(
                                                        i
                                                );

                                int amenityId =
                                        item.optInt(
                                                "amenity_id",
                                                0
                                        );

                                if (amenityId > 0) {

                                    selectedIds.add(
                                            amenityId
                                    );
                                }
                            }

                        } catch (Exception ignored) {
                        }

                        runOnUiThread(() -> {

                            originalAmenityIds.clear();

                            originalAmenityIds.addAll(
                                    selectedIds
                            );

                            renderAmenities(
                                    amenities,
                                    selectedIds
                            );
                        });
                    }
                }
        );
    }

    // =========================================================
    // DISPLAY AMENITIES
    // =========================================================

    private void renderAmenities(
            JSONArray amenities,
            Set<Integer> selectedIds
    ) {

        if (amenitiesContainer == null) {
            return;
        }

        amenitiesContainer.removeAllViews();

        amenityCheckBoxes.clear();

        if (
                amenities == null
                        ||
                        amenities.length() == 0
        ) {

            showAmenitiesMessage(
                    "No amenities available."
            );

            return;
        }

        for (
                int i = 0;
                i < amenities.length();
                i++
        ) {

            try {

                JSONObject amenity =
                        amenities.getJSONObject(
                                i
                        );

                int amenityId =
                        amenity.optInt(
                                "amenity_id",
                                0
                        );

                String amenityName =
                        amenity.optString(
                                "amenity_name",
                                ""
                        ).trim();

                if (amenityName.isEmpty()) {

                    amenityName =
                            amenity.optString(
                                    "name",
                                    ""
                            ).trim();
                }

                if (
                        amenityId <= 0
                                ||
                                amenityName.isEmpty()
                ) {

                    continue;
                }

                CheckBox checkBox =
                        new CheckBox(
                                this
                        );

                checkBox.setText(
                        amenityName
                );

                checkBox.setTextColor(
                        Color.parseColor(
                                "#1A1A1A"
                        )
                );

                checkBox.setTextSize(
                        14
                );

                checkBox.setChecked(
                        selectedIds.contains(
                                amenityId
                        )
                );

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                params.topMargin =
                        dp(4);

                amenitiesContainer.addView(
                        checkBox,
                        params
                );

                amenityCheckBoxes.put(
                        amenityId,
                        checkBox
                );

            } catch (Exception ignored) {
            }
        }

        if (amenityCheckBoxes.isEmpty()) {

            showAmenitiesMessage(
                    "No amenities available."
            );
        }
    }

    // =========================================================
    // SAVE BUTTON
    // =========================================================

    private void setupSaveButton() {

        if (btnSaveChanges == null) {
            return;
        }

        btnSaveChanges.setOnClickListener(
                v -> saveChanges()
        );
    }

    // =========================================================
    // SAVE PROPERTY
    // =========================================================

    private void saveChanges() {

        if (houseId <= 0) {

            showToast(
                    "Invalid boarding house."
            );

            return;
        }

        if (landlordId <= 0) {

            showToast(
                    "Invalid landlord account."
            );

            return;
        }

        String houseName =
                getText(
                        editPropertyName
                );

        String address =
                getText(
                        editPropertyAddress
                );

        String description =
                getText(
                        editPropertyDescription
                );

        String houseRules =
                getText(
                        editPropertyRules
                );

        String status =
                getText(
                        editPropertyStatus
                ).toUpperCase();

        if (houseName.isEmpty()) {

            editPropertyName.setError(
                    "Boarding house name is required."
            );

            editPropertyName.requestFocus();

            return;
        }

        if (address.isEmpty()) {

            editPropertyAddress.setError(
                    "Address is required."
            );

            editPropertyAddress.requestFocus();

            return;
        }

        if (
                !"ACTIVE".equals(status)
                        &&
                        !"INACTIVE".equals(status)
        ) {

            editPropertyStatus.setError(
                    "Status must be ACTIVE or INACTIVE."
            );

            editPropertyStatus.requestFocus();

            return;
        }

        setSavingState(
                true
        );

        apiClient.updateBoardingHouse(
                houseId,
                landlordId,
                houseName,
                description,
                address,
                houseRules,
                status,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            setSavingState(
                                    false
                            );

                            showToast(
                                    "Unable to connect to server."
                            );
                        });
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        String result =
                                body;

                        runOnUiThread(() -> {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                result
                                        );

                                if (!json.optBoolean(
                                        "success",
                                        false
                                )) {

                                    setSavingState(
                                            false
                                    );

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Unable to update boarding house."
                                            )
                                    );

                                    return;
                                }

                                saveAmenityChanges();

                            } catch (Exception e) {

                                setSavingState(
                                        false
                                );

                                showToast(
                                        "Invalid server response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // SAVE AMENITY CHANGES
    // =========================================================

    private void saveAmenityChanges() {

        Set<Integer> selectedAmenityIds =
                new HashSet<>();

        for (
                Map.Entry<Integer, CheckBox> entry
                :
                amenityCheckBoxes.entrySet()
        ) {

            CheckBox checkBox =
                    entry.getValue();

            if (
                    checkBox != null
                            &&
                            checkBox.isChecked()
            ) {

                selectedAmenityIds.add(
                        entry.getKey()
                );
            }
        }

        Set<Integer> amenitiesToAdd =
                new HashSet<>(
                        selectedAmenityIds
                );

        amenitiesToAdd.removeAll(
                originalAmenityIds
        );

        Set<Integer> amenitiesToRemove =
                new HashSet<>(
                        originalAmenityIds
                );

        amenitiesToRemove.removeAll(
                selectedAmenityIds
        );

        int totalOperations =
                amenitiesToAdd.size()
                        +
                        amenitiesToRemove.size();

        if (totalOperations == 0) {

            setSavingState(
                    false
            );

            showToast(
                    "Boarding house updated successfully."
            );

            finish();

            return;
        }

        AtomicInteger remaining =
                new AtomicInteger(
                        totalOperations
                );

        AtomicBoolean failed =
                new AtomicBoolean(
                        false
                );

        Callback amenityCallback =
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        failed.set(
                                true
                        );

                        finishAmenityOperation(
                                remaining,
                                failed
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        boolean successful =
                                response.isSuccessful();

                        String body = "";

                        if (response.body() != null) {

                            body =
                                    response.body()
                                            .string();
                        }

                        if (
                                successful
                                        &&
                                        body != null
                                        &&
                                        !body.trim().isEmpty()
                        ) {

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                body
                                        );

                                if (
                                        json.has(
                                                "success"
                                        )
                                                &&
                                                !json.optBoolean(
                                                        "success",
                                                        false
                                                )
                                ) {

                                    successful =
                                            false;
                                }

                            } catch (Exception ignored) {
                            }
                        }

                        if (!successful) {

                            failed.set(
                                    true
                            );
                        }

                        finishAmenityOperation(
                                remaining,
                                failed
                        );
                    }
                };

        for (Integer amenityId : amenitiesToAdd) {

            apiClient.addBoardingHouseAmenity(
                    houseId,
                    amenityId,
                    amenityCallback
            );
        }

        for (Integer amenityId : amenitiesToRemove) {

            apiClient.removeBoardingHouseAmenity(
                    houseId,
                    amenityId,
                    amenityCallback
            );
        }
    }

    // =========================================================
    // AMENITY OPERATION COMPLETION
    // =========================================================

    private void finishAmenityOperation(
            AtomicInteger remaining,
            AtomicBoolean failed
    ) {

        if (
                remaining.decrementAndGet()
                        != 0
        ) {

            return;
        }

        runOnUiThread(() -> {

            setSavingState(
                    false
            );

            if (failed.get()) {

                showToast(
                        "Property updated, but some amenities could not be saved."
                );

                loadAmenities();

                return;
            }

            showToast(
                    "Boarding house updated successfully."
            );

            finish();
        });
    }

    // =========================================================
    // AMENITY STATES
    // =========================================================

    private void showAmenitiesLoading() {

        if (amenitiesContainer == null) {
            return;
        }

        amenitiesContainer.removeAllViews();

        TextView loading =
                new TextView(
                        this
                );

        loading.setText(
                "Loading amenities..."
        );

        loading.setTextColor(
                Color.parseColor(
                        "#9A9A9E"
                )
        );

        loading.setTextSize(
                13
        );

        loading.setPadding(
                0,
                dp(8),
                0,
                dp(8)
        );

        amenitiesContainer.addView(
                loading
        );
    }

    private void showAmenitiesMessage(
            String message
    ) {

        if (amenitiesContainer == null) {
            return;
        }

        amenitiesContainer.removeAllViews();

        TextView text =
                new TextView(
                        this
                );

        text.setText(
                message
        );

        text.setTextColor(
                Color.parseColor(
                        "#9A9A9E"
                )
        );

        text.setTextSize(
                13
        );

        text.setPadding(
                0,
                dp(8),
                0,
                dp(8)
        );

        amenitiesContainer.addView(
                text
        );
    }

    // =========================================================
    // RESPONSE ARRAY HELPER
    // =========================================================

    private JSONArray extractDataArray(
            String body
    ) throws Exception {

        if (
                body == null
                        ||
                        body.trim().isEmpty()
        ) {

            return new JSONArray();
        }

        String trimmed =
                body.trim();

        if (trimmed.startsWith("[")) {

            return new JSONArray(
                    trimmed
            );
        }

        JSONObject json =
                new JSONObject(
                        trimmed
                );

        JSONArray data =
                json.optJSONArray(
                        "data"
                );

        if (data != null) {

            return data;
        }

        return new JSONArray();
    }

    // =========================================================
    // BUTTON STATE
    // =========================================================

    private void setSavingState(
            boolean saving
    ) {

        if (btnSaveChanges == null) {
            return;
        }

        btnSaveChanges.setEnabled(
                !saving
        );

        btnSaveChanges.setText(
                saving
                        ? "Saving..."
                        : "Save Changes"
        );

        btnSaveChanges.setAlpha(
                saving
                        ? 0.6f
                        : 1.0f
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String getText(
            EditText editText
    ) {

        if (editText == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private String safeString(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dp(
            int value
    ) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
                        + 0.5f
        );
    }

    // =========================================================
    // DELETE BOARDING HOUSE
    // =========================================================

    private void setupDeleteButton() {
        TextView btnDeleteProperty = findViewById(R.id.btnDeleteProperty);
        if (btnDeleteProperty != null) {
            btnDeleteProperty.setOnClickListener(v -> {
                if (houseId <= 0) return;

                new AlertDialog.Builder(this)
                        .setTitle("Delete Boarding House?")
                        .setMessage("Are you sure you want to permanently delete '" + (propertyName.isEmpty() ? "this house" : propertyName) + "'? This will remove all its rooms, photos, amenities, and records from PostgreSQL.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> deleteHouse())
                        .show();
            });
        }
    }

    private void deleteHouse() {
        setSavingState(true);
        apiClient.deleteBoardingHouse(houseId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    setSavingState(false);
                    showToast("Failed to delete boarding house.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> setSavingState(false));
                    return;
                }
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        runOnUiThread(() -> {
                            showToast("Boarding house deleted successfully!");
                            Intent intent = new Intent(LandlordEditPropertyActivity.this, LandlordPropertiesActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        String msg = json.optString("message", "Unable to delete boarding house.");
                        runOnUiThread(() -> {
                            setSavingState(false);
                            showToast(msg);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        setSavingState(false);
                        showToast("Delete error.");
                    });
                }
            }
        });
    }
}