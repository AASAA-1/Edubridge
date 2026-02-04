package com.example.edubridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Forces signout everytime app is restarted, for testing purposes
        FirebaseAuth.getInstance().signOut();

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    /**
     * Set up click listeners for buttons and links
     */
    private void setupClickListeners() {
        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // Forgot password click listener (currently does nothing)
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement forgot password functionality in future
                Toast.makeText(LoginActivity.this,
                        "Forgot Password feature coming soon!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle login button click with validation
     */
    private void handleLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (email.isEmpty()) {
            etUsername.setError("Email is required");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Perform Firebase authentication
        performFirebaseLogin(email, password);
    }

    /**
     * Authenticate with Firebase
     * @param email User's email
     * @param password User's password
     */
    private void performFirebaseLogin(String email, String password) {
        // Show loading toast
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        // Disable login button to prevent multiple clicks
        btnLogin.setEnabled(false);

        // Sign in with Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Re-enable login button
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Login successful
                        Toast.makeText(LoginActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate to MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Close LoginActivity
                    } else {
                        // Login failed
                        String errorMessage = "Authentication failed.";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Check if user is already logged in on activity start
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        if (auth.getCurrentUser() != null) {
            // User is already logged in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    /**
     * Prevent back button on login screen (optional)
     */
    @Override
    public void onBackPressed() {
        // Optional: Show exit confirmation dialog
        super.onBackPressed();
    }
}