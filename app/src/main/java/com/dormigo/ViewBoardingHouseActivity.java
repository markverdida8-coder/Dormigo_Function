package com.dormigo;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ViewBoardingHouseActivity extends AppCompatActivity {

    private final ApiClient apiClient = new ApiClient();

    private String houseName = "";
    private String houseAddress = "";
    private String houseDescription = "";
    private String houseRules = "";
    private String houseStatus = "";
    private String landlordName = "Landlord";

    private int houseId;
    private int landlordId;

    private int roomId;
    private String roomNumber = "";
    private String roomType = "";
    private int roomCapacity;
    private double monthlyRent;
    private String roomStatus = "";

    private final List<RoomItem> houseRooms = new ArrayList<>();
    private int selectedAdvanceMonths = 1;
    private int selectedDepositMonths = 1;
    private double selectedUtilityDeposit = 0;
    private double selectedOtherFees = 0;
    private String selectedOtherFeesDesc = "";
    private String selectedRefundPolicy = "";
    private double lastCalculatedTotalInitial = 0.0;

    private static class RoomItem {
        int roomId;
        int houseId;
        String roomNumber;
        String roomType;
        int capacity;
        double monthlyRent;
        String status;
        int advanceMonths = 1;
        int depositMonths = 1;
        double utilityDeposit = 0;
        double otherFees = 0;
        String otherFeesDescription = "";
        String depositRefundPolicy = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.view_boarding_houses);

        setupWindowInsets();
        readIntentData();
        setupClickListeners();
        loadLandlordName();
        loadRoomsForHouse();
        loadAmenitiesForHouse();
        loadReviewsForHouse();
        loadHousePhotos();
        loadStaticMap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (houseId > 0) {
            loadRoomsForHouse();
            loadReviewsForHouse();
            loadHousePhotos();
        }
    }

    private void setupWindowInsets() {

        View mainLayout =
                findViewById(R.id.mainLayout);

        if (mainLayout == null) {
            return;
        }

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

                    View bottomActions =
                            findViewById(
                                    R.id.bottomActions
                            );

                    if (bottomActions != null) {
                        bottomActions.setPadding(
                                bottomActions.getPaddingLeft(),
                                bottomActions.getPaddingTop(),
                                bottomActions.getPaddingRight(),
                                systemBars.bottom
                        );
                    }

                    return insets;
                }
        );
    }

    private void readIntentData() {

        Intent intent = getIntent();

        houseId = intent.getIntExtra("HOUSE_ID", 0);
        landlordId = intent.getIntExtra("LANDLORD_ID", 0);

        houseName = intent.getStringExtra("HOUSE_NAME");
        houseAddress = intent.getStringExtra("HOUSE_ADDRESS");
        houseDescription = intent.getStringExtra("HOUSE_DESCRIPTION");
        houseRules = intent.getStringExtra("HOUSE_RULES");
        houseStatus = intent.getStringExtra("HOUSE_STATUS");

        roomId = intent.getIntExtra("ROOM_ID", 0);
        roomNumber = intent.getStringExtra("ROOM_NUMBER");
        roomType = intent.getStringExtra("ROOM_TYPE");
        roomCapacity = intent.getIntExtra("ROOM_CAPACITY", 0);
        monthlyRent = intent.getDoubleExtra("MONTHLY_RENT", 0);
        roomStatus = intent.getStringExtra("ROOM_STATUS");

        if (houseName == null || houseName.trim().isEmpty()) {
            houseName = "Boarding House";
        }

        if (houseAddress == null) {
            houseAddress = "";
        }

        if (houseDescription == null) {
            houseDescription = "";
        }

        if (houseRules == null) {
            houseRules = "";
        }

        if (houseStatus == null) {
            houseStatus = "";
        }

        if (roomNumber == null) {
            roomNumber = "";
        }

        if (roomType == null) {
            roomType = "";
        }

        if (roomStatus == null) {
            roomStatus = "";
        }

        updateHouseDetailsViews();

        TextView ratingText =
                findViewById(R.id.lblRating);

        if (ratingText != null) {

            String rating =
                    intent.getStringExtra("RATING");

            ratingText.setText(
                    rating != null && !rating.trim().isEmpty()
                            ? rating
                            : "No rating yet"
            );
        }
    }

    private void updateHouseDetailsViews() {

        TextView houseNameText =
                findViewById(R.id.houseName);

        if (houseNameText != null) {
            houseNameText.setText(houseName);
        }

        TextView housePriceText =
                findViewById(R.id.housePrice);

        if (housePriceText != null && monthlyRent > 0) {
            housePriceText.setText(
                    formatRent(monthlyRent)
            );
        }

        // Update the address TextView even if the layout uses one
        // of these common IDs. getIdentifier() keeps this Java file
        // compilable even before the XML ID is finalized.
        setOptionalTextView("topBarHouseName", houseName);
        setOptionalTextView("topBarDistance", houseAddress);
        setOptionalTextView("houseAddress", houseAddress);
        setOptionalTextView("locationLabel", houseAddress);
        setOptionalTextView("addressText", houseAddress);
        setOptionalTextView("txtAddress", houseAddress);
        setOptionalTextView("locationText", houseAddress);

        // Also update other detail fields when matching IDs exist.
        setOptionalTextView("houseDescription", houseDescription);
        setOptionalTextView("descriptionText", houseDescription);
        setOptionalTextView("houseRules", houseRules);
        setOptionalTextView("rulesText", houseRules);
        setOptionalTextView("houseStatus", houseStatus);
        setOptionalTextView("statusText", houseStatus);
        renderHouseRules(houseRules);
    }

    private void renderHouseRules(String rulesStr) {
        LinearLayout container = findViewById(R.id.houseRulesContainer);
        if (container == null) return;
        container.removeAllViews();

        if (rulesStr == null || rulesStr.trim().isEmpty() || "null".equalsIgnoreCase(rulesStr.trim())) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No house rules specified.");
            emptyText.setTextColor(Color.parseColor("#9A9A9E"));
            emptyText.setTextSize(13);
            container.addView(emptyText);
            return;
        }

        String[] rules = rulesStr.split("\n");
        for (String rule : rules) {
            String trimmed = rule.trim();
            if (trimmed.isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
            lp.topMargin = dp(6);
            row.setLayoutParams(lp);

            TextView tv = new TextView(this);
            tv.setText("📌 " + trimmed);
            tv.setTextColor(Color.parseColor("#6E6E73"));
            tv.setTextSize(14);
            row.addView(tv);

            container.addView(row);
        }
    }

    private void setOptionalTextView(
            String viewName,
            String value
    ) {

        int viewId =
                getResources().getIdentifier(
                        viewName,
                        "id",
                        getPackageName()
                );

        if (viewId == 0) {
            return;
        }

        View view =
                findViewById(viewId);

        if (view instanceof TextView) {

            TextView textView =
                    (TextView) view;

            if (value != null && !value.trim().isEmpty()) {
                textView.setText(value);
            }
        }
    }

    private void loadLandlordName() {
        if (landlordId <= 0) {
            setupLandlordCardInteractions();
            return;
        }

        apiClient.getUserById(landlordId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> setLandlordDetails("Landlord", "", false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> setLandlordDetails("Landlord", "", false));
                    return;
                }
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        JSONObject u = json.optJSONObject("data");
                        if (u == null) {
                            JSONArray arr = json.optJSONArray("data");
                            if (arr != null && arr.length() > 0) {
                                u = arr.optJSONObject(0);
                            }
                        }
                        if (u != null) {
                            String name = u.optString("full_name", "Landlord");
                            String profileImage = u.optString("profile_image", "");
                            boolean isVerified = true;

                            runOnUiThread(() -> setLandlordDetails(name, profileImage, isVerified));
                            return;
                        }
                    }
                    runOnUiThread(() -> setLandlordDetails("Landlord", "", false));
                } catch (Exception e) {
                    runOnUiThread(() -> setLandlordDetails("Landlord", "", false));
                }
            }
        });
    }

    private void setLandlordDetails(String name, String profileImage, boolean isVerified) {
        landlordName = name;

        TextView landlordNameText = findViewById(R.id.landlordName);
        if (landlordNameText != null) landlordNameText.setText(name);

        TextView landlordInitials = findViewById(R.id.landlordInitials);
        if (landlordInitials != null) {
            landlordInitials.setText(getInitials(name));
        }

        ImageView landlordAvatarImage = findViewById(R.id.landlordAvatarImage);
        if (landlordAvatarImage != null && profileImage != null && !profileImage.trim().isEmpty() && !"null".equalsIgnoreCase(profileImage.trim())) {
            landlordAvatarImage.setVisibility(View.VISIBLE);
            if (landlordInitials != null) landlordInitials.setVisibility(View.GONE);
            String fullUrl = profileImage.startsWith("http") ? profileImage : "http://10.242.38.109/Dormigo_Backend/" + profileImage;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(landlordAvatarImage);
        } else {
            if (landlordAvatarImage != null) landlordAvatarImage.setVisibility(View.GONE);
            if (landlordInitials != null) landlordInitials.setVisibility(View.VISIBLE);
        }

        View verifiedBadge = findViewById(R.id.landlordVerifiedBadge);
        if (verifiedBadge != null) {
            verifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        TextView subtitle = findViewById(R.id.landlordSubtitle);
        if (subtitle != null) {
            subtitle.setText(isVerified ? "Verified Landlord" : "Member");
        }

        setupLandlordCardInteractions();
    }

    private void setupLandlordCardInteractions() {
        View landlordCard = findViewById(R.id.landlordCard);
        View landlordChatBtn = findViewById(R.id.landlordChatBtn);

        View.OnClickListener openProfileListener = v -> {
            Intent profileIntent = new Intent(this, LandlordProfileViewActivity.class);
            profileIntent.putExtra("LANDLORD_ID", landlordId);
            profileIntent.putExtra("HOUSE_ID", houseId);
            profileIntent.putExtra("LANDLORD_NAME", landlordName);
            startActivity(profileIntent);
        };

        if (landlordCard != null) landlordCard.setOnClickListener(openProfileListener);

        if (landlordChatBtn != null) {
            landlordChatBtn.setOnClickListener(v -> {
                Intent chatIntent = new Intent(this, ChatMessageActivity.class);
                chatIntent.putExtra("LANDLORD_ID", landlordId);
                chatIntent.putExtra("LANDLORD_NAME", landlordName);
                chatIntent.putExtra("HOUSE_ID", houseId);
                chatIntent.putExtra("HOUSE_NAME", houseName);
                startActivity(chatIntent);
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "L";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.US);
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase(Locale.US);
        }
        return parts[0].toUpperCase(Locale.US);
    }

    private void updateRoomViews() {

        TextView housePriceText =
                findViewById(
                        R.id.housePrice
                );

        if (housePriceText != null
                && monthlyRent > 0) {

            housePriceText.setText(
                    formatRent(monthlyRent)
            );
        }
    }

    private void loadRoomsForHouse() {

        if (houseId <= 0) {
            showRoomsMessage("Invalid boarding house.");
            return;
        }

        apiClient.getRooms(new Callback() {

            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {

                runOnUiThread(() ->
                        showRoomsMessage(
                                "Unable to load rooms."
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
                    body = response.body().string();
                }

                String result = body;

                runOnUiThread(() -> {

                    try {

                        JSONObject json =
                                new JSONObject(result);

                        if (!json.optBoolean(
                                "success",
                                false
                        )) {

                            showRoomsMessage(
                                    "Unable to load rooms."
                            );

                            return;
                        }

                        JSONArray data =
                                json.optJSONArray("data");

                        houseRooms.clear();

                        if (data != null) {

                            for (int i = 0;
                                 i < data.length();
                                 i++) {

                                JSONObject object =
                                        data.getJSONObject(i);

                                int apiHouseId =
                                        object.optInt(
                                                "house_id",
                                                0
                                        );

                                if (apiHouseId != houseId) {
                                    continue;
                                }

                                RoomItem room =
                                        new RoomItem();

                                room.roomId =
                                        object.optInt(
                                                "room_id",
                                                0
                                        );

                                room.houseId = apiHouseId;

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

                                room.advanceMonths = object.optInt("advance_months", 1);
                                room.depositMonths = object.optInt("deposit_months", 1);
                                room.utilityDeposit = object.optDouble("utility_deposit", 0);
                                room.otherFees = object.optDouble("other_fees", 0);
                                room.otherFeesDescription = object.optString("other_fees_description", "");
                                room.depositRefundPolicy = object.optString("deposit_refund_policy", "");

                                houseRooms.add(room);
                            }
                        }

                        chooseInitialRoom();
                        renderRooms();
                        updateRoomsOpenCount();

                    } catch (Exception e) {

                        showRoomsMessage(
                                "Invalid room response."
                        );
                    }
                });
            }
        });
    }

    private void chooseInitialRoom() {

        RoomItem passedRoom = null;
        RoomItem firstAvailable = null;
        RoomItem firstRoom = null;

        for (RoomItem room : houseRooms) {

            if (firstRoom == null) {
                firstRoom = room;
            }

            if (room.roomId == roomId) {
                passedRoom = room;
            }

            if (firstAvailable == null
                    && "AVAILABLE".equalsIgnoreCase(
                    room.status
            )) {

                firstAvailable = room;
            }
        }

        RoomItem selectedRoom = null;

        if (passedRoom != null
                && "AVAILABLE".equalsIgnoreCase(
                passedRoom.status
        )) {

            selectedRoom = passedRoom;

        } else if (firstAvailable != null) {

            selectedRoom = firstAvailable;

        } else if (passedRoom != null) {

            selectedRoom = passedRoom;

        } else {

            selectedRoom = firstRoom;
        }

        if (selectedRoom != null) {
            selectRoom(selectedRoom, false);
        }
    }

    private void renderRooms() {

        LinearLayout roomsContainer =
                findViewById(R.id.roomsContainer);

        if (roomsContainer == null) {
            return;
        }

        roomsContainer.removeAllViews();

        if (houseRooms.isEmpty()) {
            showRoomsMessage("No rooms found for this boarding house.");
            return;
        }

        for (RoomItem room : houseRooms) {
            roomsContainer.addView(
                    createRoomView(room)
            );
        }
    }

    private View createRoomView(
            RoomItem room
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );

        card.setBackgroundResource(
                R.drawable.bg_card_rounded
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        boolean available =
                "AVAILABLE".equalsIgnoreCase(
                        room.status
                );

        card.setAlpha(
                available ? 1.0f : 0.60f
        );

        LinearLayout left =
                new LinearLayout(this);

        left.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams leftParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        left.setLayoutParams(leftParams);

        TextView roomNameText =
                new TextView(this);

        String displayRoomName =
                room.roomNumber == null
                        || room.roomNumber.trim().isEmpty()
                        ? "Room " + room.roomId
                        : room.roomNumber.trim();

        if (room.roomId == roomId) {
            displayRoomName += "  ✓ Selected";
        }

        roomNameText.setText(displayRoomName);
        roomNameText.setTextColor(
                Color.parseColor("#1A1A1A")
        );
        roomNameText.setTextSize(14);
        roomNameText.setTypeface(
                roomNameText.getTypeface(),
                Typeface.BOLD
        );

        left.addView(roomNameText);

        TextView roomDetailsText =
                new TextView(this);

        StringBuilder details =
                new StringBuilder();

        if (room.roomType != null
                && !room.roomType.trim().isEmpty()) {

            details.append(
                    room.roomType.trim()
            );
        }

        if (room.capacity > 0) {

            if (details.length() > 0) {
                details.append(" • ");
            }

            details.append(
                    room.capacity
            ).append(" pax");
        }

        if (room.status != null
                && !room.status.trim().isEmpty()) {

            if (details.length() > 0) {
                details.append(" • ");
            }

            details.append(
                    room.status.trim()
            );
        }

        roomDetailsText.setText(
                details.toString()
        );

        roomDetailsText.setTextColor(
                available
                        ? Color.parseColor("#1B5E4C")
                        : Color.parseColor("#9A9A9E")
        );

        roomDetailsText.setTextSize(12);
        roomDetailsText.setPadding(
                0,
                dp(4),
                0,
                0
        );

        left.addView(roomDetailsText);

        TextView rentText =
                new TextView(this);

        rentText.setText(
                formatRent(room.monthlyRent)
        );

        rentText.setTextColor(
                Color.parseColor("#1A1A1A")
        );

        rentText.setTextSize(14);
        rentText.setTypeface(
                rentText.getTypeface(),
                Typeface.BOLD
        );

        LinearLayout.LayoutParams rentParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rentParams.leftMargin = dp(12);
        rentText.setLayoutParams(rentParams);

        card.addView(left);
        card.addView(rentText);

        card.setOnClickListener(v -> {

            if (!available) {

                showToast(
                        "This room is not available."
                );

                return;
            }

            selectRoom(room, true);
        });

        return card;
    }

    private void selectRoom(
            RoomItem room,
            boolean showSelectionMessage
    ) {

        roomId = room.roomId;
        roomNumber = room.roomNumber == null
                ? ""
                : room.roomNumber;
        roomType = room.roomType == null
                ? ""
                : room.roomType;
        roomCapacity = room.capacity;
        monthlyRent = room.monthlyRent;
        roomStatus = room.status == null
                ? ""
                : room.status;

        selectedAdvanceMonths = room.advanceMonths;
        selectedDepositMonths = room.depositMonths;
        selectedUtilityDeposit = room.utilityDeposit;
        selectedOtherFees = room.otherFees;
        selectedOtherFeesDesc = room.otherFeesDescription != null ? room.otherFeesDescription : "";
        selectedRefundPolicy = room.depositRefundPolicy != null ? room.depositRefundPolicy : "";

        updateRoomViews();

        if (showSelectionMessage) {

            String selectedName =
                    roomNumber.trim().isEmpty()
                            ? "Room " + roomId
                            : roomNumber;

            showToast(
                    selectedName + " selected"
            );

            renderRooms();
        }
    }

    private void updateRoomsOpenCount() {

        int availableCount = 0;

        for (RoomItem room : houseRooms) {

            if ("AVAILABLE".equalsIgnoreCase(
                    room.status
            )) {

                availableCount++;
            }
        }

        TextView roomsOpenCount =
                findViewById(
                        R.id.roomsOpenCount
                );

        if (roomsOpenCount != null) {

            roomsOpenCount.setText(
                    availableCount == 1
                            ? "1 room open"
                            : availableCount + " rooms open"
            );
        }
    }

    private void showRoomsMessage(
            String message
    ) {

        LinearLayout roomsContainer =
                findViewById(R.id.roomsContainer);

        if (roomsContainer == null) {
            return;
        }

        roomsContainer.removeAllViews();

        TextView textView =
                new TextView(this);

        textView.setText(message);
        textView.setTextColor(
                Color.parseColor("#9A9A9E")
        );
        textView.setTextSize(13);
        textView.setPadding(
                dp(4),
                dp(8),
                dp(4),
                dp(8)
        );

        roomsContainer.addView(textView);
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

    private String formatRent(
            double amount
    ) {

        return String.format(
                Locale.US,
                "₱%,.2f/month",
                amount
        );
    }

    private String formatMoveInAmount(
            double amount
    ) {

        return String.format(
                Locale.US,
                "₱%,.2f",
                amount
        );
    }

    private void addBreakdownRow(LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        if (container.getChildCount() > 0) {
            lp.topMargin = dp(6);
        }
        row.setLayoutParams(lp);

        TextView lbl = new TextView(this);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        lbl.setText(label + ":");
        lbl.setTextColor(Color.parseColor("#6E6E73"));
        lbl.setTextSize(13);

        TextView val = new TextView(this);
        val.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        val.setText(value);
        val.setTextColor(Color.parseColor("#1A1A1A"));
        val.setTextSize(13);
        val.setTypeface(null, Typeface.BOLD);

        row.addView(lbl);
        row.addView(val);
        container.addView(row);
    }



    // =========================================================
    // LOAD AMENITIES FOR THIS BOARDING HOUSE
    // =========================================================

    private void loadAmenitiesForHouse() {

        if (houseId <= 0) {

            showAmenitiesMessage(
                    "No amenities listed."
            );

            return;
        }

        apiClient.getBoardingHouseAmenities(
                houseId,
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

                                    showAmenitiesMessage(
                                            "No amenities listed."
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (
                                        data == null
                                                || data.length() == 0
                                ) {

                                    showAmenitiesMessage(
                                            "No amenities listed."
                                    );

                                    return;
                                }

                                List<String> directNames =
                                        new ArrayList<>();

                                boolean hasDirectNames =
                                        false;

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject item =
                                            data.getJSONObject(
                                                    i
                                            );

                                    String amenityName =
                                            item.optString(
                                                    "amenity_name",
                                                    ""
                                            ).trim();

                                    if (amenityName.isEmpty()) {

                                        amenityName =
                                                item.optString(
                                                        "name",
                                                        ""
                                                ).trim();
                                    }

                                    if (!amenityName.isEmpty()) {

                                        directNames.add(
                                                amenityName
                                        );

                                        hasDirectNames =
                                                true;
                                    }
                                }

                                if (hasDirectNames) {

                                    renderAmenities(
                                            directNames
                                    );

                                } else {

                                    loadAmenityNames(
                                            data
                                    );
                                }

                            } catch (Exception e) {

                                showAmenitiesMessage(
                                        "Unable to load amenities."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // LOAD AMENITY NAMES
    // =========================================================

    private void loadAmenityNames(
            JSONArray houseAmenityData
    ) {

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

                                    showAmenitiesMessage(
                                            "No amenities listed."
                                    );

                                    return;
                                }

                                JSONArray allAmenities =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (
                                        allAmenities == null
                                                || allAmenities.length() == 0
                                ) {

                                    showAmenitiesMessage(
                                            "No amenities listed."
                                    );

                                    return;
                                }

                                List<String> names =
                                        new ArrayList<>();

                                for (
                                        int i = 0;
                                        i < houseAmenityData.length();
                                        i++
                                ) {

                                    JSONObject relationship =
                                            houseAmenityData
                                                    .getJSONObject(
                                                            i
                                                    );

                                    int amenityId =
                                            relationship.optInt(
                                                    "amenity_id",
                                                    0
                                            );

                                    if (amenityId <= 0) {
                                        continue;
                                    }

                                    for (
                                            int j = 0;
                                            j < allAmenities.length();
                                            j++
                                    ) {

                                        JSONObject amenity =
                                                allAmenities
                                                        .getJSONObject(
                                                                j
                                                        );

                                        int databaseAmenityId =
                                                amenity.optInt(
                                                        "amenity_id",
                                                        0
                                                );

                                        if (
                                                amenityId
                                                        != databaseAmenityId
                                        ) {

                                            continue;
                                        }

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
                                                !amenityName.isEmpty()
                                                        && !names.contains(
                                                        amenityName
                                                )
                                        ) {

                                            names.add(
                                                    amenityName
                                            );
                                        }

                                        break;
                                    }
                                }

                                if (names.isEmpty()) {

                                    showAmenitiesMessage(
                                            "No amenities listed."
                                    );

                                } else {

                                    renderAmenities(
                                            names
                                    );
                                }

                            } catch (Exception e) {

                                showAmenitiesMessage(
                                        "Unable to load amenities."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // RENDER AMENITIES
    // =========================================================

    private void renderAmenities(
            List<String> amenities
    ) {

        LinearLayout container =
                findViewById(
                        R.id.amenitiesContainer
                );

        if (container == null) {
            return;
        }

        container.removeAllViews();

        for (String amenity : amenities) {

            if (
                    amenity == null
                            || amenity.trim().isEmpty()
            ) {

                continue;
            }

            TextView chip =
                    new TextView(
                            this
                    );

            String icon = AmenityHelper.getAmenityIcon(amenity.trim());
            chip.setText(icon + " " + amenity.trim());

            chip.setTextColor(
                    Color.parseColor(
                            "#6E6E73"
                    )
            );

            chip.setTextSize(
                    13
            );

            chip.setGravity(
                    Gravity.CENTER
            );

            chip.setPadding(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(8)
            );

            chip.setBackgroundResource(
                    R.drawable.bg_chip
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.rightMargin =
                    dp(8);

            container.addView(
                    chip,
                    params
            );
        }

        if (container.getChildCount() == 0) {

            showAmenitiesMessage(
                    "No amenities listed."
            );
        }
    }

    // =========================================================
    // AMENITIES MESSAGE
    // =========================================================

    private void showAmenitiesMessage(
            String message
    ) {

        runOnUiThread(() -> {

            LinearLayout container =
                    findViewById(
                            R.id.amenitiesContainer
                    );

            if (container == null) {
                return;
            }

            container.removeAllViews();

            TextView textView =
                    new TextView(
                            this
                    );

            textView.setText(
                    message
            );

            textView.setTextColor(
                    Color.parseColor(
                            "#9A9A9E"
                    )
            );

            textView.setTextSize(
                    13
            );

            textView.setPadding(
                    dp(4),
                    dp(8),
                    dp(4),
                    dp(8)
            );

            container.addView(
                    textView
            );
        });
    }

    private void setupClickListeners() {

        ImageView btnBack =
                findViewById(R.id.btnBack);

        TextView btnMessage =
                findViewById(
                        R.id.btnMessage
                );

        TextView btnRequestBooking =
                findViewById(
                        R.id.btnRequestBooking
                );

        View btnDirections =
                findViewById(
                        R.id.btnDirections
                );

        if (btnBack != null) {

            btnBack.setOnClickListener(v -> {

                finish();

                overridePendingTransition(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                );
            });
        }

        if (btnMessage != null) {

            btnMessage.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                this,
                                ChatMessageActivity.class
                        );

                intent.putExtra(
                        "LANDLORD_NAME",
                        landlordName
                );

                intent.putExtra(
                        "HOUSE_NAME",
                        houseName
                );

                intent.putExtra(
                        "HOUSE_ID",
                        houseId
                );

                intent.putExtra(
                        "LANDLORD_ID",
                        landlordId
                );

                startActivity(intent);
            });
        }

        if (btnRequestBooking != null) {

            btnRequestBooking.setOnClickListener(
                    v -> showBookingRequestBottomSheet()
            );
        }

        if (btnDirections != null) {

            btnDirections.setOnClickListener(
                    v -> openDirections()
            );
        }

        View locationCard =
                findViewById(
                        R.id.locationCard
                );

        if (locationCard != null) {

            locationCard.setOnClickListener(
                    v -> openLocation()
            );
        }
    }

    private void openDirections() {
        if (houseAddress == null || houseAddress.trim().isEmpty()) {
            showToast("Address is unavailable.");
            return;
        }

        String destination = Uri.encode(houseAddress);
        try {
            Uri uri = Uri.parse("google.navigation:q=" + destination);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + destination);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void openLocation() {
        if (houseAddress == null || houseAddress.trim().isEmpty()) {
            showToast("Address is unavailable.");
            return;
        }

        String destination = Uri.encode(houseAddress);
        try {
            Uri uri = Uri.parse("geo:0,0?q=" + destination);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + destination);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void loadStaticMap() {

        ImageView imgStaticMap =
                findViewById(
                        R.id.imgStaticMap
                );

        if (imgStaticMap == null) {
            return;
        }

        imgStaticMap.setImageResource(
                R.drawable.bg_map_placeholder
        );
    }

    private void showBookingRequestBottomSheet() {

        if (roomId <= 0) {
            showToast("Please select a room first.");
            return;
        }

        if (roomStatus != null
                && !roomStatus.trim().isEmpty()
                && !"AVAILABLE".equalsIgnoreCase(roomStatus)) {

            showToast("Please select an available room.");
            return;
        }

        BottomSheetDialog dialog =
                new BottomSheetDialog(this);

        View view =
                getLayoutInflater().inflate(
                        R.layout.booking_request_bottom_sheet,
                        findViewById(R.id.mainLayout),
                        false
                );

        dialog.setContentView(view);

        ImageView btnBack =
                view.findViewById(R.id.btnBackBooking);

        if (btnBack != null) {
            btnBack.setOnClickListener(
                    v -> dialog.dismiss()
            );
        }

        TextView bookingHouseName =
                view.findViewById(R.id.bookingHouseName);

        TextView selectedRoomName =
                view.findViewById(R.id.selectedRoomName);

        TextView selectedRoomDetails =
                view.findViewById(R.id.selectedRoomDetails);

        TextView selectedRoomPrice =
                view.findViewById(R.id.selectedRoomPrice);

        if (bookingHouseName != null) {
            bookingHouseName.setText(houseName);
        }

        String displayRoomName =
                roomNumber == null
                        || roomNumber.trim().isEmpty()
                        ? "Room " + roomId
                        : roomNumber.trim();

        if (selectedRoomName != null) {
            selectedRoomName.setText(displayRoomName);
        }

        StringBuilder roomDetails =
                new StringBuilder();

        if (roomType != null
                && !roomType.trim().isEmpty()) {
            roomDetails.append(roomType.trim());
        }

        if (roomCapacity > 0) {
            if (roomDetails.length() > 0) {
                roomDetails.append(" • ");
            }
            roomDetails.append(roomCapacity).append(" pax");
        }

        if (roomStatus != null
                && !roomStatus.trim().isEmpty()) {
            if (roomDetails.length() > 0) {
                roomDetails.append(" • ");
            }
            roomDetails.append(roomStatus.trim());
        }

        if (selectedRoomDetails != null) {
            selectedRoomDetails.setText(roomDetails.toString());
        }

        if (selectedRoomPrice != null) {
            selectedRoomPrice.setText(formatRent(monthlyRent));
        }

        LinearLayout breakdownContainer = view.findViewById(R.id.layoutBreakdownRows);
        TextView tvTotal = view.findViewById(R.id.textTotalInitialPayment);
        TextView tvPolicy = view.findViewById(R.id.textRefundPolicy);

        double totalInitial = monthlyRent + selectedOtherFees;
        lastCalculatedTotalInitial = totalInitial;

        if (breakdownContainer != null) {
            breakdownContainer.removeAllViews();
            addBreakdownRow(breakdownContainer, "Monthly Rent", formatMoveInAmount(monthlyRent));

            if (selectedOtherFeesDesc != null && !selectedOtherFeesDesc.trim().isEmpty() && !"null".equalsIgnoreCase(selectedOtherFeesDesc.trim())) {
                String[] parts = selectedOtherFeesDesc.split(";");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.isEmpty()) continue;
                    int colonIdx = p.indexOf(':');
                    if (colonIdx != -1) {
                        String name = p.substring(0, colonIdx).trim();
                        String valStr = p.substring(colonIdx + 1).trim().replace("₱", "").replace(",", "").trim();
                        double amt = 0;
                        try {
                            amt = Double.parseDouble(valStr);
                        } catch (Exception ignored) {}

                        if (!name.isEmpty()) {
                            addBreakdownRow(breakdownContainer, name, formatMoveInAmount(amt));
                        }
                    }
                }
            } else if (selectedOtherFees > 0) {
                addBreakdownRow(breakdownContainer, "Other Move-in Fees", formatMoveInAmount(selectedOtherFees));
            }
        }

        if (tvTotal != null) tvTotal.setText(formatMoveInAmount(totalInitial));
        if (tvPolicy != null) tvPolicy.setText("Refund Policy: " + (selectedRefundPolicy.isEmpty() ? "As specified by landlord." : selectedRefundPolicy));

        EditText inputDate =
                view.findViewById(R.id.inputMoveInDate);

        EditText inputDuration =
                view.findViewById(R.id.inputStayDuration);

        EditText inputMessage =
                view.findViewById(R.id.inputMessage);

        if (inputDate != null) {
            inputDate.setOnClickListener(
                    v -> showDatePicker(inputDate)
            );
        }

        View btnSubmit =
                view.findViewById(R.id.btnSubmitBookingRequest);

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v ->
                    submitBooking(
                            dialog,
                            inputDate,
                            inputDuration,
                            inputMessage,
                            btnSubmit
                    )
            );
        }

        dialog.show();
    }

    private void submitBooking(
            BottomSheetDialog dialog,
            EditText inputDate,
            EditText inputDuration,
            EditText inputMessage,
            View btnSubmit
    ) {

        if (houseId <= 0) {

            showToast(
                    "Invalid boarding house."
            );

            return;
        }

        if (roomId <= 0) {

            showToast(
                    "Invalid room."
            );

            return;
        }

        if (monthlyRent <= 0) {

            showToast(
                    "Room rent is unavailable."
            );

            return;
        }

        if (roomStatus != null
                && !roomStatus.isEmpty()
                && !"AVAILABLE".equalsIgnoreCase(
                roomStatus
        )) {

            showToast(
                    "This room is not available."
            );

            return;
        }

        String moveInDate =
                inputDate != null
                        ? inputDate.getText()
                        .toString()
                        .trim()
                        : "";

        String durationText =
                inputDuration != null
                        ? inputDuration.getText()
                        .toString()
                        .trim()
                        : "";

        String messageToLandlord =
                inputMessage != null
                        ? inputMessage.getText()
                        .toString()
                        .trim()
                        : "";

        if (messageToLandlord.length() > 1000) {

            showToast(
                    "Message must be 1000 characters or less."
            );

            return;
        }

        if (moveInDate.isEmpty()) {

            showToast(
                    "Please enter target move-in date."
            );

            return;
        }

        if (durationText.isEmpty()) {

            showToast(
                    "Please enter stay duration."
            );

            return;
        }

        int durationMonths;

        try {

            durationMonths =
                    Integer.parseInt(
                            durationText
                    );

        } catch (NumberFormatException e) {

            showToast(
                    "Duration must be a number."
            );

            return;
        }

        if (durationMonths <= 0) {

            showToast(
                    "Duration must be greater than 0."
            );

            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(
                        "DormigoPrefs",
                        MODE_PRIVATE
                );

        int userId =
                prefs.getInt(
                        "userId",
                        0
                );

        if (userId <= 0) {

            showToast(
                    "Please log in before booking."
            );

            return;
        }

        double totalAmount =
                lastCalculatedTotalInitial > 0
                        ? lastCalculatedTotalInitial
                        : monthlyRent * durationMonths;

        btnSubmit.setEnabled(false);

        apiClient.createBooking(
                userId,
                roomId,
                moveInDate,
                durationMonths,
                "PENDING",
                monthlyRent,
                totalAmount,
                messageToLandlord,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            btnSubmit.setEnabled(
                                    true
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
                                    response.body().string();
                        }

                        String result = body;

                        runOnUiThread(() -> {

                            btnSubmit.setEnabled(
                                    true
                            );

                            try {

                                JSONObject json =
                                        new JSONObject(
                                                result
                                        );

                                boolean success =
                                        json.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    showToast(
                                            json.optString(
                                                    "message",
                                                    "Booking failed."
                                            )
                                    );

                                    return;
                                }

                                int bookingId =
                                        json.optInt(
                                                "booking_id",
                                                0
                                        );

                                Intent intent =
                                        new Intent(
                                                ViewBoardingHouseActivity.this,
                                                RequestSentActivity.class
                                        );

                                intent.putExtra(
                                        "BOOKING_ID",
                                        bookingId
                                );

                                intent.putExtra(
                                        "LANDLORD_NAME",
                                        landlordName
                                );

                                intent.putExtra(
                                        "HOUSE_NAME",
                                        houseName
                                );

                                intent.putExtra(
                                        "HOUSE_ID",
                                        houseId
                                );

                                intent.putExtra(
                                        "ROOM_ID",
                                        roomId
                                );

                                intent.putExtra(
                                        "ROOM_NAME",
                                        roomNumber.isEmpty()
                                                ? roomType
                                                : roomNumber
                                );

                                intent.putExtra(
                                        "ROOM_TYPE",
                                        roomType
                                );

                                intent.putExtra(
                                        "MONTHLY_RENT",
                                        monthlyRent
                                );

                                intent.putExtra(
                                        "TOTAL_AMOUNT",
                                        totalAmount
                                );

                                intent.putExtra(
                                        "MOVE_IN_DATE",
                                        moveInDate
                                );

                                intent.putExtra(
                                        "DURATION",
                                        String.valueOf(
                                                durationMonths
                                        )
                                );

                                intent.putExtra(
                                        "MESSAGE_TO_LANDLORD",
                                        messageToLandlord
                                );

                                startActivity(intent);

                                dialog.dismiss();

                            } catch (Exception e) {

                                showToast(
                                        "Invalid server response."
                                );
                            }
                        });
                    }
                }
        );
    }

    private void showDatePicker(
            EditText inputDate
    ) {

        Calendar calendar =
                Calendar.getInstance();

        int year =
                calendar.get(
                        Calendar.YEAR
                );

        int month =
                calendar.get(
                        Calendar.MONTH
                );

        int day =
                calendar.get(
                        Calendar.DAY_OF_MONTH
                );

        DatePickerDialog picker =
                new DatePickerDialog(
                        this,
                        (view,
                         selectedYear,
                         selectedMonth,
                         selectedDay) -> {

                            String date =
                                    String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            selectedYear,
                                            selectedMonth + 1,
                                            selectedDay
                                    );

                            inputDate.setText(
                                    date
                            );
                        },
                        year,
                        month,
                        day
                );

        picker.show();
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

    private void loadReviewsForHouse() {
        if (houseId <= 0) return;
        apiClient.getReviewsForHouse(houseId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        double avgRating = json.optDouble("average_rating", 0.0);
                        int reviewCount = json.optInt("review_count", 0);
                        JSONArray data = json.optJSONArray("data");
                        runOnUiThread(() -> {
                            TextView lblRating = findViewById(R.id.lblRating);
                            if (lblRating != null) {
                                if (reviewCount > 0) {
                                    lblRating.setText("★ " + String.format(Locale.US, "%.1f", avgRating) + " (" + reviewCount + " " + (reviewCount == 1 ? "Review" : "Reviews") + ")");
                                } else {
                                    lblRating.setText("No ratings yet");
                                }
                            }
                            if (data != null) renderReviews(data);
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void renderReviews(JSONArray reviews) {
        LinearLayout container = findViewById(R.id.reviewsContainer);
        if (container == null) return;
        container.removeAllViews();

        if (reviews.length() == 0) {
            TextView tv = new TextView(this);
            tv.setText("No reviews yet.");
            tv.setTextColor(0xFF9A9A9E);
            tv.setTextSize(13);
            container.addView(tv);
            return;
        }

        for (int i = 0; i < reviews.length(); i++) {
            try {
                JSONObject r = reviews.getJSONObject(i);
                String comment = r.optString("comment", "");
                int rating = r.optInt("rating", 5);
                String userName = r.optString("full_name", "Student");

                LinearLayout card = new LinearLayout(this);
                card.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                card.setBackgroundResource(R.drawable.bg_card_rounded);
                LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
                cardParams.bottomMargin = dp(12);
                card.setLayoutParams(cardParams);

                TextView tvName = new TextView(this);
                tvName.setText(userName + " (" + rating + "★)");
                tvName.setTextColor(0xFF1A1A1A);
                tvName.setTextSize(14);
                tvName.setTypeface(null, Typeface.BOLD);

                TextView tvComment = new TextView(this);
                tvComment.setText(comment);
                tvComment.setTextColor(0xFF6E6E73);
                tvComment.setTextSize(13);
                LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cParams.topMargin = dp(4);
                tvComment.setLayoutParams(cParams);

                card.addView(tvName);
                if (!comment.isEmpty() && !comment.equals("null")) {
                    card.addView(tvComment);
                }
                container.addView(card);
            } catch (Exception ignored) {}
        }
    }

    private void loadHousePhotos() {
        if (houseId <= 0) return;
        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject h = data.getJSONObject(i);
                                int hId = h.optInt("house_id", 0);
                                if (hId == houseId) {
                                    String vStatus = h.optString("verification_status", "");
                                    boolean freeElec = h.optBoolean("free_electricity", false);
                                    String elecRate = h.optString("electricity_rate", "0.00");
                                    boolean freeWater = h.optBoolean("free_water", false);
                                    String waterRate = h.optString("water_rate", "0.00");

                                    String elecText = freeElec ? "Electricity — Free" : "Electricity — ₱" + elecRate + "/kWh";
                                    String waterText = freeWater ? "Water — Free" : "Water — ₱" + waterRate + "/month";

                                    runOnUiThread(() -> {
                                        TextView badge = findViewById(R.id.lblVerifiedBadge);
                                        if (badge != null) {
                                            if ("VERIFIED".equalsIgnoreCase(vStatus)) {
                                                badge.setVisibility(View.VISIBLE);
                                            } else {
                                                badge.setVisibility(View.GONE);
                                            }
                                        }
                                        TextView tvElec = findViewById(R.id.textElectricityUtility);
                                        TextView tvWater = findViewById(R.id.textWaterUtility);
                                        if (tvElec != null) tvElec.setText(elecText);
                                        if (tvWater != null) tvWater.setText(waterText);
                                    });

                                    JSONArray pArr = h.optJSONArray("photo_paths");
                                    if (pArr != null && pArr.length() > 0) {
                                        List<String> paths = new ArrayList<>();
                                        for (int j = 0; j < pArr.length(); j++) {
                                            String p = pArr.optString(j, "");
                                            if (!p.isEmpty()) paths.add(p);
                                        }
                                        runOnUiThread(() -> renderHousePhotos(paths));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void renderHousePhotos(List<String> paths) {
        ImageView imgMain = findViewById(R.id.imgHouseMain);
        ImageView imgThumb1 = findViewById(R.id.imgThumb1);
        ImageView imgThumb2 = findViewById(R.id.imgThumb2);
        ImageView imgThumb3 = findViewById(R.id.imgThumb3);

        ImageView[] thumbs = {imgThumb1, imgThumb2, imgThumb3};

        final int[] currentSelectedIndex = {0};

        if (!paths.isEmpty() && imgMain != null) {
            String mainUrl = "http://10.242.38.109/Dormigo_Backend/" + paths.get(0);
            Glide.with(this)
                    .load(mainUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(imgMain);
            imgMain.setOnClickListener(v -> showFullImageDialog(paths, currentSelectedIndex[0]));
        }

        for (int i = 0; i < thumbs.length; i++) {
            if (thumbs[i] != null) {
                if (i + 1 < paths.size()) {
                    String thumbUrl = "http://10.242.38.109/Dormigo_Backend/" + paths.get(i + 1);
                    thumbs[i].setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(thumbUrl)
                            .placeholder(R.drawable.bg_image_gallery_item)
                            .into(thumbs[i]);

                    final int index = i + 1;
                    thumbs[i].setOnClickListener(v -> {
                        if (imgMain != null && index < paths.size()) {
                            currentSelectedIndex[0] = index;
                            String url = "http://10.242.38.109/Dormigo_Backend/" + paths.get(index);
                            Glide.with(this).load(url).into(imgMain);
                        }
                    });
                } else {
                    thumbs[i].setVisibility(View.GONE);
                }
            }
        }
    }

    private void showFullImageDialog(List<String> paths, int startIndex) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);
        ViewPager2 viewPager = dialog.findViewById(R.id.viewPager);
        View btnClose = dialog.findViewById(R.id.btnClose);
        TextView textIndicator = dialog.findViewById(R.id.textImageIndicator);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (viewPager != null) {
            FullScreenImageAdapter adapter = new FullScreenImageAdapter(paths);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startIndex, false);

            if (textIndicator != null) {
                textIndicator.setText((startIndex + 1) + " / " + paths.size());
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        textIndicator.setText((position + 1) + " / " + paths.size());
                    }
                });
            }
        }
        dialog.show();
    }

    private static class FullScreenImageAdapter extends RecyclerView.Adapter<FullScreenImageAdapter.ViewHolder> {
        private final List<String> paths;

        public FullScreenImageAdapter(List<String> paths) {
            this.paths = paths;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView img = new ImageView(parent.getContext());
            img.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(img);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = "http://10.242.38.109/Dormigo_Backend/" + paths.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return paths.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
