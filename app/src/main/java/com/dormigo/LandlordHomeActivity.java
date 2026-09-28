package com.dormigo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import android.app.AlertDialog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordHomeActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private JSONArray landlordHouses = new JSONArray();
    private int currentHouseIndex = 0;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int lastOccupancyRate = -1;
    private boolean isDataLoading = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDataLoading) {
                loadLandlordData();
            }
            pollHandler.removeCallbacks(this);
            pollHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_home);

        apiClient = new ApiClient();

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadLandlordData);
        }

        loadLandlordData();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.removeCallbacks(pollRunnable);
        loadLandlordData();
        pollHandler.postDelayed(pollRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void loadLandlordData() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("fullName", "Landlord");
        int landlordId = prefs.getInt("userId", -1);

        TextView greetingLabel = findViewById(R.id.greetingLabel);
        if (greetingLabel != null) {
            String firstName = fullName.trim().split("\\s+")[0];
            greetingLabel.setText("Kumusta, " + firstName);
        }

        if (landlordId > 0) {
            loadLandlordPropertiesAndStats(landlordId);
            loadPendingRequests(landlordId);
            loadRecentPayments(landlordId);
        }
    }

    private void loadLandlordPropertiesAndStats(int landlordId) {
        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray houses = json.optJSONArray("data");
                        JSONArray myHouses = new JSONArray();
                        if (houses != null) {
                            for (int i = 0; i < houses.length(); i++) {
                                JSONObject h = houses.getJSONObject(i);
                                if (h.optInt("landlord_id", 0) == landlordId) {
                                    myHouses.put(h);
                                }
                            }
                        }

                        landlordHouses = myHouses;
                        if (landlordHouses.length() > 0) {
                            if (currentHouseIndex >= landlordHouses.length()) {
                                currentHouseIndex = 0;
                            }
                            runOnUiThread(() -> displayHouseAtIndex(currentHouseIndex));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        loadNotificationsBadge(landlordId);
    }

    private void loadNotificationsBadge(int userId) {
        apiClient.getNotifications(userId, new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        int unreadCount = json.optInt("unread_count", 0);
                        runOnUiThread(() -> {
                            TextView badge = findViewById(R.id.notificationBadge);
                            if (badge != null) {
                                if (unreadCount > 0) {
                                    badge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                                    badge.setVisibility(View.VISIBLE);
                                } else {
                                    badge.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void displayHouseAtIndex(int index) {
        try {
            if (landlordHouses.length() == 0) return;
            JSONObject house = landlordHouses.getJSONObject(index);
            int houseId = house.optInt("house_id", 0);
            String houseName = house.optString("house_name", "Landlord Properties");
            String houseAddress = house.optString("address", "Near Campus");

            TextView nameView = findViewById(R.id.textLandlordHouseName);
            TextView addrView = findViewById(R.id.textLandlordHouseAddress);
            if (nameView != null) nameView.setText(houseName);
            if (addrView != null) addrView.setText(houseAddress);

            loadRoomsForHouse(houseId);
        } catch (Exception ignored) {}
    }

    private void loadRoomsForHouse(int houseId) {
        apiClient.getRooms(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isDataLoading = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isDataLoading = false;
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray rooms = json.optJSONArray("data");
                        int total = 0;
                        int occupied = 0;
                        int available = 0;
                        if (rooms != null) {
                            for (int i = 0; i < rooms.length(); i++) {
                                JSONObject r = rooms.getJSONObject(i);
                                if (r.optInt("house_id", 0) == houseId) {
                                    total++;
                                    String status = r.optString("status", "").toUpperCase();
                                    if ("OCCUPIED".equals(status)) {
                                        occupied++;
                                    } else {
                                        available++;
                                    }
                                }
                            }
                        }

                        int finalTotal = total;
                        int finalOccupied = occupied;
                        int rate = finalTotal > 0 ? (finalOccupied * 100) / finalTotal : 0;

                        runOnUiThread(() -> {
                            TextView totalView = findViewById(R.id.textTotalUnits);
                            TextView occupiedView = findViewById(R.id.textOccupiedUnits);
                            TextView fracView = findViewById(R.id.textOccupancyFraction);

                            if (totalView != null) totalView.setText(String.valueOf(finalTotal));
                            if (occupiedView != null) occupiedView.setText(String.valueOf(finalOccupied));
                            if (fracView != null) fracView.setText(finalOccupied + " of " + finalTotal + " rooms filled");

                            animateOccupancy(rate);
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        return "ST";
    }

    private void loadRecentPayments(int landlordId) {
        apiClient.getPaymentsForLandlord(landlordId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray payments = json.optJSONArray("data");
                        runOnUiThread(() -> bindRecentPayments(payments));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void bindRecentPayments(JSONArray payments) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        View paymentsSection = findViewById(R.id.paymentsSection);
        View paymentsCard = findViewById(R.id.paymentsCard);
        View paymentItem1 = findViewById(R.id.paymentItem1);
        View paymentItem2 = findViewById(R.id.paymentItem2);
        
        // Find divider using child index relative to the card if possible
        View dividerView = null;
        if (paymentsCard instanceof LinearLayout && ((LinearLayout) paymentsCard).getChildCount() > 1) {
             dividerView = ((LinearLayout) paymentsCard).getChildAt(1);
        }

        if (payments == null || payments.length() == 0) {
            if (paymentsSection != null) paymentsSection.setVisibility(View.GONE);
            if (paymentsCard != null) paymentsCard.setVisibility(View.GONE);
        } else {
            if (paymentsSection != null) paymentsSection.setVisibility(View.VISIBLE);
            if (paymentsCard != null) paymentsCard.setVisibility(View.VISIBLE);

            List<JSONObject> sortedPayments = new ArrayList<>();
            List<JSONObject> submittedPayments = new ArrayList<>();
            List<JSONObject> otherPayments = new ArrayList<>();

            for (int i = 0; i < payments.length(); i++) {
                JSONObject obj = payments.optJSONObject(i);
                if (obj != null) {
                    String status = obj.optString("status", "").toUpperCase(Locale.ROOT);
                    if ("SUBMITTED".equals(status)) {
                        submittedPayments.add(obj);
                    } else {
                        otherPayments.add(obj);
                    }
                }
            }
            sortedPayments.addAll(submittedPayments);
            sortedPayments.addAll(otherPayments);

            // Payment 1
            if (sortedPayments.size() > 0) {
                if (paymentItem1 != null) paymentItem1.setVisibility(View.VISIBLE);
                try {
                    JSONObject p1 = sortedPayments.get(0);
                    String tenantName1 = p1.optString("tenant_name", "Student Tenant");
                    String houseName1 = p1.optString("house_name", "Boarding House");
                    String roomNo1 = p1.optString("room_number", "Room");
                    double amount1 = p1.optDouble("amount", 0);
                    String status1 = p1.optString("status", "PENDING").toUpperCase(Locale.ROOT);

                    TextView nameView1 = paymentItem1.findViewById(R.id.textPaymentTenantName1);
                    TextView detailsView1 = paymentItem1.findViewById(R.id.textPaymentDetails1);
                    TextView amountView1 = paymentItem1.findViewById(R.id.textPaymentAmount1);
                    TextView statusView1 = paymentItem1.findViewById(R.id.textPaymentStatus1);
                    View btnAccept1 = paymentItem1.findViewById(R.id.btnAcceptPayment1);

                    if (nameView1 != null) nameView1.setText(tenantName1);
                    if (detailsView1 != null) detailsView1.setText(houseName1 + " · " + roomNo1);
                    if (amountView1 != null) amountView1.setText("₱" + amount1);

                    if ("PAID".equals(status1) || "CONFIRMED".equals(status1)) {
                        if (statusView1 != null) statusView1.setText("Paid");
                        if (btnAccept1 != null) btnAccept1.setVisibility(View.GONE);
                    } else if ("SUBMITTED".equals(status1)) {
                        if (statusView1 != null) statusView1.setText("Awaiting Verification");
                        if (btnAccept1 != null) {
                            btnAccept1.setVisibility(View.VISIBLE);
                            if (btnAccept1 instanceof TextView) {
                                ((TextView) btnAccept1).setText("Review Payment");
                            }
                            final int payId1 = p1.optInt("payment_id", 0);
                            btnAccept1.setOnClickListener(v -> {
                                Intent intent = new Intent(LandlordHomeActivity.this, LandlordTransactionHistoryActivity.class);
                                if (payId1 > 0) {
                                    intent.putExtra("openPaymentId", payId1);
                                }
                                startActivity(intent);
                            });
                        }
                    } else {
                        if (statusView1 != null) statusView1.setText(status1);
                        if (btnAccept1 != null) btnAccept1.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {}
            } else {
                if (paymentItem1 != null) paymentItem1.setVisibility(View.GONE);
            }

            // Payment 2
            if (sortedPayments.size() > 1) {
                if (paymentItem2 != null) paymentItem2.setVisibility(View.VISIBLE);
                if (dividerView != null) dividerView.setVisibility(View.VISIBLE);
                try {
                    JSONObject p2 = sortedPayments.get(1);
                    String tenantName2 = p2.optString("tenant_name", "Student Tenant");
                    String houseName2 = p2.optString("house_name", "Boarding House");
                    String roomNo2 = p2.optString("room_number", "Room");
                    double amount2 = p2.optDouble("amount", 0);
                    String status2 = p2.optString("status", "PENDING").toUpperCase(Locale.ROOT);

                    TextView nameView2 = paymentItem2.findViewById(R.id.textPaymentTenantName2);
                    TextView detailsView2 = paymentItem2.findViewById(R.id.textPaymentDetails2);
                    TextView amountView2 = paymentItem2.findViewById(R.id.textPaymentAmount2);
                    TextView statusView2 = paymentItem2.findViewById(R.id.textPaymentStatus2);
                    View btnAccept2 = paymentItem2.findViewById(R.id.btnAcceptPayment2);

                    if (nameView2 != null) nameView2.setText(tenantName2);
                    if (detailsView2 != null) detailsView2.setText(houseName2 + " · " + roomNo2);
                    if (amountView2 != null) amountView2.setText("₱" + amount2);

                    if ("PAID".equals(status2) || "CONFIRMED".equals(status2)) {
                        if (statusView2 != null) statusView2.setText("Paid");
                        if (btnAccept2 != null) btnAccept2.setVisibility(View.GONE);
                    } else if ("SUBMITTED".equals(status2)) {
                        if (statusView2 != null) statusView2.setText("Awaiting Verification");
                        if (btnAccept2 != null) {
                            btnAccept2.setVisibility(View.VISIBLE);
                            if (btnAccept2 instanceof TextView) {
                                ((TextView) btnAccept2).setText("Review Payment");
                            }
                            final int payId2 = p2.optInt("payment_id", 0);
                            btnAccept2.setOnClickListener(v -> {
                                Intent intent = new Intent(LandlordHomeActivity.this, LandlordTransactionHistoryActivity.class);
                                if (payId2 > 0) {
                                    intent.putExtra("openPaymentId", payId2);
                                }
                                startActivity(intent);
                            });
                        }
                    } else {
                        if (statusView2 != null) statusView2.setText(status2);
                        if (btnAccept2 != null) btnAccept2.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {}
            } else {
                if (paymentItem2 != null) paymentItem2.setVisibility(View.GONE);
                if (dividerView != null) dividerView.setVisibility(View.GONE);
            }
        }
    }

    private void loadPendingRequests(int landlordId) {
        apiClient.getBookingsForLandlord(landlordId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray bookings = json.optJSONArray("data");
                        JSONArray pending = new JSONArray();
                        if (bookings != null) {
                            for (int i = 0; i < bookings.length(); i++) {
                                JSONObject b = bookings.getJSONObject(i);
                                String status = b.optString("status", "").toUpperCase();
                                if ("PENDING".equals(status)) {
                                    pending.put(b);
                                }
                            }
                        }
                        runOnUiThread(() -> bindPendingRequests(pending));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void bindPendingRequests(JSONArray pending) {
        View pendingSection = findViewById(R.id.pendingSection);
        View cardRequest1 = findViewById(R.id.cardRequest1);
        View cardRequest2 = findViewById(R.id.cardRequest2);

        if (pending.length() == 0) {
            if (pendingSection != null) pendingSection.setVisibility(View.GONE);
            if (cardRequest1 != null) cardRequest1.setVisibility(View.GONE);
            if (cardRequest2 != null) cardRequest2.setVisibility(View.GONE);
        } else {
            if (pendingSection != null) pendingSection.setVisibility(View.VISIBLE);
            if (cardRequest1 != null) {
                cardRequest1.setVisibility(View.VISIBLE);
                try {
                    JSONObject r1 = pending.getJSONObject(0);
                    int bookingId1 = r1.optInt("booking_id", 0);
                    int roomId1 = r1.optInt("room_id", 0);
                    String studentName = r1.optString("full_name", "Student Tenant");
                    String roomType = r1.optString("room_type", "Room");
                    double rent = r1.optDouble("agreed_monthly_rent", 0);

                    TextView name1 = cardRequest1.findViewById(R.id.textStudentName1);
                    TextView type1 = cardRequest1.findViewById(R.id.textRoomType1);
                    TextView rentView1 = cardRequest1.findViewById(R.id.textRent1);
                    TextView avatar1 = cardRequest1.findViewById(R.id.textAvatar1);
                    View btnApprove1 = cardRequest1.findViewById(R.id.btnApprove1);
                    View btnClose1 = cardRequest1.findViewById(R.id.btnClose1);
                    View btnChat1 = cardRequest1.findViewById(R.id.btnChat1);

                    if (name1 != null) name1.setText(studentName);
                    if (type1 != null) type1.setText(roomType);
                    if (rentView1 != null) rentView1.setText("₱" + rent + "/mo");
                    if (avatar1 != null) avatar1.setText(getInitials(studentName));

                    if (btnApprove1 != null) {
                        btnApprove1.setOnClickListener(v -> approveCard(cardRequest1, bookingId1, roomId1, studentName));
                    }
                    if (btnClose1 != null) {
                        btnClose1.setOnClickListener(v -> slideOutAndDiscard(cardRequest1, bookingId1));
                    }
                    if (btnChat1 != null) {
                        btnChat1.setOnClickListener(v -> {
                            Intent intent = new Intent(this, LandlordChatActivity.class);
                            startActivity(intent);
                        });
                    }
                } catch (Exception ignored) {}
            }
            if (cardRequest2 != null) {
                if (pending.length() > 1) {
                    cardRequest2.setVisibility(View.VISIBLE);
                    try {
                        JSONObject r2 = pending.getJSONObject(1);
                        int bookingId2 = r2.optInt("booking_id", 0);
                        int roomId2 = r2.optInt("room_id", 0);
                        String studentName2 = r2.optString("full_name", "Student Tenant");
                        String roomType2 = r2.optString("room_type", "Room");
                        double rent2 = r2.optDouble("agreed_monthly_rent", 0);

                        TextView name2 = cardRequest2.findViewById(R.id.textStudentName2);
                        TextView type2 = cardRequest2.findViewById(R.id.textRoomType2);
                        TextView rentView2 = cardRequest2.findViewById(R.id.textRent2);
                        TextView avatar2 = cardRequest2.findViewById(R.id.textAvatar2);
                        View btnApprove2 = cardRequest2.findViewById(R.id.btnApprove2);
                        View btnClose2 = cardRequest2.findViewById(R.id.btnClose2);
                        View btnChat2 = cardRequest2.findViewById(R.id.btnChat2);

                        if (name2 != null) name2.setText(studentName2);
                        if (type2 != null) type2.setText(roomType2);
                        if (rentView2 != null) rentView2.setText("₱" + rent2 + "/mo");
                        if (avatar2 != null) avatar2.setText(getInitials(studentName2));

                        if (btnApprove2 != null) {
                            btnApprove2.setOnClickListener(v -> approveCard(cardRequest2, bookingId2, roomId2, studentName2));
                        }
                        if (btnClose2 != null) {
                            btnClose2.setOnClickListener(v -> slideOutAndDiscard(cardRequest2, bookingId2));
                        }
                        if (btnChat2 != null) {
                            btnChat2.setOnClickListener(v -> {
                                Intent intent = new Intent(this, LandlordChatActivity.class);
                                startActivity(intent);
                            });
                        }
                    } catch (Exception ignored) {}
                } else {
                    cardRequest2.setVisibility(View.GONE);
                }
            }
        }
    }

    private void loadOccupancyStats(int landlordId) {
        apiClient.getRooms(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray rooms = json.optJSONArray("data");
                        if (rooms != null) {
                            int total = rooms.length();
                            int occupied = 0;
                            for (int i = 0; i < rooms.length(); i++) {
                                JSONObject room = rooms.getJSONObject(i);
                                String status = room.optString("status", "").toUpperCase();
                                if ("OCCUPIED".equals(status)) {
                                    occupied++;
                                }
                            }
                            int rate = total > 0 ? (occupied * 100) / total : 0;

                            runOnUiThread(() -> animateOccupancy(rate));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void animateOccupancy(int rate) {
        if (lastOccupancyRate == rate) {
            return;
        }

        CircularProgressIndicator occupancyProgressBar = findViewById(R.id.occupancyProgressBar);
        TextView occupancyPercentText = findViewById(R.id.occupancyPercentText);
        if (occupancyProgressBar != null && occupancyPercentText != null) {
            int startVal = lastOccupancyRate >= 0 ? lastOccupancyRate : 0;
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(occupancyProgressBar, "progress", startVal, rate);
            progressAnimator.setDuration(1000);
            progressAnimator.setInterpolator(new DecelerateInterpolator());
            progressAnimator.addUpdateListener(animation -> {
                int current = (int) animation.getAnimatedValue();
                occupancyPercentText.setText(current + "%");
            });
            progressAnimator.start();
            lastOccupancyRate = rate;
        }
    }

    private void setupUI() {
        View btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                TextView badge = findViewById(R.id.notificationBadge);
                if (badge != null) badge.setVisibility(View.GONE);
                Intent intent = new Intent(this, NotificationsActivity.class);
                startActivity(intent);
            });
        }

        View btnManageProperty = findViewById(R.id.btnManageProperty);
        if (btnManageProperty != null) {
            btnManageProperty.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordPropertiesActivity.class);
                startActivity(intent);
            });
        }

        View btnSeeAllRequests = findViewById(R.id.btnSeeAllRequests);
        if (btnSeeAllRequests != null) {
            btnSeeAllRequests.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordRequestsActivity.class);
                startActivity(intent);
            });
        }

        View btnHistory = findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                Intent intent = new Intent(this, LandlordTransactionHistoryActivity.class);
                startActivity(intent);
            });
        }

        View btnSwitchHouse = findViewById(R.id.btnSwitchHouse);
        if (btnSwitchHouse != null) {
            btnSwitchHouse.setOnClickListener(v -> {
                if (landlordHouses.length() > 1) {
                    currentHouseIndex = (currentHouseIndex + 1) % landlordHouses.length();
                    displayHouseAtIndex(currentHouseIndex);
                    try {
                        JSONObject h = landlordHouses.getJSONObject(currentHouseIndex);
                        Toast.makeText(this, "Switched to " + h.optString("house_name", "Property"), Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {}
                } else {
                    Toast.makeText(this, "Only 1 property registered", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void slideOutAndDiscard(View cardView, int bookingId) {
        apiClient.updateBookingStatus(bookingId, "DECLINED", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });

        ObjectAnimator animator = ObjectAnimator.ofFloat(cardView, "translationX", 0f, 1000f);
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardView.setVisibility(View.GONE);
                Toast.makeText(LandlordHomeActivity.this, "Request declined", Toast.LENGTH_SHORT).show();
            }
        });
        animator.start();
    }

    private void approveCard(View cardView, int bookingId, int roomId, String studentName) {
        apiClient.updateBookingStatus(bookingId, "APPROVED", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (roomId > 0) {
                    apiClient.updateRoomStatus(roomId, "OCCUPIED", new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {}

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
                            int landlordId = prefs.getInt("userId", -1);
                            if (landlordId > 0) {
                                runOnUiThread(() -> loadLandlordPropertiesAndStats(landlordId));
                            }
                        }
                    });
                }
            }
        });

        ObjectAnimator animator = ObjectAnimator.ofFloat(cardView, "alpha", 1f, 0f);
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardView.setVisibility(View.GONE);
                Toast.makeText(LandlordHomeActivity.this, "Accepted " + studentName, Toast.LENGTH_SHORT).show();
            }
        });
        animator.start();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        
        bottomNav.setSelectedItemId(R.id.nav_landlord_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_landlord_home) {
                return true;
            } else if (id == R.id.nav_landlord_add_house) {
                Intent intent = new Intent(this, AddBoardingHouseActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_landlord_chats) {
                Intent intent = new Intent(this, LandlordChatActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_landlord_profile) {
                Intent intent = new Intent(this, LandlordProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}