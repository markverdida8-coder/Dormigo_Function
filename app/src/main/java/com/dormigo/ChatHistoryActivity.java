package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatHistoryActivity extends AppCompatActivity implements ChatConversationAdapter.OnConversationClickListener {

    private ApiClient apiClient;
    private final List<JSONObject> conversationList = new ArrayList<>();
    private final List<JSONObject> filteredList = new ArrayList<>();
    private ChatConversationAdapter adapter;
    private RecyclerView recyclerView;
    private String lastResponseJson = "";
    private String currentSearchQuery = "";
    private boolean isUserSearchMode = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadLiveChats();
            pollHandler.postDelayed(this, 2500);
        }
    };

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

        setupRecyclerView();
        setupSearch();
        setupBottomNavigation();
        setupBackPressed();
        loadLiveChats();
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isUserSearchMode || !currentSearchQuery.isEmpty()) {
                    exitSearchMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLiveChats();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 2500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewChats);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            adapter = new ChatConversationAdapter(this, filteredList, this);
            recyclerView.setAdapter(adapter);
        }
    }

    private void loadLiveChats() {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return;

        if (isUserSearchMode && !currentSearchQuery.isEmpty()) {
            return;
        }

        apiClient.getChatConversations(userId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    String body = response.body().string();
                    if (body.equals(lastResponseJson)) return;
                    lastResponseJson = body;

                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray chats = json.optJSONArray("data");
                        runOnUiThread(() -> updateChatData(chats != null ? chats : new JSONArray(), false));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void setupSearch() {
        EditText searchInput = findViewById(R.id.chatSearchInput);
        View btnClear = findViewById(R.id.btnClearSearch);

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> exitSearchMode());
        }

        if (searchInput == null) return;

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                if (currentSearchQuery.isEmpty()) {
                    exitSearchMode();
                    return;
                }

                if (btnClear != null) {
                    btnClear.setVisibility(View.VISIBLE);
                }

                isUserSearchMode = true;
                performCombinedSearch(currentSearchQuery, "LANDLORD");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void exitSearchMode() {
        isUserSearchMode = false;
        currentSearchQuery = "";
        EditText searchInput = findViewById(R.id.chatSearchInput);
        if (searchInput != null && !searchInput.getText().toString().isEmpty()) {
            searchInput.setText("");
            searchInput.clearFocus();
        }
        View btnClear = findViewById(R.id.btnClearSearch);
        if (btnClear != null) {
            btnClear.setVisibility(View.GONE);
        }
        filteredList.clear();
        filteredList.addAll(conversationList);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        loadLiveChats();
    }

    private void performCombinedSearch(String query, String targetRole) {
        SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
        int currentUserId = prefs.getInt("userId", -1);

        // Step 1: Filter existing active conversations
        List<JSONObject> matchedChats = new ArrayList<>();
        Set<Integer> existingUserIds = new HashSet<>();

        for (JSONObject item : conversationList) {
            String name = item.optString("other_user_name", "");
            int otherId = item.optInt("other_user_id", 0);
            if (otherId > 0) existingUserIds.add(otherId);

            if (name.toLowerCase().contains(query)) {
                matchedChats.add(item);
            }
        }

        // Step 2: Query eligible users from users.php
        String url = "http://10.209.52.109/Dormigo_Backend/api/users.php?search=" + Uri.encode(query) + "&user_type=" + targetRole + "&exclude_user_id=" + currentUserId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> renderSearchCombinedResults(matchedChats, new ArrayList<>()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<JSONObject> matchedPeople = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            JSONArray users = json.optJSONArray("data");
                            if (users != null) {
                                for (int i = 0; i < users.length(); i++) {
                                    JSONObject u = users.optJSONObject(i);
                                    if (u != null) {
                                        int uid = u.optInt("user_id", 0);
                                        if (uid > 0 && !existingUserIds.contains(uid)) {
                                            u.put("is_people_user", true);
                                            matchedPeople.add(u);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                runOnUiThread(() -> renderSearchCombinedResults(matchedChats, matchedPeople));
            }
        });
    }

    private void renderSearchCombinedResults(List<JSONObject> chats, List<JSONObject> people) {
        filteredList.clear();

        if (chats.isEmpty() && people.isEmpty()) {
            try {
                JSONObject emptyHeader = new JSONObject();
                emptyHeader.put("is_section_header", true);
                emptyHeader.put("header_title", "No users found.");
                filteredList.add(emptyHeader);
            } catch (Exception ignored) {}
        } else {
            if (!chats.isEmpty()) {
                try {
                    JSONObject header1 = new JSONObject();
                    header1.put("is_section_header", true);
                    header1.put("header_title", "Existing Conversations");
                    filteredList.add(header1);
                } catch (Exception ignored) {}
                filteredList.addAll(chats);
            }

            if (!people.isEmpty()) {
                try {
                    JSONObject header2 = new JSONObject();
                    header2.put("is_section_header", true);
                    header2.put("header_title", "People");
                    filteredList.add(header2);
                } catch (Exception ignored) {}
                filteredList.addAll(people);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateChatData(JSONArray items, boolean isUserSearch) {
        if (!isUserSearch) {
            conversationList.clear();
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.optJSONObject(i);
                if (obj != null) {
                    conversationList.add(obj);
                }
            }
            if (!isUserSearchMode) {
                filteredList.clear();
                filteredList.addAll(conversationList);
            }
        } else {
            filteredList.clear();
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.optJSONObject(i);
                if (obj != null) {
                    filteredList.add(obj);
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConversationClick(JSONObject chatItem, int position) {
        if (chatItem.optBoolean("is_section_header", false)) {
            return;
        }

        boolean isPeopleUser = chatItem.optBoolean("is_people_user", false);
        int otherId = isPeopleUser ? chatItem.optInt("user_id", 0) : chatItem.optInt("other_user_id", 0);
        String name = isPeopleUser ? chatItem.optString("full_name", "") : chatItem.optString("other_user_name", "");
        int houseId = chatItem.optInt("house_id", 0);
        String houseName = chatItem.optString("house_name", "Boarding House");

        if (isPeopleUser && houseId <= 0) {
            apiClient.getBoardingHouses(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    launchChat(otherId, name, 0, houseName);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        launchChat(otherId, name, 0, houseName);
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.optBoolean("success", false)) {
                            JSONArray data = json.optJSONArray("data");
                            if (data != null) {
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject h = data.optJSONObject(i);
                                    if (h != null && h.optInt("landlord_id", 0) == otherId) {
                                        int hId = h.optInt("house_id", 0);
                                        String hName = h.optString("house_name", houseName);
                                        runOnUiThread(() -> launchChat(otherId, name, hId, hName));
                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    runOnUiThread(() -> launchChat(otherId, name, 0, houseName));
                }
            });
        } else {
            launchChat(otherId, name, houseId, houseName);
        }
    }

    private void launchChat(int otherId, String name, int houseId, String houseName) {
        Intent intent = new Intent(this, ChatMessageActivity.class);
        intent.putExtra("LANDLORD_ID", otherId);
        intent.putExtra("LANDLORD_NAME", name);
        intent.putExtra("HOUSE_ID", houseId);
        intent.putExtra("HOUSE_NAME", houseName);
        startActivity(intent);
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
