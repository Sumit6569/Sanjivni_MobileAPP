package com.example.blogapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blogapp.databinding.ActivityForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener(v -> resetPassword());
        binding.backToLoginText.setOnClickListener(v -> finish());
    }

    private void resetPassword() {
        String email = binding.emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailInput.setError("Email is required");
            return;
        }

        // Show loading state
        binding.resetPasswordButton.setEnabled(false);
        binding.resetPasswordButton.setText("Sending...");

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password reset link sent to your email",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        binding.resetPasswordButton.setEnabled(true);
                        binding.resetPasswordButton.setText("Send Reset Link");
                    }
                });
    }
}
