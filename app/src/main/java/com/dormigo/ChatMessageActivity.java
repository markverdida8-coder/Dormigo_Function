package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatMessageActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int currentSenderId;
    private int currentOtherId;
    private int houseId = 0;
    private String houseName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_message);

        apiClient = new ApiClient();

        // Adjust for system bars
        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupUI();
    }

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentSenderId > 0 && currentOtherId > 0) {
                loadChatThread(currentSenderId, currentOtherId);
            }
            pollHandler.postDelayed(this, 2500);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.postDelayed(pollRunnable, 2500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void loadChatThread(int currentUserId, int otherUserId) {
        apiClient.getChatThread(currentUserId, otherUserId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray messages = json.optJSONArray("data");
                        if (messages != null) {
                            for (int i = 0; i < messages.length(); i++) {
                                JSONObject msg = messages.optJSONObject(i);
                                if (msg != null) {
                                    int mHouseId = msg.optInt("house_id", 0);
                                    if (mHouseId > 0 && houseId <= 0) {
                                        houseId = mHouseId;
                                    }
                                }
                            }
                            runOnUiThread(() -> renderMessages(messages, currentUserId));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        if (houseId <= 0 && otherUserId > 0) {
            apiClient.getBoardingHouses(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) return;
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            JSONArray houses = json.optJSONArray("data");
                            if (houses != null) {
                                for (int i = 0; i < houses.length(); i++) {
                                    JSONObject h = houses.optJSONObject(i);
                                    if (h != null && h.optInt("landlord_id", 0) == otherUserId) {
                                        houseId = h.optInt("house_id", 0);
                                        if (houseName == null || houseName.isEmpty() || houseName.equals("Boarding House")) {
                                            houseName = h.optString("house_name", "Boarding House");
                                        }
                                        runOnUiThread(() -> {
                                            TextView houseLabel = findViewById(R.id.chatHouseName);
                                            if (houseLabel != null && houseName != null) {
                                                houseLabel.setText(houseName + " • Verified Landlord");
                                            }
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
    }

    private void renderMessages(JSONArray messages, int currentUserId) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        LinearLayout container = findViewById(R.id.messagesContainer);
        if (container == null) return;
        container.removeAllViews();

        if (messages.length() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("💬\n\nNo messages yet.\nStart the conversation by sending a message.");
            emptyText.setTextColor(0xFF9A9A9E);
            emptyText.setTextSize(14);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dp(60), 0, 0);
            container.addView(emptyText);
            return;
        }

        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject msg = messages.getJSONObject(i);
                int senderId = msg.optInt("sender_id", 0);
                String text = msg.optString("message_text", "");
                String time = msg.optString("created_at", "");
                time = formatMessageTime(time);

                boolean isSentByMe = (senderId == currentUserId);

                LinearLayout outerLayout = new LinearLayout(this);
                outerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                outerLayout.setOrientation(LinearLayout.VERTICAL);
                outerLayout.setGravity(isSentByMe ? Gravity.END : Gravity.START);
                outerLayout.setPadding(0, dp(8), 0, dp(8));

                LinearLayout bubble = new LinearLayout(this);
                LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                if (isSentByMe) {
                    bubbleParams.setMarginStart(dp(48));
                    bubble.setBackgroundResource(R.drawable.bg_chat_bubble_sent);
                } else {
                    bubbleParams.setMarginEnd(dp(48));
                    bubble.setBackgroundResource(R.drawable.bg_chat_bubble_received);
                }
                bubble.setLayoutParams(bubbleParams);
                bubble.setOrientation(LinearLayout.VERTICAL);
                bubble.setPadding(dp(16), dp(12), dp(16), dp(12));

                TextView tvText = new TextView(this);
                tvText.setText(text);
                tvText.setTextColor(isSentByMe ? 0xFFFFFFFF : 0xFF1A1A1A);
                tvText.setTextSize(14);
                tvText.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75));
                tvText.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                TextView tvTime = new TextView(this);
                boolean isRead = msg.optBoolean("is_read", false);
                String timeText = time;
                if (isSentByMe) {
                    timeText += isRead ? " · Read" : " · Sent";
                }
                tvTime.setText(timeText);
                tvTime.setTextColor(isSentByMe ? 0xFFA0C6BC : 0xFF9A9A9E);
                tvTime.setTextSize(11);
                LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                timeParams.topMargin = dp(4);
                tvTime.setLayoutParams(timeParams);

                bubble.addView(tvText);
                if (!time.isEmpty()) {
                    bubble.addView(tvTime);
                }
                outerLayout.addView(bubble);
                container.addView(outerLayout);
            } catch (Exception ignored) {}
        }

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private String formatMessageTime(String createdAt) {
        if (createdAt == null || createdAt.length() < 16) return "";
        try {
            String timePart = createdAt.substring(11, 16);
            String[] parts = timePart.split(":");
            if (parts.length == 2) {
                int hour = Integer.parseInt(parts[0]);
                String minute = parts[1];
                String amPm = hour >= 12 ? "PM" : "AM";
                if (hour == 0) {
                    hour = 12;
                } else if (hour > 12) {
                    hour -= 12;
                }
                return hour + ":" + minute + " " + amPm;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private boolean isMutedState = false;

    private void loadMuteStatus() {
        if (currentSenderId <= 0 || currentOtherId <= 0) return;
        apiClient.getMuteStatus(currentSenderId, currentOtherId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    boolean muted = json.optBoolean("is_muted", false);
                    runOnUiThread(() -> updateMuteUI(muted));
                } catch (Exception ignored) {}
            }
        });
    }

    private void updateMuteUI(boolean isMuted) {
        this.isMutedState = isMuted;
        View muteBanner = findViewById(R.id.muteBanner);
        if (muteBanner != null) {
            muteBanner.setVisibility(isMuted ? View.VISIBLE : View.GONE);
        }
    }

    private void showMuteDurationDialog() {
        String[] options = {"1 Hour", "8 Hours", "24 Hours", "Until I Turn It Back On"};
        int[] hours = {1, 8, 24, 876000};

        new AlertDialog.Builder(this)
                .setTitle("Mute Notifications For")
                .setItems(options, (dialog, which) -> {
                    int duration = hours[which];
                    apiClient.muteConversation(currentSenderId, currentOtherId, duration, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() -> Toast.makeText(ChatMessageActivity.this, "Unable to mute chat.", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            runOnUiThread(() -> {
                                updateMuteUI(true);
                                Toast.makeText(ChatMessageActivity.this, "Conversation muted.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unmuteChat() {
        apiClient.unmuteConversation(currentSenderId, currentOtherId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ChatMessageActivity.this, "Unable to unmute chat.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    updateMuteUI(false);
                    Toast.makeText(ChatMessageActivity.this, "Conversation unmuted.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupUI() {
        Intent intent = getIntent();
        String landlordName = intent.getStringExtra("LANDLORD_NAME");
        this.houseName = intent.getStringExtra("HOUSE_NAME");
        currentOtherId = intent.getIntExtra("LANDLORD_ID", 2);
        this.houseId = intent.getIntExtra("HOUSE_ID", 0);
        currentSenderId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).getInt("userId", 1);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> loadChatThread(currentSenderId, currentOtherId));
        }

        loadChatThread(currentSenderId, currentOtherId);
        loadMuteStatus();

        View btnUnmuteBanner = findViewById(R.id.btnUnmuteBanner);
        if (btnUnmuteBanner != null) {
            btnUnmuteBanner.setOnClickListener(v -> unmuteChat());
        }

        if (landlordName != null) {
            TextView nameLabel = findViewById(R.id.chatLandlordName);
            if (nameLabel != null) nameLabel.setText(landlordName);

            TextView avatar = findViewById(R.id.headerAvatar);
            if (avatar != null) {
                avatar.setText(getInitials(landlordName));
            }
        }

        String roomType = intent.getStringExtra("ROOM_TYPE");
        String houseStr = (houseName != null && !houseName.isEmpty()) ? houseName : "Boarding House";
        String subtitle = houseStr + " • " + (roomType != null && !roomType.isEmpty() ? roomType : "Verified Landlord");
        TextView houseLabel = findViewById(R.id.chatHouseName);
        if (houseLabel != null) houseLabel.setText(subtitle);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnOverflowMenu = findViewById(R.id.btnOverflowMenu);
        if (btnOverflowMenu != null) {
            btnOverflowMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenu().add("👤 View Landlord Profile");
                popup.getMenu().add(isMutedState ? "🔔 Unmute Conversation" : "🔕 Mute Conversation");
                popup.getMenu().add("🗑 Clear Conversation");

                popup.setOnMenuItemClickListener(item -> {
                    CharSequence title = item.getTitle();
                    if (title != null) {
                        String str = title.toString();
                        if (str.contains("View Landlord Profile")) {
                            Intent profileIntent = new Intent(this, LandlordProfileViewActivity.class);
                            profileIntent.putExtra("LANDLORD_ID", currentOtherId);
                            profileIntent.putExtra("HOUSE_ID", houseId);
                            profileIntent.putExtra("LANDLORD_NAME", landlordName);
                            profileIntent.putExtra("HOUSE_NAME", houseName);
                            startActivity(profileIntent);
                        } else if (str.contains("Unmute")) {
                            unmuteChat();
                        } else if (str.contains("Mute")) {
                            showMuteDurationDialog();
                        } else if (str.contains("Clear Conversation")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Clear Conversation?")
                                    .setMessage("This will permanently remove all messages in this conversation.\n\nThis action cannot be undone.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Clear", (dialog, which) -> {
                                        apiClient.clearConversationThread(currentSenderId, currentOtherId, new Callback() {
                                            @Override
                                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                runOnUiThread(() -> Toast.makeText(ChatMessageActivity.this, "Failed to clear conversation.", Toast.LENGTH_SHORT).show());
                                            }

                                            @Override
                                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(ChatMessageActivity.this, "Conversation cleared.", Toast.LENGTH_SHORT).show();
                                                    loadChatThread(currentSenderId, currentOtherId);
                                                });
                                            }
                                        });
                                    })
                                    .show();
                        }
                    }
                    return true;
                });
                popup.show();
            });
        }

        EditText chatInput = findViewById(R.id.chatInput);
        ImageView btnSend = findViewById(R.id.btnSend);

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                if (chatInput != null) {
                    String message = chatInput.getText().toString().trim();
                    if (!message.isEmpty()) {
                        Integer hId = (houseId > 0) ? houseId : null;
                        apiClient.sendMessage(currentSenderId, currentOtherId, hId, message, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> Toast.makeText(ChatMessageActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                runOnUiThread(() -> {
                                    chatInput.setText("");
                                    loadChatThread(currentSenderId, currentOtherId);
                                });
                            }
                        });
                    }
                }
            });
        }

        // Quick replies
        int[] chipIds = {R.id.chipReply1, R.id.chipReply2, R.id.chipReply3};
        for (int id : chipIds) {
            TextView chip = findViewById(id);
            if (chip != null && chatInput != null) {
                chip.setOnClickListener(v -> {
                    chatInput.setText(chip.getText().toString());
                    chatInput.setSelection(chatInput.getText().length());
                });
            }
        }
    }


}