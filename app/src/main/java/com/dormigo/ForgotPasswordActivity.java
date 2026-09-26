package com.dormigo;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.forgot_password_page);

        // Adjust for system bars
        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI components
        ImageView btnBack = findViewById(R.id.btnBack);
        EditText emailInput = findViewById(R.id.emailInput);
        TextView btnSendResetLink = findViewById(R.id.btnSendResetLink);
        TextView backToSignIn = findViewById(R.id.backToSignIn);

        // Back Button Logic
        btnBack.setOnClickListener(v -> finish());
        
        // Back to Sign In Link
        backToSignIn.setOnClickListener(v -> finish());

        // Send Reset Link Logic
        btnSendResetLink.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show();
            // Here you would normally call an API to send the reset link
        });
    }
}