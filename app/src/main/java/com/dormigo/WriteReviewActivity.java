package com.dormigo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WriteReviewActivity extends AppCompatActivity {

    private final ApiClient apiClient =
            new ApiClient();

    private int currentRating = 5;

    private int houseId = 0;

    private EditText inputHouseName;
    private EditText inputReviewComment;

    private View btnSubmitReview;

    private ImageView[] stars;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_write_review
        );

        setupWindowInsets();

        bindViews();

        readIntentData();

        setupBackButton();

        setupStarRating();

        setupSubmitButton();
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
                                            .systemBars() | WindowInsetsCompat.Type.ime()
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

    // =========================================================
    // BIND VIEWS
    // =========================================================

    private void bindViews() {

        inputHouseName =
                findViewById(
                        R.id.inputHouseName
                );

        inputReviewComment =
                findViewById(
                        R.id.inputReviewComment
                );

        btnSubmitReview =
                findViewById(
                        R.id.btnSubmitReview
                );

        stars =
                new ImageView[]{
                        findViewById(
                                R.id.star1
                        ),
                        findViewById(
                                R.id.star2
                        ),
                        findViewById(
                                R.id.star3
                        ),
                        findViewById(
                                R.id.star4
                        ),
                        findViewById(
                                R.id.star5
                        )
                };
    }

    // =========================================================
    // INTENT DATA
    // =========================================================

    private void readIntentData() {

        houseId =
                getIntent().getIntExtra(
                        "HOUSE_ID",
                        0
                );

        String houseName =
                getIntent().getStringExtra(
                        "HOUSE_NAME"
                );

        if (
                inputHouseName != null
                        && houseName != null
                        && !houseName.trim().isEmpty()
        ) {

            inputHouseName.setText(
                    houseName.trim()
            );

            if (houseId > 0) {

                inputHouseName.setEnabled(
                        false
                );
            }
        }
    }

    // =========================================================
    // BACK BUTTON
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
    // STAR RATING
    // =========================================================

    private void setupStarRating() {

        for (
                int i = 0;
                i < stars.length;
                i++
        ) {

            final int rating =
                    i + 1;

            ImageView star =
                    stars[i];

            if (star != null) {

                star.setOnClickListener(v -> {

                    currentRating =
                            rating;

                    updateStars();
                });
            }
        }

        updateStars();
    }

    private void updateStars() {

        for (
                int i = 0;
                i < stars.length;
                i++
        ) {

            if (stars[i] == null) {
                continue;
            }

            if (i < currentRating) {

                stars[i].setColorFilter(
                        0xFFF5A623
                );

            } else {

                stars[i].setColorFilter(
                        0xFF9A9A9E
                );
            }
        }
    }

    // =========================================================
    // SUBMIT BUTTON
    // =========================================================

    private void setupSubmitButton() {

        if (btnSubmitReview == null) {
            return;
        }

        btnSubmitReview.setOnClickListener(v ->
                submitReview()
        );
    }

    // =========================================================
    // SUBMIT REVIEW
    // =========================================================

    private void submitReview() {

        String houseName =
                inputHouseName != null
                        ? inputHouseName
                        .getText()
                        .toString()
                        .trim()
                        : "";

        String comment =
                inputReviewComment != null
                        ? inputReviewComment
                        .getText()
                        .toString()
                        .trim()
                        : "";

        if (houseName.isEmpty()) {

            showToast(
                    "Please enter a boarding house."
            );

            return;
        }

        if (comment.isEmpty()) {

            showToast(
                    "Please write your review."
            );

            return;
        }

        if (
                currentRating < 1
                        || currentRating > 5
        ) {

            showToast(
                    "Please select a rating from 1 to 5."
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
                    "Please log in before submitting a review."
            );

            return;
        }

        setSubmitEnabled(
                false
        );

        if (houseId > 0) {

            sendReview(
                    userId,
                    houseId,
                    comment
            );

        } else {

            findHouseAndSubmit(
                    userId,
                    houseName,
                    comment
            );
        }
    }

    // =========================================================
    // FIND HOUSE ID FROM HOUSE NAME
    // =========================================================

    private void findHouseAndSubmit(
            int userId,
            String enteredHouseName,
            String comment
    ) {

        apiClient.getBoardingHouses(
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            setSubmitEnabled(
                                    true
                            );

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

                                    setSubmitEnabled(
                                            true
                                    );

                                    showToast(
                                            "Unable to find boarding house."
                                    );

                                    return;
                                }

                                JSONArray data =
                                        json.optJSONArray(
                                                "data"
                                        );

                                if (data == null) {

                                    setSubmitEnabled(
                                            true
                                    );

                                    showToast(
                                            "Boarding house not found."
                                    );

                                    return;
                                }

                                int matchedHouseId =
                                        0;

                                String matchedHouseName =
                                        "";

                                for (
                                        int i = 0;
                                        i < data.length();
                                        i++
                                ) {

                                    JSONObject house =
                                            data.getJSONObject(
                                                    i
                                            );

                                    String databaseHouseName =
                                            house.optString(
                                                    "house_name",
                                                    ""
                                            ).trim();

                                    if (
                                            databaseHouseName.equalsIgnoreCase(
                                                    enteredHouseName
                                            )
                                    ) {

                                        matchedHouseId =
                                                house.optInt(
                                                        "house_id",
                                                        0
                                                );

                                        matchedHouseName =
                                                databaseHouseName;

                                        break;
                                    }
                                }

                                if (matchedHouseId <= 0) {

                                    setSubmitEnabled(
                                            true
                                    );

                                    showToast(
                                            "Boarding house not found. Please enter the exact house name."
                                    );

                                    return;
                                }

                                houseId =
                                        matchedHouseId;

                                if (
                                        inputHouseName != null
                                                && !matchedHouseName.isEmpty()
                                ) {

                                    inputHouseName.setText(
                                            matchedHouseName
                                    );
                                }

                                sendReview(
                                        userId,
                                        houseId,
                                        comment
                                );

                            } catch (Exception e) {

                                setSubmitEnabled(
                                        true
                                );

                                showToast(
                                        "Invalid boarding house response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // SEND REVIEW TO PHP / POSTGRESQL
    // =========================================================

    private void sendReview(
            int userId,
            int selectedHouseId,
            String comment
    ) {

        apiClient.createReview(
                userId,
                selectedHouseId,
                currentRating,
                comment,
                new Callback() {

                    @Override
                    public void onFailure(
                            @NonNull Call call,
                            @NonNull IOException e
                    ) {

                        runOnUiThread(() -> {

                            setSubmitEnabled(
                                    true
                            );

                            showToast(
                                    "Unable to submit review."
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
                                    response.body()
                                            .string();
                        }

                        String result =
                                body;

                        runOnUiThread(() -> {

                            setSubmitEnabled(
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
                                                    "Unable to submit review."
                                            )
                                    );

                                    return;
                                }

                                showToast(
                                        "Review submitted successfully! "
                                                + currentRating
                                                + " stars"
                                );

                                finish();

                            } catch (Exception e) {

                                showToast(
                                        "Invalid review response."
                                );
                            }
                        });
                    }
                }
        );
    }

    // =========================================================
    // SUBMIT STATE
    // =========================================================

    private void setSubmitEnabled(
            boolean enabled
    ) {

        if (btnSubmitReview == null) {
            return;
        }

        btnSubmitReview.setEnabled(
                enabled
        );

        btnSubmitReview.setAlpha(
                enabled
                        ? 1.0f
                        : 0.55f
        );
    }

    // =========================================================
    // TOAST
    // =========================================================

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}