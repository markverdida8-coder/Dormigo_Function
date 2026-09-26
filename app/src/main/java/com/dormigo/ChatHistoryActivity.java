package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatHistoryActivity extends AppCompatActivity {

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_history);

        apiClient = new ApiClient();

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

        setupSearch();
        setupBottomNavigation();
        loadLiveChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLiveChats();
        pollHandler.postDelayed(pollRunnable, 2500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadLiveChats();
            pollHandler.postDelayed(this, 2500);
        }
    };

    private void loadLiveChats() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return;

        apiClient.getChatConversations(userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray chats = json.optJSONArray("data");
                        runOnUiThread(() -> renderChatList(chats != null ? chats : new JSONArray(), false));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void setupSearch() {
        EditText searchInput = findViewById(R.id.chatSearchInput);
        if (searchInput == null) return;

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    loadLiveChats();
                    return;
                }

                apiClient.getUsers(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {}

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) return;
                        String body = response.body() != null ? response.body().string() : "";
                        try {
                            JSONObject json = new JSONObject(body);
                            if (json.optBoolean("success", false)) {
                                JSONArray users = json.optJSONArray("data");
                                JSONArray filtered = new JSONArray();
                                if (users != null) {
                                    for (int i = 0; i < users.length(); i++) {
                                        JSONObject u = users.getJSONObject(i);
                                        String name = u.optString("full_name", "");
                                        String userType = u.optString("user_type", "");
                                        if (name.toLowerCase().contains(query) && userType.equalsIgnoreCase("LANDLORD")) {
                                            filtered.put(u);
                                        }
                                    }
                                }
                                runOnUiThread(() -> renderChatList(filtered, true));
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void renderChatList(JSONArray items, boolean isUserSearch) {
        LinearLayout container = findViewById(R.id.chatListContainer);
        if (container == null) return;
        container.removeAllViews();

        if (items.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText(isUserSearch ? "No landlords found." : "No conversations yet. Use search above to find landlords.");
            empty.setTextColor(0xFF6E6E73);
            empty.setTextSize(14);
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            container.addView(empty);
            return;
        }

        for (int i = 0; i < items.length(); i++) {
            try {
                JSONObject obj = items.getJSONObject(i);
                int otherId = isUserSearch ? obj.optInt("user_id", 0) : obj.optInt("other_user_id", 0);
                String name = isUserSearch ? obj.optString("full_name", "") : obj.optString("other_user_name", "");
                String lastMsg = isUserSearch ? obj.optString("email", "") : obj.optString("message_text", "Tap to chat");

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setGravity(Gravity.CENTER_VERTICAL);
                itemLayout.setPadding(dp(16), dp(16), dp(16), dp(16));

                TextView avatar = new TextView(this);
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(40), dp(40));
                avatar.setLayoutParams(avatarParams);
                avatar.setGravity(Gravity.CENTER);
                avatar.setBackgroundResource(R.drawable.bg_circle_green);
                String initials = name.isEmpty() ? "L" : name.substring(0, Math.min(2, name.length())).toUpperCase();
                avatar.setText(initials);
                avatar.setTextColor(0xFFFFFFFF);
                avatar.setTextSize(12);
                avatar.setTypeface(null, Typeface.BOLD);

                LinearLayout textLayout = new LinearLayout(this);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
                textParams.setMarginStart(dp(12));
                textLayout.setLayoutParams(textParams);
                textLayout.setOrientation(LinearLayout.VERTICAL);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextColor(0xFF1A1A1A);
                tvName.setTextSize(15);
                tvName.setTypeface(null, Typeface.BOLD);

                TextView tvSub = new TextView(this);
                tvSub.setText(lastMsg);
                tvSub.setTextColor(0xFF6E6E73);
                tvSub.setTextSize(13);
                tvSub.setSingleLine(true);
                tvSub.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                subParams.topMargin = dp(4);
                tvSub.setLayoutParams(subParams);

                boolean isRead = isUserSearch || obj.optBoolean("is_read", true);
                if (!isRead) {
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvSub.setTypeface(null, Typeface.BOLD);
                    tvSub.setTextColor(0xFF1B5E4C);
                } else {
                    tvName.setTypeface(null, Typeface.NORMAL);
                    tvSub.setTypeface(null, Typeface.NORMAL);
                    tvSub.setTextColor(0xFF6E6E73);
                }

                textLayout.addView(tvName);
                textLayout.addView(tvSub);

                itemLayout.addView(avatar);
                itemLayout.addView(textLayout);

                itemLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ChatMessageActivity.class);
                    intent.putExtra("LANDLORD_ID", otherId);
                    intent.putExtra("LANDLORD_NAME", name);
                    intent.putExtra("HOUSE_NAME", "Boarding House");
                    startActivity(intent);
                });

                container.addView(itemLayout);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
                divider.setBackgroundColor(0xFFEFEFEF);
                container.addView(divider);

            } catch (Exception ignored) {}
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        
        bottomNav.setSelectedItemId(R.id.nav_chats);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_explore) {
                Intent intent = new Intent(this, BoardingHouseListingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_requests) {
                Intent intent = new Intent(this, BookingRequestsActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return true;
        });
    }
}
