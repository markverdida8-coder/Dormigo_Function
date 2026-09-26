package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MyReviewsActivity extends AppCompatActivity {

    private final ApiClient apiClient =
            new ApiClient();

    private LinearLayout reviewsContainer;

    private TextView reviewsCount;
    private TextView averageRatingText;
    private TextView reviewStatsText;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_my_reviews
        );

        bindViews();

        setupWindowInsets();

        setupBackButton();

        setupWriteReviewButton();
    }

    @Override
    protected void onResume() {

        super.onResume();

        loadReviews();
    }

    // =========================================================
    // BIND VIEWS
    // =========================================================

    private void bindViews() {

        reviewsContainer =
                findViewById(
                        R.id.reviewsContainer
                );

        reviewsCount =
                findViewById(
                        R.id.reviewsCount
                );

        averageRatingText =
                findViewById(
                        R.id.averageRatingText
                );

        reviewStatsText =
                findViewById(
                        R.id.reviewStatsText
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
                                dp(12),
                                dp(16),
                                systemBars.bottom
                                        + dp(12)
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

            btnBack.setOnClickListener(v ->
                    finish()
            );
        }
    }

    // =========================================================
    // WRITE REVIEW
    // =========================================================

    private void setupWriteReviewButton() {

        View btnWriteReview =
                findViewById(
                        R.id.btnWriteReview
                );

        if (btnWriteReview == null) {
            return;
        }

        btnWriteReview.setOnClickListener(v -> {
            Log.d("WRITE_REVIEW", "Write Review button clicked");
            Toast.makeText(this, "Write Review button clicked", Toast.LENGTH_SHORT).show();

            SharedPreferences prefs = getSharedPreferences("DormigoPrefs", MODE_PRIVATE);
            int userId = prefs.getInt("userId", 0);
            if (userId <= 0) {
                Toast.makeText(this, "Please log in.", Toast.LENGTH_SHORT).show();
                return;
            }

            apiClient.getEligibleReviews(userId, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("WRITE_REVIEW", "Eligibility check failed", e);
                    runOnUiThread(() -> Toast.makeText(MyReviewsActivity.this, "Unable to check review eligibility.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d("WRITE_REVIEW", "Eligibility response: " + body);
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray data = json.optJSONArray("data");
                        runOnUiThread(() -> {
                            if (data == null || data.length() == 0) {
                                new AlertDialog.Builder(MyReviewsActivity.this)
                                        .setTitle("Reviews Unavailable")
                                        .setMessage("Reviews are available only after a completed stay at a boarding house.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else if (data.length() == 1) {
                                try {
                                    JSONObject house = data.getJSONObject(0);
                                    int houseId = house.optInt("house_id", 0);
                                    String houseName = house.optString("house_name", "");
                                    Intent intent = new Intent(MyReviewsActivity.this, WriteReviewActivity.class);
                                    intent.putExtra("HOUSE_ID", houseId);
                                    intent.putExtra("HOUSE_NAME", houseName);
                                    startActivity(intent);
                                } catch (Exception ignored) {}
                            } else {
                                try {
                                    String[] names = new String[data.length()];
                                    int[] houseIds = new int[data.length()];
                                    for (int i = 0; i < data.length(); i++) {
                                        JSONObject h = data.getJSONObject(i);
                                        houseIds[i] = h.optInt("house_id", 0);
                                        names[i] = h.optString("house_name", "Boarding House");
                                    }
                                    new AlertDialog.Builder(MyReviewsActivity.this)
                                            .setTitle("Select Boarding House to Review")
                                            .setItems(names, (dialog, which) -> {
                                                Intent intent = new Intent(MyReviewsActivity.this, WriteReviewActivity.class);
                                                intent.putExtra("HOUSE_ID", houseIds[which]);
                                                intent.putExtra("HOUSE_NAME", names[which]);
                                                startActivity(intent);
                                            })
                                            .setNegativeButton("Cancel", null)
                                            .show();
                                } catch (Exception ignored) {}
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MyReviewsActivity.this, "Error checking eligibility.", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }

    // =========================================================
    // LOAD REVIEWS
    // =========================================================

    private void loadReviews() {

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

            showEmptyState(
                    "Please log in to view your reviews."
            );

            return;
        }

        apiClient.getReviews(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() ->
                                showEmptyState(
                                        "Unable to load your reviews."
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

                                boolean success =
                                        json.optBoolean(
                                                "success",
                                                false
                                        );

                                if (!success) {

                                    showEmptyState(
                                            json.optString(
                                                    "message",
                                                    "Unable to load reviews."
                                            )
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (data == null) {

                                    showEmptyState(
                                            "You have not written any reviews yet."
                                    );

                                    return;
                                }

                                JSONArray myReviews =
                                        new JSONArray();

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject review =
                                            data.getJSONObject(
                                                    i
                                            );

                                    int reviewUserId =
                                            review.optInt(
                                                    "user_id",
                                                    0
                                            );

                                    if (
                                            reviewUserId
                                                    == userId
                                    ) {

                                        myReviews.put(
                                                review
                                        );
                                    }
                                }

                                if (
                                        myReviews.length()
                                                == 0
                                ) {

                                    showEmptyState(
                                            "You have not written any reviews yet."
                                    );

                                    return;
                                }

                                loadBoardingHouses(
                                        myReviews
                                );

                            } catch (Exception e) {

                                showEmptyState(
                                        "Invalid review response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // LOAD BOARDING HOUSE NAMES
    // =========================================================

    private void loadBoardingHouses(
            JSONArray reviews
    ) {

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        renderReviews(
                                reviews,
                                new HashMap<>()
                        );
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {

                        Map<Integer, String> houseMap =
                                new HashMap<>();

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

                            JSONArray data =
                                    json.optJSONArray(
                                            "data"
                                    );

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

                                    int houseId =
                                            house.optInt(
                                                    "house_id",
                                                    0
                                            );

                                    String houseName =
                                            house.optString(
                                                    "house_name",
                                                    "Boarding House"
                                            );

                                    if (houseId > 0) {

                                        houseMap.put(
                                                houseId,
                                                houseName
                                        );
                                    }
                                }
                            }

                        } catch (Exception ignored) {
                        }

                        renderReviews(
                                reviews,
                                houseMap
                        );
                    }
                }
        );
    }

    // =========================================================
    // RENDER REVIEWS
    // =========================================================

    private void renderReviews(
            JSONArray reviews,
            Map<Integer, String> houseMap
    ) {

        runOnUiThread(() -> {

            if (reviewsContainer == null) {
                return;
            }

            reviewsContainer.removeAllViews();

            int count =
                    reviews.length();

            updateReviewSummary(
                    reviews
            );

            if (reviewsCount != null) {

                reviewsCount.setText(
                        count == 1
                                ? "1 review"
                                : count + " reviews"
                );
            }

            for (
                    int i = 0;
                    i < reviews.length();
                    i++
            ) {

                try {

                    JSONObject review =
                            reviews.getJSONObject(
                                    i
                            );

                    createReviewCard(
                            review,
                            houseMap,
                            i
                    );

                } catch (Exception ignored) {
                }
            }
        });
    }

    // =========================================================
    // UPDATE GREEN SUMMARY CARD
    // =========================================================

    private void updateReviewSummary(
            JSONArray reviews
    ) {

        int count =
                reviews.length();

        if (averageRatingText != null) {

            averageRatingText.setText(
                    String.valueOf(count)
            );
        }

        if (reviewStatsText != null) {

            reviewStatsText.setText(
                    count == 1
                            ? "Review Submitted"
                            : "Reviews Submitted"
            );
        }
    }

    // =========================================================
    // CREATE REVIEW CARD
    // =========================================================

    private void createReviewCard(
            JSONObject review,
            Map<Integer, String> houseMap,
            int position
    ) {

        int houseId =
                review.optInt(
                        "house_id",
                        0
                );

        int rating =
                review.optInt(
                        "rating",
                        0
                );

        if (rating < 0) {
            rating = 0;
        }

        if (rating > 5) {
            rating = 5;
        }

        String comment =
                review.optString(
                        "comment",
                        ""
                ).trim();

        String createdAt =
                review.optString(
                        "created_at",
                        ""
                ).trim();

        String houseName =
                houseMap.containsKey(
                        houseId
                )
                        ? houseMap.get(
                        houseId
                )
                        : "Boarding House";

        if (
                houseName == null
                        || houseName.trim().isEmpty()
        ) {

            houseName =
                    "Boarding House";
        }

        // =====================================================
        // CARD
        // =====================================================

        LinearLayout card =
                new LinearLayout(
                        this
                );

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        card.setBackgroundResource(
                R.drawable.bg_card_rounded
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin =
                dp(
                        position == 0
                                ? 12
                                : 12
                );

        reviewsContainer.addView(
                card,
                cardParams
        );

        // =====================================================
        // TOP ROW
        // =====================================================

        RelativeLayout topRow =
                new RelativeLayout(
                        this
                );

        card.addView(
                topRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // HOUSE NAME

        TextView houseText =
                createTextView(
                        houseName,
                        16,
                        Color.rgb(
                                26,
                                26,
                                26
                        )
                );

        houseText.setTypeface(
                null,
                Typeface.BOLD
        );

        houseText.setId(
                View.generateViewId()
        );

        RelativeLayout.LayoutParams houseParams =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        houseParams.addRule(
                RelativeLayout.ALIGN_PARENT_START
        );

        houseParams.addRule(
                RelativeLayout.CENTER_VERTICAL
        );

        topRow.addView(
                houseText,
                houseParams
        );

        // RATING GROUP

        LinearLayout ratingLayout =
                new LinearLayout(
                        this
                );

        ratingLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        ratingLayout.setGravity(
                Gravity.CENTER_VERTICAL
        );

        RelativeLayout.LayoutParams ratingLayoutParams =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        ratingLayoutParams.addRule(
                RelativeLayout.ALIGN_PARENT_END
        );

        ratingLayoutParams.addRule(
                RelativeLayout.CENTER_VERTICAL
        );

        ImageView star =
                new ImageView(
                        this
                );

        star.setImageResource(
                R.drawable.ic_star
        );

        star.setColorFilter(
                Color.rgb(
                        245,
                        166,
                        35
                )
        );

        ratingLayout.addView(
                star,
                new LinearLayout.LayoutParams(
                        dp(14),
                        dp(14)
                )
        );

        TextView ratingNumber =
                createTextView(
                        String.format(
                                Locale.US,
                                "%.1f",
                                (double) rating
                        ),
                        14,
                        Color.rgb(
                                26,
                                26,
                                26
                        )
                );

        ratingNumber.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams ratingNumberParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        ratingNumberParams.leftMargin =
                dp(4);

        ratingLayout.addView(
                ratingNumber,
                ratingNumberParams
        );

        topRow.addView(
                ratingLayout,
                ratingLayoutParams
        );

        // =====================================================
        // STAR TEXT
        // =====================================================

        TextView starsText =
                createTextView(
                        createStars(
                                rating
                        ),
                        18,
                        Color.rgb(
                                245,
                                166,
                                35
                        )
                );

        LinearLayout.LayoutParams starsParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        starsParams.topMargin =
                dp(8);

        card.addView(
                starsText,
                starsParams
        );

        // =====================================================
        // REVIEW DATE
        // =====================================================

        if (!createdAt.isEmpty()) {

            TextView dateText =
                    createTextView(
                            "Reviewed on "
                                    + formatReviewDate(
                                    createdAt
                            ),
                            12,
                            Color.rgb(
                                    154,
                                    154,
                                    158
                            )
                    );

            LinearLayout.LayoutParams dateParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            dateParams.topMargin =
                    dp(2);

            card.addView(
                    dateText,
                    dateParams
            );
        }

        // =====================================================
        // DIVIDER
        // =====================================================

        View divider =
                new View(
                        this
                );

        divider.setBackgroundColor(
                Color.rgb(
                        239,
                        239,
                        239
                )
        );

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        dividerParams.topMargin =
                dp(12);

        dividerParams.bottomMargin =
                dp(12);

        card.addView(
                divider,
                dividerParams
        );

        // =====================================================
        // COMMENT
        // =====================================================

        if (!comment.isEmpty()) {

            TextView commentText =
                    createTextView(
                            comment,
                            14,
                            Color.rgb(
                                    26,
                                    26,
                                    26
                            )
                    );

            commentText.setLineSpacing(
                    dp(4),
                    1.0f
            );

            card.addView(
                    commentText
            );
        }
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private void showEmptyState(
            String message
    ) {

        runOnUiThread(() -> {

            if (reviewsContainer == null) {
                return;
            }

            reviewsContainer.removeAllViews();

            if (reviewsCount != null) {

                reviewsCount.setText(
                        "0 reviews"
                );
            }

            if (averageRatingText != null) {

                averageRatingText.setText(
                        "0.0"
                );
            }

            if (reviewStatsText != null) {

                reviewStatsText.setText(
                        "No reviews posted yet"
                );
            }

            TextView emptyText =
                    createTextView(
                            message,
                            14,
                            Color.rgb(
                                    110,
                                    110,
                                    115
                            )
                    );

            emptyText.setGravity(
                    Gravity.CENTER
            );

            emptyText.setPadding(
                    dp(12),
                    dp(32),
                    dp(12),
                    dp(32)
            );

            reviewsContainer.addView(
                    emptyText
            );
        });
    }

    // =========================================================
    // CREATE STARS
    // =========================================================

    private String createStars(
            int rating
    ) {

        StringBuilder stars =
                new StringBuilder();

        for (
                int i = 1;
                i <= 5;
                i++
        ) {

            if (i <= rating) {

                stars.append(
                        "★"
                );

            } else {

                stars.append(
                        "☆"
                );
            }
        }

        return stars.toString();
    }

    // =========================================================
    // DATE DISPLAY
    // =========================================================

    private String formatReviewDate(
            String value
    ) {

        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            return "";
        }

        String date =
                value.trim();

        if (date.length() >= 10) {

            return date.substring(
                    0,
                    10
            );
        }

        return date;
    }

    // =========================================================
    // CREATE TEXT VIEW
    // =========================================================

    private TextView createTextView(
            String text,
            int size,
            int color
    ) {

        TextView textView =
                new TextView(
                        this
                );

        textView.setText(
                text
        );

        textView.setTextSize(
                size
        );

        textView.setTextColor(
                color
        );

        return textView;
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
        );
    }
}