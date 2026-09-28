package com.dormigo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private Uri uploadedFileUri;
    private TextView uploadText;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            uploadedFileUri = result.getData().getData();
                            if (uploadedFileUri != null && uploadText != null) {
                                String fileName = getFileName(uploadedFileUri);
                                uploadText.setText(fileName);
                                Toast.makeText(this, "File selected: " + fileName, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Selected File";
    }

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

        TextView termsText = findViewById(R.id.termsText);
        if (termsText != null) {
            SpannableString spannable = new SpannableString("I have read and agree to the Terms & Conditions and Privacy Policy.");
            
            ClickableSpan termsSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    showTermsDialog(termsCheckbox);
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(Color.parseColor("#1B5E4C"));
                }
            };
            
            ClickableSpan privacySpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    showPrivacyDialog(termsCheckbox);
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(Color.parseColor("#1B5E4C"));
                }
            };

            spannable.setSpan(termsSpan, 29, 47, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(privacySpan, 52, 66, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            termsText.setText(spannable);
            termsText.setMovementMethod(LinkMovementMethod.getInstance());
        }

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

        // Upload Dropzone Logic
        LinearLayout uploadDropzone = findViewById(R.id.uploadDropzone);
        uploadText = findViewById(R.id.uploadText);

        if (uploadDropzone != null) {
            uploadDropzone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                filePickerLauncher.launch(Intent.createChooser(intent, "Select Student ID Image"));
            });
        }

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

            // Check uploaded ID
            if (uploadedFileUri == null) {
                Toast.makeText(CreateAccountActivity.this, "Please upload your Student ID (Image).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check terms
            if (!termsCheckbox.isChecked()) {
                Toast.makeText(
                        CreateAccountActivity.this,
                        "You must agree to the Terms & Conditions before creating an account.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Combine first and last name
            String fullName =
                    firstName + " " + lastName;

            // Disable button during request
            btnCreateAccount.setEnabled(false);

            // Send registration request
            apiClient.registerStudentWithId(
                    this,
                    fullName,
                    email,
                    pass,
                    contact,
                    school,
                    uploadedFileUri,
                    new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {

                            // Log exact error
                            Log.e(
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

                                    Log.e(
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

    private void showTermsDialog(MaterialCheckBox checkbox) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_terms)
                .create();
        
        dialog.show();

        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView content = dialog.findViewById(R.id.dialogContent);
        if (title != null) title.setText("Dormigo Terms & Conditions");
        if (content != null) content.setText(
                "1. Welcome\n" +
                "Dormigo is a boarding house finder platform that connects students and landlords.\n" +
                "By creating an account you agree to these Terms.\n\n" +
                "2. User Accounts\n" +
                "Users must provide accurate information.\n" +
                "Duplicate accounts are prohibited.\n" +
                "Users are responsible for protecting their passwords.\n\n" +
                "3. Student Responsibilities\n" +
                "Students must:\n" +
                "• Submit truthful booking requests.\n" +
                "• Respect house rules.\n" +
                "• Upload legitimate payment proofs.\n" +
                "• Use respectful communication.\n\n" +
                "4. Landlord Responsibilities\n" +
                "Landlords must:\n" +
                "• Upload genuine boarding house information.\n" +
                "• Keep room availability updated.\n" +
                "• Verify payments honestly.\n" +
                "• Upload valid verification documents.\n\n" +
                "5. Booking Policy\n" +
                "Submitting a booking request does not guarantee approval.\n" +
                "Only landlords approve bookings.\n\n" +
                "6. Payment Policy\n" +
                "Students must only use payment methods provided inside Dormigo.\n" +
                "Dormigo does not directly process or hold payments.\n\n" +
                "7. Messaging\n" +
                "Users must not send:\n" +
                "• Spam\n" +
                "• Harassment\n" +
                "• Threats\n" +
                "• Fraudulent content\n" +
                "Dormigo may suspend violating accounts.\n\n" +
                "8. Reviews\n" +
                "Reviews must represent real experiences.\n" +
                "Fake reviews may be removed.\n\n" +
                "9. Verification\n" +
                "Students may upload Student IDs.\n" +
                "Landlords must upload:\n" +
                "Business Permit\n" +
                "Valid Government ID\n" +
                "Verification is reviewed by the Administrator.\n\n" +
                "10. Privacy\n" +
                "Dormigo stores:\n" +
                "Account information\n" +
                "Booking records\n" +
                "Payment records\n" +
                "Verification documents\n" +
                "Verification documents are only accessible by authorized Administrators.\n" +
                "Dormigo does not sell user information.\n\n" +
                "11. Account Suspension\n" +
                "Dormigo may suspend accounts involved in:\n" +
                "Fraud\n" +
                "Fake documents\n" +
                "False payment proofs\n" +
                "Harassment\n" +
                "Platform abuse\n\n" +
                "12. Limitation of Liability\n" +
                "Dormigo only connects students and landlords.\n" +
                "Dormigo is not responsible for rental disputes between users.\n\n" +
                "13. Contact\n" +
                "For concerns, contact the Dormigo Administrator."
        );

        View btnDecline = dialog.findViewById(R.id.btnDecline);
        View btnAgree = dialog.findViewById(R.id.btnAgree);

        if (btnDecline != null) btnDecline.setOnClickListener(v -> dialog.dismiss());
        if (btnAgree != null) {
            btnAgree.setOnClickListener(v -> {
                checkbox.setChecked(true);
                dialog.dismiss();
            });
        }
    }

    private void showPrivacyDialog(MaterialCheckBox checkbox) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_terms)
                .create();
        
        dialog.show();

        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView content = dialog.findViewById(R.id.dialogContent);
        if (title != null) title.setText("Privacy Policy");
        if (content != null) content.setText(
                "• Information Collected\n" +
                "We collect personal information such as your name, email, phone number, and school details when you register.\n\n" +
                "• How Information is Used\n" +
                "Your information is used to facilitate communication between students and landlords, process bookings, and manage your account.\n\n" +
                "• Verification Document Handling\n" +
                "Verification documents are securely uploaded and only accessed by authorized Dormigo Administrators for approval purposes.\n\n" +
                "• Payment Information\n" +
                "Dormigo only records payment transaction references and proof of payment receipts. We do not store credit card details.\n\n" +
                "• Security\n" +
                "We implement robust security measures to protect your personal data from unauthorized access or disclosure.\n\n" +
                "• User Rights\n" +
                "You have the right to request the deletion of your account and personal data at any time via your account settings.\n\n" +
                "• Data Retention\n" +
                "We retain your data only for as long as your account is active or as needed to provide you services and comply with legal obligations.\n\n" +
                "• Contact Information\n" +
                "If you have questions about this Privacy Policy, please contact the Dormigo Administrator."
        );

        View btnDecline = dialog.findViewById(R.id.btnDecline);
        View btnAgree = dialog.findViewById(R.id.btnAgree);

        if (btnDecline != null) btnDecline.setOnClickListener(v -> dialog.dismiss());
        if (btnAgree != null) {
            btnAgree.setOnClickListener(v -> {
                checkbox.setChecked(true);
                dialog.dismiss();
            });
        }
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