package com.dormigo;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StudentMaintenanceRequestsActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private static final String[] CATEGORIES = {
            "PLUMBING",
            "ELECTRICAL",
            "FURNITURE",
            "APPLIANCE",
            "STRUCTURAL",
            "CLEANLINESS",
            "OTHER"
    };

    private final ApiClient apiClient = new ApiClient();
    private final List<BookingOption> activeBookings = new ArrayList<>();

    private Spinner bookingSpinner;
    private Spinner categorySpinner;
    private EditText descriptionInput;
    private Button submitButton;
    private TextView bookingDetails;
    private TextView eligibilityMessage;
    private TextView requestsMessage;
    private LinearLayout requestsContainer;

    private boolean isSubmitting;
    private boolean isUpdatingBookingSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_maintenance_requests);

        bookingSpinner = findViewById(R.id.maintenanceBookingSpinner);
        categorySpinner = findViewById(R.id.maintenanceCategorySpinner);
        descriptionInput = findViewById(R.id.maintenanceDescriptionInput);
        submitButton = findViewById(R.id.maintenanceSubmitButton);
        bookingDetails = findViewById(R.id.maintenanceBookingDetails);
        eligibilityMessage = findViewById(R.id.maintenanceEligibilityMessage);
        requestsMessage = findViewById(R.id.maintenanceRequestsMessage);
        requestsContainer = findViewById(R.id.maintenanceRequestsContainer);

        findViewById(R.id.maintenanceBackButton).setOnClickListener(v -> finish());
        setupCategorySpinner();
        setupBookingSpinner();
        submitButton.setOnClickListener(v -> confirmSubmission());

        setFormEnabled(false);
        eligibilityMessage.setText("Loading active bookings...");
        loadActiveBookings();
        loadMaintenanceRequests();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CATEGORIES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupBookingSpinner() {
        bookingSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    android.widget.AdapterView<?> parent,
                    View view,
                    int position,
                    long id
            ) {
                if (!isUpdatingBookingSelection) {
                    updateSelectedBooking();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                updateSelectedBooking();
            }
        });
    }

    private void loadActiveBookings() {
        int studentId = getSharedPreferences("DormigoPrefs", MODE_PRIVATE)
                .getInt("userId", -1);
        if (studentId <= 0) {
            showBookingLoadError("Please log in to view active bookings.");
            return;
        }
        apiClient.getBookingsForStudent(studentId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showBookingLoadError("Unable to load active bookings. Check your connection and try again."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body() != null ? response.body().string() : "";
                boolean successful = response.isSuccessful();
                response.close();

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseText);
                        if (!successful || !json.optBoolean("success", false)) {
                            showBookingLoadError(responseMessage(json, "Unable to load active bookings."));
                            return;
                        }
                        JSONArray data = json.optJSONArray("data");
                        if (data == null) {
                            showBookingLoadError("The server returned an unexpected booking response.");
                            return;
                        }

                        activeBookings.clear();

                        if (studentId > 0) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject booking = data.optJSONObject(i);
                                if (booking == null
                                        || booking.optInt("user_id", 0) != studentId
                                        || !"ACTIVE".equalsIgnoreCase(
                                                booking.optString("status", "").trim()
                                        )) {
                                    continue;
                                }
                                activeBookings.add(new BookingOption(booking));
                            }
                        }

                        populateBookingChoices();
                    } catch (Exception e) {
                        showBookingLoadError("Unable to read active bookings.");
                    }
                });
            }
        });
    }

    private void populateBookingChoices() {
        if (activeBookings.isEmpty()) {
            bookingSpinner.setAdapter(null);
            bookingDetails.setText("");
            eligibilityMessage.setText(
                    "No eligible active booking was found. A maintenance request requires an active booking."
            );
            eligibilityMessage.setVisibility(View.VISIBLE);
            setFormEnabled(false);
            return;
        }

        ArrayAdapter<BookingOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                activeBookings
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        isUpdatingBookingSelection = true;
        bookingSpinner.setAdapter(adapter);

        int requestedBookingId = getIntent().getIntExtra(EXTRA_BOOKING_ID, 0);
        for (int i = 0; i < activeBookings.size(); i++) {
            if (activeBookings.get(i).bookingId == requestedBookingId) {
                bookingSpinner.setSelection(i);
                break;
            }
        }
        isUpdatingBookingSelection = false;

        bookingSpinner.setEnabled(activeBookings.size() > 1);
        eligibilityMessage.setVisibility(View.GONE);
        updateSelectedBooking();
    }

    private void updateSelectedBooking() {
        int position = bookingSpinner.getSelectedItemPosition();
        if (position < 0 || position >= activeBookings.size()) {
            bookingDetails.setText("");
            setFormEnabled(false);
            return;
        }

        BookingOption booking = activeBookings.get(position);
        bookingDetails.setText(
                "Booking #" + booking.bookingId
                        + "\n" + booking.houseName + " · Room " + booking.roomNumber
                        + "\nMove-in: " + booking.moveInDate
                        + " · " + booking.durationMonths + " month(s)"
        );
        eligibilityMessage.setVisibility(View.GONE);
        setFormEnabled(!isSubmitting);
    }

    private void showBookingLoadError(String message) {
        eligibilityMessage.setText(message);
        eligibilityMessage.setVisibility(View.VISIBLE);
        setFormEnabled(false);
    }

    private void setFormEnabled(boolean enabled) {
        categorySpinner.setEnabled(enabled);
        descriptionInput.setEnabled(enabled);
        submitButton.setEnabled(enabled && !isSubmitting);
    }

    private void confirmSubmission() {
        if (isSubmitting || bookingSpinner.getSelectedItemPosition() < 0) {
            return;
        }

        String description = descriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            descriptionInput.setError("Description is required.");
            descriptionInput.requestFocus();
            return;
        }

        if (description.length() > 2000) {
            descriptionInput.setError("Description must be 2,000 characters or fewer.");
            descriptionInput.requestFocus();
            return;
        }

        BookingOption booking = activeBookings.get(bookingSpinner.getSelectedItemPosition());
        String category = categorySpinner.getSelectedItem().toString();
        new AlertDialog.Builder(this)
                .setTitle("Submit maintenance request?")
                .setMessage(
                        "Submit a " + category + " request for "
                                + booking.houseName + ", Room " + booking.roomNumber + "?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", (dialog, which) ->
                        submitRequest(booking.bookingId, category, description)
                )
                .show();
    }

    private void submitRequest(int bookingId, String category, String description) {
        if (isSubmitting) {
            return;
        }

        isSubmitting = true;
        setFormEnabled(false);
        submitButton.setText("Submitting...");

        apiClient.createMaintenanceRequest(bookingId, category, description, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    isSubmitting = false;
                    submitButton.setText("Submit Request");
                    setFormEnabled(bookingSpinner.getSelectedItemPosition() >= 0);
                    showFeedback("Unable to connect to the server. Your request was not submitted.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body() != null ? response.body().string() : "";
                int statusCode = response.code();
                response.close();

                runOnUiThread(() -> {
                    isSubmitting = false;
                    submitButton.setText("Submit Request");
                    try {
                        JSONObject json = new JSONObject(responseText);
                        if (statusCode == 201 && json.optBoolean("success", false)) {
                            categorySpinner.setSelection(0);
                            descriptionInput.setText("");
                            descriptionInput.setError(null);
                            setFormEnabled(bookingSpinner.getSelectedItemPosition() >= 0);
                            Toast.makeText(
                                    StudentMaintenanceRequestsActivity.this,
                                    json.optString("message", "Maintenance request submitted successfully."),
                                    Toast.LENGTH_LONG
                            ).show();
                            loadMaintenanceRequests();
                        } else {
                            setFormEnabled(bookingSpinner.getSelectedItemPosition() >= 0);
                            showFeedback(responseMessage(json, "Unable to submit the maintenance request."));
                        }
                    } catch (Exception e) {
                        setFormEnabled(bookingSpinner.getSelectedItemPosition() >= 0);
                        showFeedback("Unable to read the server response. Your request may not have been submitted.");
                    }
                });
            }
        });
    }

    private void loadMaintenanceRequests() {
        requestsMessage.setText("Loading maintenance requests...");
        requestsContainer.removeAllViews();

        apiClient.getMaintenanceRequests(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> requestsMessage.setText(
                        "Unable to load maintenance requests. Check your connection and try again."
                ));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body() != null ? response.body().string() : "";
                boolean successful = response.isSuccessful();
                response.close();

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseText);
                        if (!successful || !json.optBoolean("success", false)) {
                            requestsMessage.setText(responseMessage(json, "Unable to load maintenance requests."));
                            return;
                        }

                        JSONArray data = json.optJSONArray("data");
                        if (data == null) {
                            requestsMessage.setText("The server returned an unexpected maintenance request response.");
                            return;
                        }

                        requestsContainer.removeAllViews();
                        if (data.length() == 0) {
                            requestsMessage.setText("You have not submitted any maintenance requests.");
                            return;
                        }

                        requestsMessage.setText(data.length() + " maintenance request(s)");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject request = data.optJSONObject(i);
                            if (request != null) {
                                requestsContainer.addView(createRequestCard(request));
                            }
                        }
                    } catch (Exception e) {
                        requestsMessage.setText("Unable to read maintenance request history.");
                    }
                });
            }
        });
    }

    private View createRequestCard(JSONObject request) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card_rounded);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(10);
        card.setLayoutParams(cardParams);

        addCardText(card, request.optString("category", "Maintenance"), 15, true, "#1B5E4C");
        addCardText(card, request.optString("status", "PENDING"), 12, true, "#6E6E73");
        addCardText(card, request.optString("description", ""), 14, false, "#1A1A1A");
        addCardText(
                card,
                request.optString("house_name", "Boarding House")
                        + " · Room " + request.optString("room_number", "Room"),
                12,
                false,
                "#6E6E73"
        );
        addCardText(
                card,
                "Submitted: " + formatSubmittedDate(request.optString("submitted_at", "")),
                12,
                false,
                "#6E6E73"
        );

        String resolutionNotes = request.optString("resolution_notes", "").trim();
        if (!resolutionNotes.isEmpty() && !"null".equalsIgnoreCase(resolutionNotes)) {
            addCardText(card, "Resolution notes: " + resolutionNotes, 13, false, "#1A1A1A");
        }
        return card;
    }

    private void addCardText(
            LinearLayout card,
            String text,
            int size,
            boolean bold,
            String color
    ) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(android.graphics.Color.parseColor(color));
        if (bold) {
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        label.setPadding(0, dp(3), 0, dp(3));
        card.addView(label);
    }

    private String formatSubmittedDate(String value) {
        if (value.isEmpty()) {
            return "Date unavailable";
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.US);
                input.setLenient(false);
                Date date = input.parse(value);
                if (date != null) {
                    return new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                            .format(date);
                }
            } catch (ParseException ignored) {
                // Try the next known timestamp format.
            }
        }
        return value.replace('T', ' ');
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String responseMessage(JSONObject json, String fallback) {
        String message = json.optString("message", "").trim();
        return message.isEmpty() || "null".equalsIgnoreCase(message) ? fallback : message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class BookingOption {
        final int bookingId;
        final String houseName;
        final String roomNumber;
        final String moveInDate;
        final int durationMonths;

        BookingOption(JSONObject booking) {
            bookingId = booking.optInt("booking_id", 0);
            houseName = booking.optString("house_name", "Boarding House");
            roomNumber = booking.optString("room_number", "Room");
            moveInDate = booking.optString("move_in_date", "Not specified");
            durationMonths = booking.optInt("duration_months", 0);
        }

        @NonNull
        @Override
        public String toString() {
            return houseName + " · Room " + roomNumber + " · Booking #" + bookingId;
        }
    }
}
