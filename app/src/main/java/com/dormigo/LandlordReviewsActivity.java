package com.dormigo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LandlordReviewsActivity extends AppCompatActivity {

    private int houseId;
    private String houseName;
    private SwipeRefreshLayout swipeRefreshLayout;

    private LinearLayout ratingSummaryCard;
    private TextView textAverageRating;
    private TextView textReviewCount;
    private LinearLayout layoutRatingBreakdown;
    private LinearLayout reviewsContainer;
    private View layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_reviews);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Intent intent = getIntent();
        houseId = intent.getIntExtra("HOUSE_ID", 0);
        houseName = intent.getStringExtra("PROPERTY_NAME");

        TextView houseNameLabel = findViewById(R.id.houseNameLabel);
        if (houseNameLabel != null && houseName != null && !houseName.isEmpty()) {
            houseNameLabel.setText(houseName);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ratingSummaryCard = findViewById(R.id.ratingSummaryCard);
        textAverageRating = findViewById(R.id.textAverageRating);
        textReviewCount = findViewById(R.id.textReviewCount);
        layoutRatingBreakdown = findViewById(R.id.layoutRatingBreakdown);
        reviewsContainer = findViewById(R.id.reviewsContainer);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadReviews);
        }

        loadReviews();
    }

    private void loadReviews() {
        if (houseId <= 0) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        String url = "http://10.242.38.109/Dormigo_Backend/api/reviews.php?house_id=" + houseId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(LandlordReviewsActivity.this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        double avgRating = json.optDouble("average_rating", 0.0);
                        int reviewCount = json.optInt("review_count", 0);
                        JSONArray data = json.optJSONArray("data");

                        runOnUiThread(() -> {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            renderSummaryAndReviews(avgRating, reviewCount, data);
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void renderSummaryAndReviews(double avgRating, int reviewCount, JSONArray reviews) {
        if (reviews == null || reviews.length() == 0) {
            if (ratingSummaryCard != null) ratingSummaryCard.setVisibility(View.GONE);
            if (reviewsContainer != null) reviewsContainer.removeAllViews();
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);

        // Display rating summary card
        if (ratingSummaryCard != null) {
            ratingSummaryCard.setVisibility(View.VISIBLE);
            if (textAverageRating != null) {
                textAverageRating.setText(String.format(Locale.US, "⭐ %.1f", avgRating));
            }
            if (textReviewCount != null) {
                textReviewCount.setText(String.format(Locale.US, "%d Review%s", reviewCount, reviewCount > 1 ? "s" : ""));
            }

            renderRatingBreakdown(reviews, reviewCount);
        }

        // Display review list
        if (reviewsContainer != null) {
            reviewsContainer.removeAllViews();

            for (int i = 0; i < reviews.length(); i++) {
                JSONObject r = reviews.optJSONObject(i);
                if (r != null) {
                    String studentName = r.optString("full_name", "Student");
                    int rating = r.optInt("rating", 5);
                    String comment = r.optString("comment", "");
                    String date = r.optString("created_at", "");
                    String vStatus = r.optString("verification_status", "");

                    boolean isVerified = "VERIFIED".equalsIgnoreCase(vStatus) || "APPROVED".equalsIgnoreCase(vStatus);

                    LinearLayout card = new LinearLayout(this);
                    card.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(16), dp(16), dp(16), dp(16));
                    card.setBackgroundResource(R.drawable.bg_card_rounded);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    params.bottomMargin = dp(12);
                    card.setLayoutParams(params);

                    // Header Row: Student Name + Verified Tenant Badge + Rating Stars
                    LinearLayout header = new LinearLayout(this);
                    header.setOrientation(LinearLayout.HORIZONTAL);
                    header.setGravity(Gravity.CENTER_VERTICAL);

                    TextView tvName = new TextView(this);
                    tvName.setText(studentName);
                    tvName.setTextColor(Color.parseColor("#1A1A1A"));
                    tvName.setTextSize(15);
                    tvName.setTypeface(null, Typeface.BOLD);
                    header.addView(tvName);

                    if (isVerified) {
                        TextView badge = new TextView(this);
                        badge.setText("🟢 Verified Tenant");
                        badge.setTextColor(Color.parseColor("#1B5E4C"));
                        badge.setTextSize(11);
                        badge.setTypeface(null, Typeface.BOLD);
                        badge.setBackgroundResource(R.drawable.bg_chip_white);
                        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
                        LinearLayout.LayoutParams bParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        bParams.setMarginStart(dp(8));
                        badge.setLayoutParams(bParams);
                        header.addView(badge);
                    }

                    TextView tvStars = new TextView(this);
                    tvStars.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    tvStars.setGravity(Gravity.END);
                    tvStars.setText(getStarString(rating));
                    tvStars.setTextColor(Color.parseColor("#D97706"));
                    tvStars.setTextSize(14);
                    header.addView(tvStars);

                    card.addView(header);

                    // Review Comment
                    if (!comment.trim().isEmpty() && !"null".equalsIgnoreCase(comment.trim())) {
                        TextView tvComment = new TextView(this);
                        tvComment.setText(comment.trim());
                        tvComment.setTextColor(Color.parseColor("#1A1A1A"));
                        tvComment.setTextSize(14);
                        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cParams.topMargin = dp(8);
                        tvComment.setLayoutParams(cParams);
                        card.addView(tvComment);
                    }

                    // Review Date
                    if (!date.isEmpty()) {
                        TextView tvDate = new TextView(this);
                        tvDate.setText(formatDate(date));
                        tvDate.setTextColor(Color.parseColor("#9A9A9E"));
                        tvDate.setTextSize(11);
                        LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        dParams.topMargin = dp(8);
                        tvDate.setLayoutParams(dParams);
                        card.addView(tvDate);
                    }

                    reviewsContainer.addView(card);
                }
            }
        }
    }

    private void renderRatingBreakdown(JSONArray reviews, int totalCount) {
        if (layoutRatingBreakdown == null) return;
        layoutRatingBreakdown.removeAllViews();

        int[] counts = new int[6]; // 1 to 5
        for (int i = 0; i < reviews.length(); i++) {
            JSONObject r = reviews.optJSONObject(i);
            if (r != null) {
                int star = r.optInt("rating", 5);
                if (star >= 1 && star <= 5) {
                    counts[star]++;
                }
            }
        }

        for (int star = 5; star >= 1; star--) {
            int cnt = counts[star];
            if (cnt == 0 && totalCount > 0) continue; // Only show stars that have count or if all 0

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(2), 0, dp(2));

            TextView starLabel = new TextView(this);
            starLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.WRAP_CONTENT));
            starLabel.setText(star + " " + getStarString(star));
            starLabel.setTextColor(Color.parseColor("#D97706"));
            starLabel.setTextSize(12);

            TextView countLabel = new TextView(this);
            countLabel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            countLabel.setPadding(dp(12), 0, 0, 0);
            countLabel.setText(String.valueOf(cnt));
            countLabel.setTextColor(Color.parseColor("#6E6E73"));
            countLabel.setTextSize(12);

            row.addView(starLabel);
            row.addView(countLabel);
            layoutRatingBreakdown.addView(row);
        }
    }

    private String getStarString(double rating) {
        int r = (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < r) sb.append("★");
            else sb.append("☆");
        }
        return sb.toString();
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";
        try {
            if (rawDate.length() >= 10) {
                return rawDate.substring(0, 10);
            }
        } catch (Exception ignored) {}
        return rawDate;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
