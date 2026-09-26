package com.dormigo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LandlordChatMessageActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int currentLandlordId;
    private int currentStudentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_chat_message);

        apiClient = new ApiClient();

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
            if (currentLandlordId > 0 && currentStudentId > 0) {
                loadChatThread(currentLandlordId, currentStudentId);
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
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray messages = json.optJSONArray("data");
                        if (messages != null) {
                            runOnUiThread(() -> renderMessages(messages, currentUserId));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void renderMessages(JSONArray messages, int currentUserId) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        LinearLayout container = findViewById(R.id.messagesContainer);
        if (container == null) return;
        container.removeAllViews();

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

    private void setupUI() {
        Intent intent = getIntent();
        String studentName = intent.getStringExtra("STUDENT_NAME");
        String roomInfo = intent.getStringExtra("ROOM_INFO");
        currentStudentId = intent.getIntExtra("STUDENT_ID", 1);
        currentLandlordId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE).getInt("userId", 2);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> loadChatThread(currentLandlordId, currentStudentId));
        }

        loadChatThread(currentLandlordId, currentStudentId);

        if (studentName != null) {
            TextView nameLabel = findViewById(R.id.chatStudentName);
            if (nameLabel != null) nameLabel.setText(studentName);
        }

        if (roomInfo != null) {
            TextView roomLabel = findViewById(R.id.chatRoomInfo);
            if (roomLabel != null) roomLabel.setText(roomInfo);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        EditText chatInput = findViewById(R.id.chatInput);
        ImageView btnSend = findViewById(R.id.btnSend);

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                if (chatInput != null) {
                    String message = chatInput.getText().toString().trim();
                    if (!message.isEmpty()) {
                        apiClient.sendMessage(currentLandlordId, currentStudentId, null, message, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> Toast.makeText(LandlordChatMessageActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                runOnUiThread(() -> {
                                    chatInput.setText("");
                                    loadChatThread(currentLandlordId, currentStudentId);
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