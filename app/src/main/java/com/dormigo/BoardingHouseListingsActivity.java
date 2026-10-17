package com.dormigo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BoardingHouseListingsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView locationLabel;
    private FusedLocationProviderClient fusedLocationClient;

    private final ApiClient apiClient = new ApiClient();

    private final List<BoardingHouseItem> boardingHouses =
            new ArrayList<>();

    private final List<RoomItem> rooms =
            new ArrayList<>();

    private boolean housesLoaded = false;
    private boolean roomsLoaded = false;

    private String currentSearchQuery = "";
    private String currentMinBudget = "";
    private String currentMaxBudget = "";
    private String currentRoomType = "";
    private SwipeRefreshLayout swipeRefreshLayout;

    private final Set<String> selectedFilterAmenities = new HashSet<>();
    private static final String[] EXPANDED_AMENITIES = new String[]{
            "Wi-Fi", "Air Conditioning", "Electric Fan", "Private Bathroom",
            "Shared Bathroom", "Hot Shower", "Kitchen Access", "Refrigerator",
            "Drinking Water", "Study Table", "Chair", "Cabinet / Closet",
            "Laundry Area", "Parking", "CCTV", "24/7 Security",
            "Visitors Allowed", "Pet Friendly", "Balcony", "Generator Backup"
    };

    // ---------------------------------------------------------
    // Boarding House model
    // ---------------------------------------------------------

    private static class BoardingHouseItem {

        int houseId;
        int landlordId;
        String houseName;
        String description;
        String address;
        String houseRules;
        String status;
        String firstPhoto = "";
        Set<String> houseAmenities = new HashSet<>();
    }

    // ---------------------------------------------------------
    // Room model
    // ---------------------------------------------------------

    private static class RoomItem {

        int roomId;
        int houseId;
        String roomNumber;
        String roomType;
        int capacity;
        double monthlyRent;
        String status;
    }

    // ---------------------------------------------------------
    // Activity
    // ---------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.boarding_house_listings);

        // System bar handling
        View mainLayout = findViewById(R.id.mainLayout);

        if (mainLayout != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    mainLayout,
                    (v, insets) -> {

                        Insets systemBars =
                                insets.getInsets(
                                        WindowInsetsCompat.Type.systemBars()
                                );

                        v.setPadding(
                                systemBars.left,
                                systemBars.top,
                                systemBars.right,
                                0
                        );

                        View bottomNav =
                                findViewById(R.id.bottomNav);

                        if (bottomNav != null) {

                            bottomNav.setPadding(
                                    0,
                                    0,
                                    0,
                                    systemBars.bottom
                            );
                        }

                        return insets;
                    }
            );
        }

        // Location
        locationLabel =
                findViewById(R.id.locationLabel);

        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        if (hasLocationPermission()) {

            fetchCurrentLocationLabel();

        } else {

            requestLocationPermission();
        }

        // Existing UI functions
        setupBottomNavigation();
        setupSearchFunctionality();
        setupClickListeners();
        handleIntent();

        // Load actual database data
        loadBoardingHouses();
        loadRooms();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadBoardingHouses();
                loadRooms();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBoardingHouses();
        loadRooms();
    }

    // ---------------------------------------------------------
    // Load Boarding Houses
    // ---------------------------------------------------------

    private void loadBoardingHouses() {

        apiClient.getAvailableBoardingHouses(new Callback() {

            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {

                runOnUiThread(() -> {

                    housesLoaded = false;

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    showToast(
                            "Unable to load boarding houses."
                    );
                });
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {

                String responseBody = "";

                if (response.body() != null) {

                    responseBody =
                            response.body().string();
                }

                String finalResponseBody =
                        responseBody;

                runOnUiThread(() -> {

                    try {

                        JSONObject json =
                                new JSONObject(
                                        finalResponseBody
                                );

                        boolean success =
                                json.optBoolean(
                                        "success",
                                        false
                                );

                        if (!success) {

                            housesLoaded = false;

                            showToast(
                                    "Failed to load boarding houses."
                            );

                            return;
                        }

                        JSONArray data =
                                json.optJSONArray("data");

                        boardingHouses.clear();

                        if (data != null) {

                            for (int i = 0;
                                 i < data.length();
                                 i++) {

                                JSONObject object =
                                        data.getJSONObject(i);

                                BoardingHouseItem house =
                                        new BoardingHouseItem();

                                house.houseId =
                                        object.optInt(
                                                "house_id",
                                                0
                                        );

                                house.landlordId =
                                        object.optInt(
                                                "landlord_id",
                                                0
                                        );

                                house.houseName =
                                        object.optString(
                                                "house_name",
                                                ""
                                        );

                                house.description =
                                        object.optString(
                                                "description",
                                                ""
                                        );

                                house.address =
                                        object.optString(
                                                "address",
                                                ""
                                        );

                                house.houseRules =
                                        object.optString(
                                                "house_rules",
                                                ""
                                        );

                                house.status =
                                        object.optString(
                                                "status",
                                                ""
                                        );

                                Object pObj = object.opt("photo_paths");
                                if (pObj instanceof JSONArray) {
                                    JSONArray photosArr = (JSONArray) pObj;
                                    if (photosArr.length() > 0) {
                                        house.firstPhoto = photosArr.optString(0, "");
                                    }
                                } else if (pObj instanceof String) {
                                    String str = (String) pObj;
                                    str = str.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim();
                                    String[] parts = str.split(",");
                                    if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                                        house.firstPhoto = parts[0].trim();
                                    }
                                }

                                boardingHouses.add(
                                        house
                                );
                                loadHouseAmenities(house);
                            }
                        }

                        housesLoaded = true;

                        renderListings();

                    } catch (Exception e) {

                        housesLoaded = false;

                        showToast(
                                "Invalid boarding house response."
                        );
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------
    // Load Rooms
    // ---------------------------------------------------------

    private void loadRooms() {

        apiClient.getRooms(new Callback() {

            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {

                runOnUiThread(() -> {

                    roomsLoaded = false;

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    showToast(
                            "Unable to load rooms."
                    );
                });
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {

                String responseBody = "";

                if (response.body() != null) {

                    responseBody =
                            response.body().string();
                }

                String finalResponseBody =
                        responseBody;

                runOnUiThread(() -> {

                    try {

                        JSONObject json =
                                new JSONObject(
                                        finalResponseBody
                                );

                        boolean success =
                                json.optBoolean(
                                        "success",
                                        false
                                );

                        if (!success) {

                            roomsLoaded = false;

                            showToast(
                                    "Failed to load rooms."
                            );

                            return;
                        }

                        JSONArray data =
                                json.optJSONArray("data");

                        rooms.clear();

                        if (data != null) {

                            for (int i = 0;
                                 i < data.length();
                                 i++) {

                                JSONObject object =
                                        data.getJSONObject(i);

                                RoomItem room =
                                        new RoomItem();

                                room.roomId =
                                        object.optInt(
                                                "room_id",
                                                0
                                        );

                                room.houseId =
                                        object.optInt(
                                                "house_id",
                                                0
                                        );

                                room.roomNumber =
                                        object.optString(
                                                "room_number",
                                                ""
                                        );

                                room.roomType =
                                        object.optString(
                                                "room_type",
                                                ""
                                        );

                                room.capacity =
                                        object.optInt(
                                                "capacity",
                                                0
                                        );

                                room.monthlyRent =
                                        object.optDouble(
                                                "monthly_rent",
                                                0
                                        );

                                room.status =
                                        object.optString(
                                                "status",
                                                ""
                                        );

                                rooms.add(room);
                            }
                        }

                        roomsLoaded = true;

                        renderListings();

                    } catch (Exception e) {

                        roomsLoaded = false;

                        showToast(
                                "Invalid room response."
                        );
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------
    // Render listings
    // ---------------------------------------------------------

    private void renderListings() {

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        if (!housesLoaded || !roomsLoaded) {
            return;
        }

        View[] cards = {
                findViewById(R.id.cardListing1),
                findViewById(R.id.cardListing2),
                findViewById(R.id.cardListing3),
                findViewById(R.id.cardListing4)
        };

        TextView[] houseNames = {
                findViewById(R.id.houseName1),
                findViewById(R.id.houseName2),
                findViewById(R.id.houseName3),
                findViewById(R.id.houseName4)
        };

        TextView[] priceLabels = {
                findViewById(R.id.priceLabel1),
                findViewById(R.id.priceLabel2),
                findViewById(R.id.priceLabel3),
                findViewById(R.id.priceLabel4)
        };

        ImageView[] houseImages = {
                findViewById(R.id.houseImage1),
                findViewById(R.id.houseImage2),
                findViewById(R.id.houseImage3),
                findViewById(R.id.houseImage4)
        };

        View[] inquireButtons = {
                findViewById(R.id.btnInquire1),
                findViewById(R.id.btnInquire2),
                findViewById(R.id.btnInquire3),
                findViewById(R.id.btnInquire4)
        };

        TextView[] ratingLabels = {
                findViewById(R.id.ratingLabel1),
                findViewById(R.id.ratingLabel2),
                findViewById(R.id.ratingLabel3),
                findViewById(R.id.ratingLabel4)
        };

        // Hide all cards first
        for (View card : cards) {

            if (card != null) {
                card.setVisibility(View.GONE);
            }
        }

        int count =
                Math.min(
                        boardingHouses.size(),
                        4
                );

        // Fill available cards
        for (int i = 0;
             i < count;
             i++) {

            final int index = i;

            BoardingHouseItem house =
                    boardingHouses.get(index);

            RoomItem room =
                    getRoomForHouse(
                            house.houseId
                    );

            if (cards[index] != null) {

                cards[index].setVisibility(
                        View.VISIBLE
                );
            }

            if (houseNames[index] != null) {

                houseNames[index].setText(
                        house.houseName
                );
            }

            if (priceLabels[index] != null) {

                priceLabels[index].setText(
                        buildRoomSummary(room)
                );
            }

            if (houseImages[index] != null) {
                if (house.firstPhoto != null && !house.firstPhoto.isEmpty()) {
                    String imgUrl = "http://172.20.10.3/Dormigo_Backend/" + house.firstPhoto;
                    Glide.with(this)
                            .load(imgUrl)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(houseImages[index]);
                } else {
                    houseImages[index].setImageResource(R.drawable.bg_image_placeholder);
                }
            }

            if (inquireButtons[index] != null) {
                inquireButtons[index].setOnClickListener(v -> setupCardClick(cards[index], house, room));
            }

            if (ratingLabels[index] != null) {
                final TextView ratingTv = ratingLabels[index];
                apiClient.getReviewsForHouse(house.houseId, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) return;
                        try {
                            JSONObject rJson = new JSONObject(response.body().string());
                            if (rJson.optBoolean("success", false)) {
                                double avg = rJson.optDouble("average_rating", 0.0);
                                int count = rJson.optInt("review_count", 0);
                                runOnUiThread(() -> {
                                    if (ratingTv != null) {
                                        if (count > 0) {
                                            ratingTv.setText("★ " + String.format(Locale.US, "%.1f", avg));
                                        } else {
                                            ratingTv.setText("No ratings yet");
                                        }
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }

            setupCardClick(
                    cards[index],
                    house,
                    room
            );
        }

        applyAllCurrentFilters();
    }

    // ---------------------------------------------------------
    // Find room belonging to house
    // ---------------------------------------------------------

    private RoomItem getRoomForHouse(
            int houseId
    ) {

        RoomItem availableRoom = null;

        for (RoomItem room : rooms) {

            if (room.houseId != houseId) {
                continue;
            }

            if (!"AVAILABLE".equalsIgnoreCase(
                    room.status
            )) {
                continue;
            }

            if (availableRoom == null
                    || room.monthlyRent
                    < availableRoom.monthlyRent) {

                availableRoom = room;
            }
        }

        // Return only an actually available room.
        // If every room is occupied/unavailable, return null so
        // the listing can clearly show that no room is available.
        return availableRoom;
    }

    // ---------------------------------------------------------
    // Display room information
    // ---------------------------------------------------------

    private String buildRoomSummary(
            RoomItem room
    ) {

        if (room == null) {

            return "No available rooms";
        }

        String rent =
                formatRent(
                        room.monthlyRent
                );

        String type =
                room.roomType == null
                        ? ""
                        : room.roomType.trim();

        String capacity =
                room.capacity > 0
                        ? " • "
                          + room.capacity
                          + " pax"
                        : "";

        String status =
                room.status == null
                        ? ""
                        : room.status.trim();

        return rent
                + (type.isEmpty()
                ? ""
                : " • " + type)
                + capacity
                + (status.isEmpty()
                ? ""
                : " • " + status);
    }

    private String formatRent(
            double amount
    ) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.US
                );

        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);

        return "₱"
                + formatter.format(amount)
                + "/mo";
    }

    // ---------------------------------------------------------
    // Card click
    // ---------------------------------------------------------

    private void setupCardClick(
            View card,
            BoardingHouseItem house,
            RoomItem room
    ) {

        if (card == null) {
            return;
        }

        card.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            ViewBoardingHouseActivity.class
                    );

            intent.putExtra(
                    "HOUSE_ID",
                    house.houseId
            );

            intent.putExtra(
                    "LANDLORD_ID",
                    house.landlordId
            );

            intent.putExtra(
                    "HOUSE_NAME",
                    house.houseName
            );

            intent.putExtra(
                    "HOUSE_DESCRIPTION",
                    house.description
            );

            intent.putExtra(
                    "HOUSE_ADDRESS",
                    house.address
            );

            intent.putExtra(
                    "HOUSE_RULES",
                    house.houseRules
            );

            intent.putExtra(
                    "HOUSE_STATUS",
                    house.status
            );

            if (room != null) {

                intent.putExtra(
                        "ROOM_ID",
                        room.roomId
                );

                intent.putExtra(
                        "ROOM_NUMBER",
                        room.roomNumber
                );

                intent.putExtra(
                        "ROOM_TYPE",
                        room.roomType
                );

                intent.putExtra(
                        "ROOM_CAPACITY",
                        room.capacity
                );

                intent.putExtra(
                        "MONTHLY_RENT",
                        room.monthlyRent
                );

                intent.putExtra(
                        "ROOM_STATUS",
                        room.status
                );

                intent.putExtra(
                        "RENT_PRICE",
                        formatRent(
                                room.monthlyRent
                        )
                );
            }

            startActivity(intent);

            overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
            );
        });
    }

    // ---------------------------------------------------------
    // Search
    // ---------------------------------------------------------

    private void setupSearchFunctionality() {

        EditText searchInput =
                findViewById(
                        R.id.searchInput
                );

        if (searchInput == null) {
            return;
        }

        searchInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        performSearch(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        searchInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEARCH
                            || actionId ==
                            EditorInfo.IME_ACTION_DONE) {

                        performSearch(
                                v.getText().toString()
                        );

                        return true;
                    }

                    return false;
                }
        );
    }

    private void performSearch(
            String query
    ) {

        currentSearchQuery =
                query.trim();

        applyAllCurrentFilters();
    }

    // ---------------------------------------------------------
    // Search + filters
    // ---------------------------------------------------------

    private void applyAllCurrentFilters() {

        if (!housesLoaded || !roomsLoaded) {
            return;
        }

        View[] cards = {
                findViewById(R.id.cardListing1),
                findViewById(R.id.cardListing2),
                findViewById(R.id.cardListing3),
                findViewById(R.id.cardListing4)
        };

        int count =
                Math.min(
                        boardingHouses.size(),
                        4
                );

        int minValue = 0;
        int maxValue = Integer.MAX_VALUE;

        try {

            if (!currentMinBudget.isEmpty()) {

                minValue =
                        Integer.parseInt(
                                currentMinBudget
                        );
            }

            if (!currentMaxBudget.isEmpty()) {

                maxValue =
                        Integer.parseInt(
                                currentMaxBudget
                        );
            }

        } catch (NumberFormatException e) {

            return;
        }

        String search =
                currentSearchQuery
                        .toLowerCase(
                                Locale.ROOT
                        );

        for (int i = 0;
             i < count;
             i++) {

            BoardingHouseItem house =
                    boardingHouses.get(i);

            RoomItem room =
                    getRoomForHouse(
                            house.houseId
                    );

            boolean searchMatch =
                    search.isEmpty()
                            || contains(
                            house.houseName,
                            search
                    )
                            || contains(
                            house.address,
                            search
                    )
                            || contains(
                            house.description,
                            search
                    )
                            || contains(
                            house.houseRules,
                            search
                    );

            boolean priceMatch = true;

            if (room != null) {

                priceMatch =
                        room.monthlyRent >= minValue
                                && room.monthlyRent <= maxValue;

            } else {

                if (!currentMinBudget.isEmpty()
                        || !currentMaxBudget.isEmpty()) {

                    priceMatch = false;
                }
            }

            boolean typeMatch =
                    currentRoomType.isEmpty()
                            || roomMatchesType(
                            room,
                            currentRoomType
                    );

            boolean amenityMatch = true;
            if (!selectedFilterAmenities.isEmpty()) {
                for (String reqAmenity : selectedFilterAmenities) {
                    if (!houseContainsAmenity(house, reqAmenity)) {
                        amenityMatch = false;
                        break;
                    }
                }
            }

            boolean visible =
                    searchMatch
                            && priceMatch
                            && typeMatch
                            && amenityMatch;

            if (cards[i] != null) {

                cards[i].setVisibility(
                        visible
                                ? View.VISIBLE
                                : View.GONE
                );
            }
        }

        for (int i = count;
             i < cards.length;
             i++) {

            if (cards[i] != null) {

                cards[i].setVisibility(
                        View.GONE
                );
            }
        }
    }

    private boolean contains(
            String value,
            String query
    ) {

        if (value == null) {
            return false;
        }

        return value
                .toLowerCase(
                        Locale.ROOT
                )
                .contains(query);
    }

    private boolean roomMatchesType(
            RoomItem room,
            String type
    ) {

        if (room == null || room.roomType == null) {
            return false;
        }

        String rType = room.roomType.trim().toLowerCase(Locale.ROOT);
        String filterType = type.trim().toLowerCase(Locale.ROOT);

        if (filterType.contains("solo") && (rType.contains("solo") || rType.contains("single"))) {
            return true;
        }
        if (filterType.contains("shared") && rType.contains("shared")) {
            return true;
        }
        if (filterType.contains("bedspace") && rType.contains("bedspace")) {
            return true;
        }
        if (filterType.contains("dormitory") && rType.contains("dormitory")) {
            return true;
        }
        if (filterType.contains("studio") && rType.contains("studio")) {
            return true;
        }
        if (filterType.contains("apartment") && rType.contains("apartment")) {
            return true;
        }

        return rType.contains(filterType);
    }

    // ---------------------------------------------------------
    // Intent
    // ---------------------------------------------------------

    private void handleIntent() {

        Intent intent = getIntent();

        if (intent != null
                && intent.hasExtra(
                "SEARCH_QUERY"
        )) {

            String query =
                    intent.getStringExtra(
                            "SEARCH_QUERY"
                    );

            EditText searchInput =
                    findViewById(
                            R.id.searchInput
                    );

            if (searchInput != null
                    && query != null) {

                searchInput.setText(query);
                performSearch(query);
            }
        }
    }

    // ---------------------------------------------------------
    // Bottom navigation
    // ---------------------------------------------------------

    private void setupBottomNavigation() {

        BottomNavigationView bottomNav =
                findViewById(
                        R.id.bottomNav
                );

        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(
                R.id.nav_explore
        );

        bottomNav.setOnItemSelectedListener(
                item -> {

                    int id =
                            item.getItemId();

                    if (id ==
                            R.id.nav_home) {

                        startActivity(
                                new Intent(
                                        this,
                                        HomeActivity.class
                                )
                        );

                        finish();

                        overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                        );

                        return true;

                    } else if (id ==
                            R.id.nav_profile) {

                        startActivity(
                                new Intent(
                                        this,
                                        ProfileActivity.class
                                )
                        );

                        finish();

                        overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                        );

                        return true;

                    } else if (id ==
                            R.id.nav_chats) {

                        startActivity(
                                new Intent(
                                        this,
                                        ChatHistoryActivity.class
                                )
                        );

                        finish();

                        overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                        );

                        return true;

                    } else if (id ==
                            R.id.nav_requests) {

                        startActivity(
                                new Intent(
                                        this,
                                        BookingRequestsActivity.class
                                )
                        );

                        finish();

                        overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                        );

                        return true;
                    }

                    return id ==
                            R.id.nav_explore;
                }
        );
    }

    // ---------------------------------------------------------
    // Click listeners
    // ---------------------------------------------------------

    private void setupClickListeners() {

        View btnFilter =
                findViewById(
                        R.id.btnFilter
                );

        if (btnFilter != null) {

            btnFilter.setOnClickListener(
                    v -> showFilterBottomSheet()
            );
        }

        setupCategoryClickListeners();
    }

    // ---------------------------------------------------------
    // Categories
    // ---------------------------------------------------------

    private void setupCategoryClickListeners() {

        int[] categoryIds = {
                R.id.btnCatAll,
                R.id.btnCatBedspace,
                R.id.btnCatShared,
                R.id.btnCatSolo,
                R.id.btnCatDormitory,
                R.id.btnCatStudio,
                R.id.btnCatApartment
        };

        for (int id : categoryIds) {

            View button =
                    findViewById(id);

            if (button == null) {
                continue;
            }

            button.setOnClickListener(v -> {

                for (int otherId : categoryIds) {

                    View other =
                            findViewById(
                                    otherId
                            );

                    if (other != null) {

                        other.setBackgroundResource(
                                R.drawable.bg_chip
                        );

                        if (other instanceof TextView) {

                            ((TextView) other)
                                    .setTextColor(
                                            0xFF6E6E73
                                    );
                        }
                    }
                }

                v.setBackgroundResource(
                        R.drawable.bg_button_filled
                );

                if (v instanceof TextView) {

                    ((TextView) v)
                            .setTextColor(
                                    0xFFFFFFFF
                            );
                }

                if (id ==
                        R.id.btnCatAll) {

                    currentRoomType = "";

                } else if (id ==
                        R.id.btnCatBedspace) {

                    currentRoomType = "Bedspace";

                } else if (id ==
                        R.id.btnCatShared) {

                    currentRoomType = "Shared room";

                } else if (id ==
                        R.id.btnCatSolo) {

                    currentRoomType = "Solo room";

                } else if (id ==
                        R.id.btnCatDormitory) {

                    currentRoomType = "Dormitory Type";

                } else if (id ==
                        R.id.btnCatStudio) {

                    currentRoomType = "Studio Type";

                } else if (id ==
                        R.id.btnCatApartment) {

                    currentRoomType = "Apartment Type";
                }

                applyAllCurrentFilters();
            });
        }
    }

    // ---------------------------------------------------------
    // Filter bottom sheet
    // ---------------------------------------------------------

    private void showFilterBottomSheet() {

        BottomSheetDialog dialog =
                new BottomSheetDialog(this);

        View view =
                getLayoutInflater().inflate(
                        R.layout.filter_bottom_sheet,
                        findViewById(
                                R.id.mainLayout
                        ),
                        false
                );

        dialog.setContentView(view);

        View btnClose =
                view.findViewById(
                        R.id.btnCloseFilter
                );

        View btnApply =
                view.findViewById(
                        R.id.btnApplyFilters
                );

        View btnReset =
                view.findViewById(
                        R.id.btnResetFilters
                );

        EditText minInput =
                view.findViewById(
                        R.id.minBudgetInput
                );

        EditText maxInput =
                view.findViewById(
                        R.id.maxBudgetInput
                );

        if (btnClose != null) {

            btnClose.setOnClickListener(
                    v -> dialog.dismiss()
            );
        }

        Runnable updateFilters = () -> {

            currentMinBudget =
                    minInput == null
                            ? ""
                            : minInput.getText()
                            .toString()
                            .trim();

            currentMaxBudget =
                    maxInput == null
                            ? ""
                            : maxInput.getText()
                            .toString()
                            .trim();

            String selectedType =
                    getSelectedChipText(
                            view,
                            R.id.chipSingle,
                            R.id.chipShared,
                            R.id.chipStudio
                    );

            currentRoomType =
                    selectedType == null
                            ? ""
                            : selectedType.trim();

            applyAllCurrentFilters();
        };

        TextWatcher watcher =
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateFilters.run();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                };

        if (minInput != null) {

            minInput.addTextChangedListener(
                    watcher
            );
        }

        if (maxInput != null) {

            maxInput.addTextChangedListener(
                    watcher
            );
        }

        if (btnApply != null) {

            btnApply.setOnClickListener(v -> {

                updateFilters.run();

                showToast(
                        "Filters applied"
                );

                dialog.dismiss();
            });
        }

        if (btnReset != null) {

            btnReset.setOnClickListener(v -> {

                if (minInput != null) {
                    minInput.setText("");
                }

                if (maxInput != null) {
                    maxInput.setText("");
                }

                currentMinBudget = "";
                currentMaxBudget = "";
                currentRoomType = "";
                selectedFilterAmenities.clear();

                int[] chipIds = {
                        R.id.chipSingle,
                        R.id.chipShared,
                        R.id.chipStudio,
                        R.id.chip500m,
                        R.id.chip1km,
                        R.id.chipAnyDistance
                };

                for (int id : chipIds) {

                    View chip =
                            view.findViewById(id);

                    if (chip != null) {

                        chip.setSelected(false);

                        if (chip instanceof TextView) {

                            ((TextView) chip)
                                    .setTextColor(
                                            0xFF1A1A1A
                                    );
                        }
                    }
                }

                populateAmenitiesFilterContainer(view, updateFilters);
                applyAllCurrentFilters();

                showToast(
                        "Filters reset"
                );
            });
        }

        setupSingleChipSelection(
                view,
                updateFilters,
                R.id.chipSingle,
                R.id.chipShared,
                R.id.chipStudio
        );

        setupSingleChipSelection(
                view,
                updateFilters,
                R.id.chip500m,
                R.id.chip1km,
                R.id.chipAnyDistance
        );

        populateAmenitiesFilterContainer(view, updateFilters);

        dialog.show();
    }

    private void populateAmenitiesFilterContainer(View filterView, Runnable updateFilters) {
        LinearLayout container = filterView.findViewById(R.id.filterAmenitiesContainer);
        if (container == null) return;
        container.removeAllViews();

        LinearLayout currentRow = null;
        for (int i = 0; i < EXPANDED_AMENITIES.length; i++) {
            final String amenityName = EXPANDED_AMENITIES[i];

            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rParams.bottomMargin = dp(8);
                currentRow.setLayoutParams(rParams);
                container.addView(currentRow);
            }

            TextView chip = new TextView(this);
            LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i % 2 == 1) cParams.setMarginStart(dp(8));
            chip.setLayoutParams(cParams);
            chip.setGravity(Gravity.CENTER);
            chip.setText(amenityName);
            chip.setTextSize(12);

            boolean isSelected = selectedFilterAmenities.contains(amenityName.toLowerCase(Locale.ROOT));
            chip.setSelected(isSelected);
            chip.setBackgroundResource(R.drawable.bg_chip_selectable);
            chip.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#1A1A1A"));

            chip.setOnClickListener(v -> {
                boolean newSel = !chip.isSelected();
                chip.setSelected(newSel);
                chip.setTextColor(newSel ? Color.WHITE : Color.parseColor("#1A1A1A"));
                if (newSel) {
                    selectedFilterAmenities.add(amenityName.toLowerCase(Locale.ROOT));
                } else {
                    selectedFilterAmenities.remove(amenityName.toLowerCase(Locale.ROOT));
                }
                if (updateFilters != null) updateFilters.run();
            });

            if (currentRow != null) currentRow.addView(chip);
        }
    }

    private void loadHouseAmenities(BoardingHouseItem house) {
        if (house == null || house.houseId <= 0) return;
        apiClient.getBoardingHouseAmenities(house.houseId, new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    String body = response.body().string();
                    JSONArray arr = extractDataArray(body);
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            if (obj != null) {
                                String aName = obj.optString("amenity_name", "").trim();
                                if (!aName.isEmpty()) {
                                    house.houseAmenities.add(aName.toLowerCase(Locale.ROOT));
                                }
                            }
                        }
                    }
                    runOnUiThread(() -> applyAllCurrentFilters());
                } catch (Exception ignored) {}
            }
        });
    }

    private boolean houseContainsAmenity(BoardingHouseItem house, String requiredAmenity) {
        if (house == null || house.houseAmenities == null || requiredAmenity == null) return false;
        String req = requiredAmenity.toLowerCase(Locale.ROOT).trim();
        for (String a : house.houseAmenities) {
            if (a.contains(req) || req.contains(a)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------
    // Get selected chip
    // ---------------------------------------------------------

    private String getSelectedChipText(
            View parent,
            int... ids
    ) {

        for (int id : ids) {

            View chip =
                    parent.findViewById(id);

            if (chip != null
                    && chip.isSelected()
                    && chip instanceof TextView) {

                return ((TextView) chip)
                        .getText()
                        .toString();
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // Single selection chips
    // ---------------------------------------------------------

    private void setupSingleChipSelection(
            View parent,
            Runnable callback,
            int... ids
    ) {

        for (int id : ids) {

            View chip =
                    parent.findViewById(id);

            if (chip == null) {
                continue;
            }

            chip.setOnClickListener(v -> {

                for (int otherId : ids) {

                    View other =
                            parent.findViewById(
                                    otherId
                            );

                    if (other != null) {

                        other.setSelected(false);

                        if (other instanceof TextView) {

                            ((TextView) other)
                                    .setTextColor(
                                            0xFF1A1A1A
                                    );
                        }
                    }
                }

                v.setSelected(true);

                if (v instanceof TextView) {

                    ((TextView) v)
                            .setTextColor(
                                    0xFFFFFFFF
                            );
                }

                if (callback != null) {
                    callback.run();
                }
            });
        }
    }

    // ---------------------------------------------------------
    // Multiple selection chips
    // ---------------------------------------------------------

    private void setupMultiChipSelection(
            View parent,
            Runnable callback,
            int... ids
    ) {

        for (int id : ids) {

            View chip =
                    parent.findViewById(id);

            if (chip == null) {
                continue;
            }

            chip.setOnClickListener(v -> {

                v.setSelected(
                        !v.isSelected()
                );

                if (v instanceof TextView) {

                    ((TextView) v)
                            .setTextColor(
                                    v.isSelected()
                                            ? 0xFFFFFFFF
                                            : 0xFF1A1A1A
                            );
                }

                if (callback != null) {
                    callback.run();
                }
            });
        }
    }

    // ---------------------------------------------------------
    // Location
    // ---------------------------------------------------------

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @SuppressWarnings("MissingPermission")
    private void fetchCurrentLocationLabel() {

        if (locationLabel != null) {

            locationLabel.setText(
                    R.string.getting_location
            );
        }

        CancellationTokenSource cts =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.getToken()
                )
                .addOnSuccessListener(
                        this,
                        location -> {

                            if (location != null) {

                                updateLocationLabel(
                                        location
                                );

                            } else if (
                                    locationLabel != null
                            ) {

                                locationLabel.setText(
                                        R.string.location_unavailable
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        this,
                        e -> {

                            if (locationLabel != null) {

                                locationLabel.setText(
                                        R.string.location_unavailable
                                );
                            }
                        }
                );
    }

    private void updateLocationLabel(
            Location location
    ) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            if (
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.TIRAMISU
            ) {

                geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1,
                        addresses -> {

                            if (!addresses.isEmpty()) {

                                Address address =
                                        addresses.get(0);

                                runOnUiThread(() ->
                                        locationLabel.setText(
                                                buildNearestPlaceLabel(
                                                        address
                                                )
                                        )
                                );

                            } else if (
                                    locationLabel != null
                            ) {

                                runOnUiThread(() ->
                                        locationLabel.setText(
                                                R.string.location_unavailable
                                        )
                                );
                            }
                        }
                );

            } else {

                List<Address> addresses =
                        geocoder.getFromLocation(
                                location.getLatitude(),
                                location.getLongitude(),
                                1
                        );

                if (addresses != null
                        && !addresses.isEmpty()) {

                    Address address =
                            addresses.get(0);

                    if (locationLabel != null) {

                        locationLabel.setText(
                                buildNearestPlaceLabel(
                                        address
                                )
                        );
                    }

                } else if (
                        locationLabel != null
                ) {

                    locationLabel.setText(
                            R.string.location_unavailable
                    );
                }
            }

        } catch (Exception e) {

            if (locationLabel != null) {

                locationLabel.setText(
                        R.string.location_unavailable
                );
            }
        }
    }

    private String buildNearestPlaceLabel(
            Address address
    ) {

        String subLocality =
                address.getSubLocality();

        String locality =
                address.getLocality();

        StringBuilder result =
                new StringBuilder();

        if (subLocality != null
                && !subLocality.isEmpty()) {

            result.append(subLocality);
        }

        if (locality != null
                && !locality.isEmpty()) {

            if (result.length() > 0) {

                result.append(", ");
            }

            result.append(locality);
        }

        String place =
                result.length() > 0
                        ? result.toString()
                        : getString(
                        R.string.your_location
                );

        return getString(
                R.string.near_location,
                place
        );
    }

    // ---------------------------------------------------------
    // Permission result
    // ---------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                fetchCurrentLocationLabel();

            } else if (
                    locationLabel != null
            ) {

                locationLabel.setText(
                        R.string.location_permission_needed
                );
            }
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private JSONArray extractDataArray(String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            return new JSONArray();
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject json = new JSONObject(trimmed);
        JSONArray data = json.optJSONArray("data");
        if (data != null) {
            return data;
        }
        return new JSONArray();
    }
}
