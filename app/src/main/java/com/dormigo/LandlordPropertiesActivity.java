package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import okhttp3.Response;

public class LandlordPropertiesActivity extends AppCompatActivity {

    private final ApiClient apiClient =
            new ApiClient();

    private LinearLayout propertiesContainer;
    private TextView propertiesCount;
    private SwipeRefreshLayout swipeRefreshLayout;

    private int landlordId;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_landlord_properties
        );

        propertiesContainer =
                findViewById(
                        R.id.propertiesContainer
                );

        propertiesCount =
                findViewById(
                        R.id.propertiesCount
                );

        swipeRefreshLayout =
                findViewById(
                        R.id.swipeRefreshLayout
                );

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadProperties);
        }

        setupWindowInsets();

        setupBackButton();

        setupAddPropertyButton();

        SharedPreferences prefs =
                getSharedPreferences(
                        "DormigoPrefs",
                        MODE_PRIVATE
                );

        landlordId =
                prefs.getInt(
                        "userId",
                        0
                );
    }

    @Override
    protected void onResume() {

        super.onResume();

        loadProperties();
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
                                            .systemBars()
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
    // ADD PROPERTY
    // =========================================================

    private void setupAddPropertyButton() {

        View btnAddProperty =
                findViewById(
                        R.id.btnAddProperty
                );

        if (btnAddProperty == null) {
            return;
        }

        btnAddProperty.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LandlordPropertiesActivity.this,
                            AddBoardingHouseActivity.class
                    );

            startActivity(intent);
        });
    }

    // =========================================================
    // LOAD LANDLORD PROPERTIES
    // =========================================================

    private void loadProperties() {

        if (landlordId <= 0) {

            showEmptyState(
                    "Please log in as a landlord to view your properties."
            );

            return;
        }

        showLoadingState();

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                showEmptyState(
                                        "Unable to load your boarding houses."
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

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            if (!json.optBoolean(
                                    "success",
                                    false
                            )) {

                                runOnUiThread(() ->
                                        showEmptyState(
                                                "Unable to load your boarding houses."
                                        )
                                );

                                return;
                            }

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            JSONArray ownedProperties =
                                    new JSONArray();

                            if (data != null) {

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject house =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int houseLandlordId =
                                            house.optInt(
                                                    "landlord_id",
                                                    0
                                            );

                                    if (
                                            houseLandlordId
                                                    == landlordId
                                    ) {

                                        ownedProperties.put(
                                                house
                                        );
                                    }
                                }
                            }

                            if (
                                    ownedProperties.length()
                                            == 0
                            ) {

                                runOnUiThread(() ->
                                        showEmptyState(
                                                "You have not listed any boarding houses yet."
                                        )
                                );

                                return;
                            }

                            loadRoomsForProperties(
                                    ownedProperties
                            );

                        } catch (Exception e) {

                            runOnUiThread(() ->
                                    showEmptyState(
                                            "Invalid boarding house response."
                                    )
                            );
                        }
                    }
                }
        );
    }

    // =========================================================
    // LOAD ROOMS
    // =========================================================

    private void loadRoomsForProperties(
            JSONArray properties
    ) {

        apiClient.getRooms(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        renderProperties(
                                properties,
                                new JSONArray()
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

                        JSONArray rooms =
                                new JSONArray();

                        try {

                            JSONObject json =
                                    new JSONObject(
                                            body
                                    );

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

                            if (data != null) {
                                rooms = data;
                            }

                        } catch (Exception ignored) {
                        }

                        renderProperties(
                                properties,
                                rooms
                        );
                    }
                }
        );
    }

    // =========================================================
    // RENDER PROPERTIES
    // =========================================================

    private void renderProperties(
            JSONArray properties,
            JSONArray rooms
    ) {

        runOnUiThread(() -> {

            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (propertiesContainer == null) {
                return;
            }

            propertiesContainer.removeAllViews();

            int propertyCount =
                    properties.length();

            if (propertiesCount != null) {

                propertiesCount.setText(
                        propertyCount == 1
                                ? "1 Listed Boarding House"
                                : propertyCount
                                  + " Listed Boarding Houses"
                );
            }

            for (
                    int i = 0;
                    i < properties.length();
                    i++
            ) {

                try {

                    JSONObject house =
                            properties.getJSONObject(
                                    i
                            );

                    createPropertyCard(
                            house,
                            rooms
                    );

                } catch (Exception ignored) {
                }
            }
        });
    }

    // =========================================================
    // CREATE PROPERTY CARD
    // =========================================================

    private void createPropertyCard(
            JSONObject house,
            JSONArray rooms
    ) {

        int houseId =
                house.optInt(
                        "house_id",
                        0
                );

        String houseName =
                house.optString(
                        "house_name",
                        "Boarding House"
                ).trim();

        String address =
                house.optString(
                        "address",
                        ""
                ).trim();

        String description =
                house.optString(
                        "description",
                        ""
                ).trim();

        String houseRules =
                house.optString(
                        "house_rules",
                        ""
                ).trim();

        String status =
                house.optString(
                        "status",
                        ""
                ).trim();

        int totalRooms = 0;
        int occupiedRooms = 0;
        int availableRooms = 0;

        for (
                int i = 0;
                i < rooms.length();
                i++
        ) {

            try {

                JSONObject room =
                        rooms.getJSONObject(
                                i
                        );

                int roomHouseId =
                        room.optInt(
                                "house_id",
                                0
                        );

                if (
                        roomHouseId
                                != houseId
                ) {

                    continue;
                }

                totalRooms++;

                String roomStatus =
                        room.optString(
                                "status",
                                ""
                        );

                if (
                        "OCCUPIED".equalsIgnoreCase(
                                roomStatus
                        )
                ) {

                    occupiedRooms++;

                } else if (
                        "AVAILABLE".equalsIgnoreCase(
                                roomStatus
                        )
                ) {

                    availableRooms++;
                }

            } catch (Exception ignored) {
            }
        }

        final int finalTotalRooms =
                totalRooms;

        final int finalOccupiedRooms =
                occupiedRooms;

        final int finalAvailableRooms =
                availableRooms;

        // =====================================================
        // MAIN GREEN CARD
        // =====================================================

        LinearLayout card =
                new LinearLayout(
                        this
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(20)
        );

        card.setBackgroundResource(
                R.drawable.bg_card_green
        );

        card.setClickable(
                true
        );

        card.setFocusable(
                true
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin =
                dp(12);

        propertiesContainer.addView(
                card,
                cardParams
        );

        // =====================================================
        // HOUSE NAME
        // =====================================================

        TextView houseNameText =
                new TextView(
                        this
                );

        houseNameText.setText(
                houseName.isEmpty()
                        ? "Boarding House"
                        : houseName
        );

        houseNameText.setTextColor(
                Color.WHITE
        );

        houseNameText.setTextSize(
                18
        );

        houseNameText.setTypeface(
                null,
                Typeface.BOLD
        );

        card.addView(
                houseNameText
        );

        // =====================================================
        // ADDRESS
        // =====================================================

        TextView addressText =
                new TextView(
                        this
                );

        addressText.setText(
                address.isEmpty()
                        ? "Address not provided"
                        : address
        );

        addressText.setTextColor(
                Color.parseColor(
                        "#A0C6BC"
                )
        );

        addressText.setTextSize(
                12
        );

        LinearLayout.LayoutParams addressParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        addressParams.topMargin =
                dp(2);

        card.addView(
                addressText,
                addressParams
        );

        // =====================================================
        // OCCUPANCY BOX
        // =====================================================

        LinearLayout occupancyBox =
                new LinearLayout(
                        this
                );

        occupancyBox.setOrientation(
                LinearLayout.VERTICAL
        );

        occupancyBox.setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
        );

        occupancyBox.setBackgroundResource(
                R.drawable.bg_occupancy_box
        );

        LinearLayout.LayoutParams occupancyParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        occupancyParams.topMargin =
                dp(16);

        card.addView(
                occupancyBox,
                occupancyParams
        );

        // =====================================================
        // OCCUPIED ROOM COUNT
        // =====================================================

        TextView occupancyText =
                new TextView(
                        this
                );

        String roomWord =
                finalTotalRooms == 1
                        ? "Room"
                        : "Rooms";

        occupancyText.setText(
                finalOccupiedRooms
                        + " of "
                        + finalTotalRooms
                        + " "
                        + roomWord
                        + " Occupied"
        );

        occupancyText.setTextColor(
                Color.WHITE
        );

        occupancyText.setTextSize(
                14
        );

        occupancyText.setTypeface(
                null,
                Typeface.BOLD
        );

        occupancyBox.addView(
                occupancyText
        );

        // =====================================================
        // AVAILABLE COUNT
        // =====================================================

        TextView availabilityText =
                new TextView(
                        this
                );

        availabilityText.setText(
                finalAvailableRooms == 1
                        ? "1 room available"
                        : finalAvailableRooms
                          + " rooms available"
        );

        availabilityText.setTextColor(
                Color.parseColor(
                        "#D7E9E1"
                )
        );

        availabilityText.setTextSize(
                12
        );

        LinearLayout.LayoutParams availabilityParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        availabilityParams.topMargin =
                dp(4);

        occupancyBox.addView(
                availabilityText,
                availabilityParams
        );

        // =====================================================
        // PROPERTY STATUS
        // =====================================================

        TextView statusText =
                new TextView(
                        this
                );

        statusText.setText(
                createStatusText(
                        status
                )
        );

        statusText.setTextColor(
                Color.parseColor(
                        "#A0C6BC"
                )
        );

        statusText.setTextSize(
                12
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        statusParams.topMargin =
                dp(4);

        occupancyBox.addView(
                statusText,
                statusParams
        );

        // =====================================================
        // OPEN PROPERTY DETAILS
        // =====================================================

        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LandlordPropertiesActivity.this,
                            LandlordPropertyDetailsActivity.class
                    );

            intent.putExtra(
                    "HOUSE_ID",
                    houseId
            );

            intent.putExtra(
                    "LANDLORD_ID",
                    landlordId
            );

            intent.putExtra(
                    "PROPERTY_NAME",
                    houseName
            );

            intent.putExtra(
                    "PROPERTY_ADDRESS",
                    address
            );

            intent.putExtra(
                    "PROPERTY_DESCRIPTION",
                    description
            );

            intent.putExtra(
                    "PROPERTY_RULES",
                    houseRules
            );

            intent.putExtra(
                    "PROPERTY_STATUS",
                    status
            );

            intent.putExtra(
                    "TOTAL_UNITS",
                    String.valueOf(
                            finalTotalRooms
                    )
            );

            intent.putExtra(
                    "OCCUPIED_UNITS",
                    String.valueOf(
                            finalOccupiedRooms
                    )
            );

            intent.putExtra(
                    "AVAILABLE_UNITS",
                    String.valueOf(
                            finalAvailableRooms
                    )
            );

            startActivity(
                    intent
            );
        });
    }

    // =========================================================
    // STATUS DISPLAY
    // =========================================================

    private String createStatusText(
            String status
    ) {

        if (
                status == null
                        || status.trim().isEmpty()
        ) {

            return "Status: Not specified";
        }

        if (
                "ACTIVE".equalsIgnoreCase(
                        status
                )
        ) {

            return "Status: Active & Accepting Inquiries";
        }

        if (
                "INACTIVE".equalsIgnoreCase(
                        status
                )
        ) {

            return "Status: Inactive";
        }

        String formatted =
                status.trim()
                        .toLowerCase();

        formatted =
                Character.toUpperCase(
                        formatted.charAt(0)
                )
                        + formatted.substring(1);

        return "Status: "
                + formatted;
    }

    // =========================================================
    // LOADING
    // =========================================================

    private void showLoadingState() {

        runOnUiThread(() -> {

            if (propertiesContainer == null) {
                return;
            }

            propertiesContainer.removeAllViews();

            if (propertiesCount != null) {

                propertiesCount.setText(
                        "Loading properties..."
                );
            }

            TextView loadingText =
                    new TextView(
                            this
                    );

            loadingText.setText(
                    "Loading your boarding houses..."
            );

            loadingText.setTextColor(
                    Color.parseColor(
                            "#9A9A9E"
                    )
            );

            loadingText.setTextSize(
                    14
            );

            loadingText.setPadding(
                    dp(4),
                    dp(24),
                    dp(4),
                    dp(24)
            );

            propertiesContainer.addView(
                    loadingText
            );
        });
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmptyState(
            String message
    ) {

        runOnUiThread(() -> {

            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (propertiesContainer == null) {
                return;
            }

            propertiesContainer.removeAllViews();

            if (propertiesCount != null) {

                propertiesCount.setText(
                        "0 Listed Boarding Houses"
                );
            }

            TextView emptyText =
                    new TextView(
                            this
                    );

            emptyText.setText(
                    message
            );

            emptyText.setTextColor(
                    Color.parseColor(
                            "#6E6E73"
                    )
            );

            emptyText.setTextSize(
                    14
            );

            emptyText.setPadding(
                    dp(4),
                    dp(32),
                    dp(4),
                    dp(32)
            );

            propertiesContainer.addView(
                    emptyText
            );
        });
    }

    // =========================================================
    // DP
    // =========================================================

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
}