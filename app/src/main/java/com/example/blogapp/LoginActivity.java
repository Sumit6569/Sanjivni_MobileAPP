package com.example.blogapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blogapp.databinding.ActivityLoginBinding;
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

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign In result code: " + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Google Sign In successful, account: " + account.getEmail());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Google Sign In failed with result code: " + result.getResultCode());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("12720675257-bp00f0bf73lqid2qqvb4ta2va8bi04cd.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d(TAG, "Google Sign In configured");

        // Check if user is already signed in
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        binding.registerText.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        binding.forgotPasswordText.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign In flow");
        // Sign out first to always show account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Previous Google Sign In state cleared");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Log.e(TAG, "firebaseAuthWithGoogle: ID Token is null");
            Toast.makeText(this, "Google Sign In failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "firebaseAuthWithGoogle: starting Firebase auth");
        binding.googleSignInButton.setEnabled(false);

        // Get the Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getEmail() != null) {
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, 
                                "Login successful!", 
                                Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        binding.googleSignInButton.setEnabled(true);
                        String errorMessage = task.getException() != null ? 
                            task.getException().getMessage() : 
                            "Authentication failed";
                        Toast.makeText(LoginActivity.this, 
                            "Login failed: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            binding.googleSignInButton.setEnabled(true);
            Toast.makeText(this, "Failed to get Google account info", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

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

        binding.loginButton.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(LoginActivity.this, 
                            "Login successful!", 
                            Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    String errorMessage = task.getException() != null ? 
                        task.getException().getMessage() : 
                        "Authentication failed";
                    Toast.makeText(LoginActivity.this, 
                        "Login failed: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                    binding.loginButton.setEnabled(true);
                    binding.passwordInput.setText("");
                }
            });
    }
}
