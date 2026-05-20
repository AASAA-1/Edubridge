package com.example.edubridge.shared;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.edubridge.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnSendResetLink;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        View root = findViewById(android.R.id.content);
        TextSizeHelper.applyScaleRecursively(root);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        tilEmail = findViewById(R.id.tilEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnSendResetLink.setOnClickListener(v -> handleSendResetLink());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handleSendResetLink() {
        String email = etEmail.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            tilEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Clear any previous errors
        tilEmail.setError(null);

        // Show progress
        showLoading(true);

        // Send password reset email
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Show success message
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password reset email sent! Please check your inbox.",
                                Toast.LENGTH_LONG).show();

                        // Clear the email field
                        etEmail.setText("");

                        // Optional: Go back to login after a delay
                        etEmail.postDelayed(() -> finish(), 2000);
                    } else {
                        // Show error message
                        String errorMessage = "Failed to send reset email.";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        // Handle specific Firebase errors
                        if (errorMessage != null && errorMessage.contains("no user record")) {
                            tilEmail.setError("No account found with this email");
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendResetLink.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        tvBackToLogin.setEnabled(!isLoading);
    }
}