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

public class LandlordRegistrationActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private Uri uploadedFileUri;
    private TextView uploadText;

    private ApiClient apiClient;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            uploadedFileUri =
                                    result.getData().getData();

                            if (uploadedFileUri != null) {

                                String fileName =
                                        getFileName(uploadedFileUri);

                                uploadText.setText(fileName);

                                Toast.makeText(
                                        this,
                                        "File selected: " + fileName,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.landlord_login_page);

        // Initialize API client
        apiClient = new ApiClient();

        // Adjust for system bars
        View root = findViewById(android.R.id.content);

        if (root != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    root,
                    (v, insets) -> {

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
                    }
            );
        }

        // Initialize UI components
        LinearLayout btnBack =
                findViewById(R.id.btnBack);

        LinearLayout roleStudent =
                findViewById(R.id.roleStudent);

        LinearLayout uploadDropzone =
                findViewById(R.id.uploadDropzone);

        uploadText =
                findViewById(R.id.uploadText);

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

        TextView signInLink =
                findViewById(R.id.signInLink);

        EditText fullNameInput =
                findViewById(R.id.fullNameInput);

        EditText emailInput =
                findViewById(R.id.emailInput);

        EditText mobileNumberInput =
                findViewById(R.id.mobileNumberInput);

        MaterialCheckBox termsCheckbox =
                findViewById(R.id.termsCheckbox);

        TextView termsText = findViewById(R.id.termsText);
        if (termsText != null && termsCheckbox != null) {
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
        if (btnBack != null) {

            btnBack.setOnClickListener(v -> {

                finish();

                overrideActivityFade();
            });
        }

        // Switch to Student Role
        if (roleStudent != null) {

            roleStudent.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                this,
                                CreateAccountActivity.class
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                startActivity(intent);

                finish();

                overrideActivityFade();
            });
        }

        // Password Visibility Logic
        if (togglePasswordVisibility != null) {

            togglePasswordVisibility.setOnClickListener(v -> {

                isPasswordVisible =
                        !isPasswordVisible;

                togglePassword(
                        passwordInput,
                        togglePasswordVisibility,
                        isPasswordVisible
                );
            });
        }

        // Confirm Password Visibility Logic
        if (toggleConfirmPasswordVisibility != null) {

            toggleConfirmPasswordVisibility.setOnClickListener(v -> {

                isConfirmPasswordVisible =
                        !isConfirmPasswordVisible;

                togglePassword(
                        confirmPasswordInput,
                        toggleConfirmPasswordVisibility,
                        isConfirmPasswordVisible
                );
            });
        }

        // Upload Document Logic
        if (uploadDropzone != null) {

            uploadDropzone.setOnClickListener(v -> {

                Intent intent =
                        new Intent(Intent.ACTION_GET_CONTENT);

                intent.setType("*/*");

                String[] mimeTypes = {
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain"
                };

                intent.putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        mimeTypes
                );

                intent.addCategory(
                        Intent.CATEGORY_OPENABLE
                );

                filePickerLauncher.launch(
                        Intent.createChooser(
                                intent,
                                "Select Document"
                        )
                );
            });
        }

        // Create Account Logic
        if (btnCreateAccount != null) {

            btnCreateAccount.setOnClickListener(v -> {

                String fullName =
                        fullNameInput
                                .getText()
                                .toString()
                                .trim();

                String email =
                        emailInput
                                .getText()
                                .toString()
                                .trim();

                String mobile =
                        mobileNumberInput
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
                if (fullName.isEmpty()
                        || email.isEmpty()
                        || mobile.isEmpty()
                        || pass.isEmpty()
                        || confirmPass.isEmpty()) {

                    Toast.makeText(
                            LandlordRegistrationActivity.this,
                            "Please fill in all fields",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Password confirmation
                if (!Objects.equals(pass, confirmPass)) {

                    Toast.makeText(
                            LandlordRegistrationActivity.this,
                            "Passwords do not match!",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Document requirement
                if (uploadedFileUri == null) {

                    Toast.makeText(
                            LandlordRegistrationActivity.this,
                            "Please upload the required documents",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Terms
                if (!termsCheckbox.isChecked()) {
                    Toast.makeText(
                            LandlordRegistrationActivity.this,
                            "You must agree to the Terms & Conditions before creating an account.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Disable button during request
                btnCreateAccount.setEnabled(false);

                // Send landlord registration to API
                apiClient.registerLandlordWithDocument(
                        LandlordRegistrationActivity.this,
                        fullName,
                        email,
                        pass,
                        mobile,
                        uploadedFileUri,
                        new Callback() {

                            @Override
                            public void onFailure(
                                    Call call,
                                    IOException e
                            ) {

                                Log.e(
                                        "DORMIGO_LANDLORD_REGISTER",
                                        "Connection failed",
                                        e
                                );

                                runOnUiThread(() -> {

                                    btnCreateAccount
                                            .setEnabled(true);

                                    Toast.makeText(
                                            LandlordRegistrationActivity.this,
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

                                runOnUiThread(() -> {

                                    btnCreateAccount
                                            .setEnabled(true);

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
                                                    LandlordRegistrationActivity.this,
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

                                        String returnedName =
                                                user.getString(
                                                        "full_name"
                                                );

                                        String returnedUserType =
                                                user.getString(
                                                        "user_type"
                                                );

                                        // Make sure it really
                                        // registered as landlord
                                        if (!returnedUserType
                                                .equalsIgnoreCase(
                                                        "LANDLORD"
                                                )) {

                                            Toast.makeText(
                                                    LandlordRegistrationActivity.this,
                                                    "Invalid account type returned by server.",
                                                    Toast.LENGTH_LONG
                                            ).show();

                                            return;
                                        }

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
                                                false
                                        );

                                        editor.putInt(
                                                "userId",
                                                userId
                                        );

                                        editor.putString(
                                                "fullName",
                                                returnedName
                                        );

                                        editor.apply();

                                        Toast.makeText(
                                                LandlordRegistrationActivity.this,
                                                "Landlord account created successfully!",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        // Go to landlord home
                                        Intent intent =
                                                new Intent(
                                                        LandlordRegistrationActivity.this,
                                                        LandlordHomeActivity.class
                                                );

                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        );

                                        startActivity(intent);

                                    } catch (Exception e) {

                                        Log.e(
                                                "DORMIGO_LANDLORD_REGISTER",
                                                "Invalid server response: "
                                                        + finalResponseBody,
                                                e
                                        );

                                        Toast.makeText(
                                                LandlordRegistrationActivity.this,
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

        // Sign In Link
        if (signInLink != null) {

            signInLink.setOnClickListener(v -> {

                finish();

                overrideActivitySlideBack();
            });
        }
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
    private void overrideActivityFade() {

        overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
        );
    }

    @SuppressWarnings("deprecation")
    private void overrideActivitySlideBack() {

        overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
    }

    private void togglePassword(
            EditText editText,
            ImageView imageView,
            boolean visible
    ) {

        if (editText == null || imageView == null) {
            return;
        }

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

    private String getFileName(Uri uri) {

        if (uri == null) {
            return "Unknown file";
        }

        String name = null;

        String scheme = uri.getScheme();

        if ("content".equals(scheme)) {

            try (
                    Cursor cursor =
                            getContentResolver().query(
                                    uri,
                                    null,
                                    null,
                                    null,
                                    null
                            )
            ) {

                if (cursor != null
                        && cursor.moveToFirst()) {

                    int nameIndex =
                            cursor.getColumnIndex(
                                    OpenableColumns.DISPLAY_NAME
                            );

                    if (nameIndex != -1) {

                        name =
                                cursor.getString(
                                        nameIndex
                                );
                    }
                }

            } catch (Exception ignored) {
            }
        }

        if (name == null
                && uri.getPath() != null) {

            String path =
                    uri.getPath();

            int cut =
                    path.lastIndexOf('/');

            if (cut != -1) {

                name =
                        path.substring(cut + 1);

            } else {

                name = path;
            }
        }

        return Objects.requireNonNullElse(
                name,
                "Unknown file"
        );
    }
}