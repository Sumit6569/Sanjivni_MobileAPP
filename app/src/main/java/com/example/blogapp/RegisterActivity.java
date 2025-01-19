package com.example.blogapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blogapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        binding.loginText.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void signInWithGoogle() {
        // Sign out first to always show account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account == null || account.getIdToken() == null) {
            Toast.makeText(this, "Google Sign In failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.googleSignInButton.setEnabled(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        // Update profile with Google account name if needed
                        if (account.getDisplayName() != null && !account.getDisplayName().isEmpty()) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(account.getDisplayName())
                                .build();

                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    Toast.makeText(RegisterActivity.this, 
                                        "Registration successful!", 
                                        Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                        } else {
                            // No display name to update, proceed to MainActivity
                            Toast.makeText(RegisterActivity.this, 
                                "Registration successful!", 
                                Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                } else {
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : 
                        "Google sign in failed";
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    binding.googleSignInButton.setEnabled(true);
                }
            });
    }

    private void registerUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String name = binding.nameInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailInput.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Please enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            binding.passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (TextUtils.isEmpty(name)) {
            binding.nameInput.setError("Name is required");
            return;
        }

        binding.registerButton.setEnabled(false);

        // Create new user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        // Update user profile with the provided name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    // Registration successful
                                    Toast.makeText(RegisterActivity.this, 
                                        "Registration successful!", 
                                        Toast.LENGTH_SHORT).show();
                                    
                                    // Go to MainActivity
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Failed to update profile
                                    String error = profileTask.getException() != null ? 
                                        profileTask.getException().getMessage() : 
                                        "Failed to update profile";
                                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
                                    binding.registerButton.setEnabled(true);
                                }
                            });
                    }
                } else {
                    // Registration failed
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : 
                        "Registration failed";
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    binding.registerButton.setEnabled(true);
                }
            });
    }
}
