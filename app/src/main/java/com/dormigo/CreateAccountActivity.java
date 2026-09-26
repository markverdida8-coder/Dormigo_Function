package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CreateAccountActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean isStudent = true;

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.create_account);

        // Initialize API client
        apiClient = new ApiClient();

        // Adjust for system bars
        View root = findViewById(android.R.id.content);

        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {

                Insets systemBars =
                        insets.getInsets(
                                WindowInsetsCompat.Type.systemBars() |
                                WindowInsetsCompat.Type.ime()
                        );

                v.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );

                return insets;
            });
        }

        // Initialize UI components
        LinearLayout btnBack =
                findViewById(R.id.btnBack);

        LinearLayout roleStudent =
                findViewById(R.id.roleStudent);

        LinearLayout roleLandlord =
                findViewById(R.id.roleLandlord);

        ImageView togglePasswordVisibility =
                findViewById(R.id.togglePasswordVisibility);

        ImageView toggleConfirmPasswordVisibility =
                findViewById(R.id.toggleConfirmPasswordVisibility);

        EditText passwordInput =
                findViewById(R.id.passwordInput);

        EditText confirmPasswordInput =
                findViewById(R.id.confirmPasswordInput);

        TextView btnCreateAccount =
                findViewById(R.id.btnCreateAccount);

        AutoCompleteTextView schoolCampusInput =
                findViewById(R.id.schoolCampusInput);

        TextView signInLink =
                findViewById(R.id.signInLink);

        EditText firstNameInput =
                findViewById(R.id.firstNameInput);

        EditText lastNameInput =
                findViewById(R.id.lastNameInput);

        EditText schoolEmailInput =
                findViewById(R.id.schoolEmailInput);

        EditText contactNumberInput =
                findViewById(R.id.contactNumberInput);

        MaterialCheckBox termsCheckbox =
                findViewById(R.id.termsCheckbox);

        // Back Button Logic
        btnBack.setOnClickListener(v -> {
            finish();
            overrideActivitySlideBack();
        });

        // Sign In Link Logic
        signInLink.setOnClickListener(v -> {
            finish();
            overrideActivitySlideBack();
        });

        // Dropdown Logic
        String[] schoolOptions =
                getResources().getStringArray(
                        R.array.school_campus_options
                );

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        schoolOptions
                );

        schoolCampusInput.setAdapter(adapter);

        // Role Selection Logic
        roleStudent.setOnClickListener(v -> {
            isStudent = true;
            updateRoleUI(roleStudent, roleLandlord);
        });

        roleLandlord.setOnClickListener(v -> {
            isStudent = false;
            updateRoleUI(roleStudent, roleLandlord);

            Intent intent =
                    new Intent(
                            this,
                            LandlordRegistrationActivity.class
                    );

            startActivity(intent);
            finish();
            overrideActivityFade();
        });

        // Password Visibility Logic
        togglePasswordVisibility.setOnClickListener(v -> {

            isPasswordVisible = !isPasswordVisible;

            togglePassword(
                    passwordInput,
                    togglePasswordVisibility,
                    isPasswordVisible
            );
        });

        // Confirm Password Visibility Logic
        toggleConfirmPasswordVisibility.setOnClickListener(v -> {

            isConfirmPasswordVisible =
                    !isConfirmPasswordVisible;

            togglePassword(
                    confirmPasswordInput,
                    toggleConfirmPasswordVisibility,
                    isConfirmPasswordVisible
            );
        });

        // Create Account Logic
        btnCreateAccount.setOnClickListener(v -> {

            String firstName =
                    firstNameInput
                            .getText()
                            .toString()
                            .trim();

            String lastName =
                    lastNameInput
                            .getText()
                            .toString()
                            .trim();

            String email =
                    schoolEmailInput
                            .getText()
                            .toString()
                            .trim();

            String school =
                    schoolCampusInput
                            .getText()
                            .toString()
                            .trim();

            String contact =
                    contactNumberInput
                            .getText()
                            .toString()
                            .trim();

            String pass =
                    passwordInput
                            .getText()
                            .toString();

            String confirmPass =
                    confirmPasswordInput
                            .getText()
                            .toString();

            // Required fields
            if (firstName.isEmpty()
                    || lastName.isEmpty()
                    || email.isEmpty()
                    || school.isEmpty()
                    || contact.isEmpty()
                    || pass.isEmpty()
                    || confirmPass.isEmpty()) {

                Toast.makeText(
                        CreateAccountActivity.this,
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Check password confirmation
            if (!Objects.equals(pass, confirmPass)) {

                Toast.makeText(
                        CreateAccountActivity.this,
                        "Passwords do not match!",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Check terms
            if (!termsCheckbox.isChecked()) {

                Toast.makeText(
                        CreateAccountActivity.this,
                        "Please agree to the terms",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Combine first and last name
            String fullName =
                    firstName + " " + lastName;

            // Database role values
            String userType =
                    isStudent
                            ? "STUDENT"
                            : "LANDLORD";

            // Disable button during request
            btnCreateAccount.setEnabled(false);

            // Send registration request
            apiClient.register(
                    fullName,
                    email,
                    pass,
                    contact,
                    userType,
                    new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            // Log exact error
                            android.util.Log.e(
                                    "DORMIGO_REGISTER",
                                    "Connection failed",
                                    e
                            );

                            runOnUiThread(() -> {

                                btnCreateAccount.setEnabled(true);

                                Toast.makeText(
                                        CreateAccountActivity.this,
                                        "Connection error: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

                            String responseBody = "";

                            if (response.body() != null) {
                                responseBody =
                                        response.body().string();
                            }

                            String finalResponseBody =
                                    responseBody;

                            int responseCode =
                                    response.code();

                            runOnUiThread(() -> {

                                btnCreateAccount.setEnabled(true);

                                try {

                                    JSONObject json =
                                            new JSONObject(
                                                    finalResponseBody
                                            );

                                    boolean success =
                                            json.optBoolean(
                                                    "success",
                                                    false
                                            );

                                    // Registration failed
                                    if (!success) {

                                        String message =
                                                json.optString(
                                                        "message",
                                                        "Registration failed."
                                                );

                                        Toast.makeText(
                                                CreateAccountActivity.this,
                                                message,
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    // Registration successful
                                    JSONObject user =
                                            json.getJSONObject(
                                                    "user"
                                            );

                                    int userId =
                                            user.getInt(
                                                    "user_id"
                                            );

                                    String returnedFullName =
                                            user.getString(
                                                    "full_name"
                                            );

                                    String returnedUserType =
                                            user.getString(
                                                    "user_type"
                                            );

                                    boolean registeredAsStudent =
                                            returnedUserType
                                                    .equalsIgnoreCase(
                                                            "STUDENT"
                                                    );

                                    // Save login state
                                    SharedPreferences.Editor editor =
                                            getSharedPreferences(
                                                    "DormigoPrefs",
                                                    MODE_PRIVATE
                                            ).edit();

                                    editor.putBoolean(
                                            "isLoggedIn",
                                            true
                                    );

                                    editor.putBoolean(
                                            "isStudent",
                                            registeredAsStudent
                                    );

                                    editor.putInt(
                                            "userId",
                                            userId
                                    );

                                    editor.putString(
                                            "fullName",
                                            returnedFullName
                                    );

                                    editor.apply();

                                    Toast.makeText(
                                            CreateAccountActivity.this,
                                            "Account created successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // Existing navigation
                                    Intent intent =
                                            new Intent(
                                                    CreateAccountActivity.this,
                                                    registeredAsStudent
                                                            ? HomeActivity.class
                                                            : LandlordHomeActivity.class
                                            );

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);

                                } catch (Exception e) {

                                    android.util.Log.e(
                                            "DORMIGO_REGISTER",
                                            "Invalid server response. HTTP "
                                                    + responseCode
                                                    + ": "
                                                    + finalResponseBody,
                                            e
                                    );

                                    Toast.makeText(
                                            CreateAccountActivity.this,
                                            "Invalid server response.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        }
                    }
            );
        });
    }

    @SuppressWarnings("deprecation")
    private void overrideActivitySlideBack() {
        overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
    }

    @SuppressWarnings("deprecation")
    private void overrideActivityFade() {
        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    private void togglePassword(
            EditText editText,
            ImageView imageView,
            boolean visible
    ) {

        if (visible) {

            editText.setInputType(
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            imageView.setImageResource(
                    R.drawable.ic_eye_off
            );

        } else {

            editText.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            imageView.setImageResource(
                    R.drawable.ic_eye
            );
        }

        editText.setSelection(
                editText.getText().length()
        );
    }

    private void updateRoleUI(
            LinearLayout studentLayout,
            LinearLayout landlordLayout
    ) {

        if (isStudent) {

            studentLayout.setBackgroundResource(
                    R.drawable.bg_role_selected
            );

            landlordLayout.setBackgroundResource(
                    R.drawable.bg_role_unselected
            );

        } else {

            studentLayout.setBackgroundResource(
                    R.drawable.bg_role_unselected
            );

            landlordLayout.setBackgroundResource(
                    R.drawable.bg_role_selected
            );
        }
    }
}