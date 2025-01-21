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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blogapp.databinding.ActivityRegisterBinding;
import com.example.blogapp.models.BlogPost;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private DatabaseReference databaseReference;

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

        try {
            // Initialize Firebase Database with direct URL
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://sanjivni-34359-default-rtdb.firebaseio.com");
            
            // Enable offline persistence
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch (Exception e) {
                Log.w("BlogApp", "Persistence already enabled");
            }
            
            // Get reference to blogPosts node
            databaseReference = database.getReference("blogPosts");

            // Add connection state listener
            DatabaseReference connectedRef = database.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    if (connected) {
                        Log.d("BlogApp", "RegisterActivity: Connected to Firebase Database");
                    } else {
                        Log.e("BlogApp", "RegisterActivity: Not connected to Firebase Database");
                        Toast.makeText(RegisterActivity.this, "Checking database connection...", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("BlogApp", "RegisterActivity: Connection listener was cancelled: " + error.getMessage());
                }
            });

            Log.d("BlogApp", "RegisterActivity: Firebase Database initialized with reference: " + databaseReference.toString());
        } catch (Exception e) {
            Log.e("BlogApp", "Error initializing Firebase Database: " + e.getMessage());
            e.printStackTrace();
        }

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

    public void createBlogPost(String title, String content, String imageUrl, String locationName) {
        try {
            if (databaseReference == null) {
                Log.e("BlogApp", "Database reference is null!");
                Toast.makeText(this, "Error: Database not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e("BlogApp", "No user logged in!");
                Toast.makeText(this, "Error: Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = currentUser.getUid();

            // Generate a unique key for the post
            String postId = databaseReference.push().getKey();
            if (postId == null) {
                Log.e("BlogApp", "Could not generate postId!");
                Toast.makeText(this, "Error: Could not generate post ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create blog post object
            BlogPost blogPost = new BlogPost(title, content, imageUrl, locationName, userId);
            
            // Log the blog post data
            Log.d("BlogApp", "Creating blog post with data:" +
                    "\nPostId: " + postId +
                    "\nTitle: " + title +
                    "\nContent: " + content +
                    "\nImageUrl: " + imageUrl +
                    "\nLocation: " + locationName +
                    "\nUserId: " + userId +
                    "\nTimestamp: " + blogPost.getTimestamp());

            // Create a map of the blog post data
            Map<String, Object> postValues = new HashMap<>();
            postValues.put("title", title);
            postValues.put("content", content);
            postValues.put("imageUrl", imageUrl);
            postValues.put("locationName", locationName);
            postValues.put("userId", userId);
            postValues.put("timestamp", blogPost.getTimestamp());

            // Save to Firebase using updateChildren for atomic operation
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/blogPosts/" + postId, postValues);

            databaseReference.getRoot().updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("BlogApp", "Blog post saved successfully at path: /blogPosts/" + postId);
                    
                    // Verify the data was saved
                    databaseReference.child(postId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DataSnapshot snapshot = task.getResult();
                            BlogPost savedPost = snapshot.getValue(BlogPost.class);
                            if (savedPost != null) {
                                Log.d("BlogApp", "Verified saved post: " + savedPost.getTitle());
                                Toast.makeText(RegisterActivity.this, "Blog post created successfully!", Toast.LENGTH_SHORT).show();
                                
                                // Start MainActivity
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e("BlogApp", "Failed to verify saved post - post is null");
                                Toast.makeText(RegisterActivity.this, "Error: Failed to verify post creation", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("BlogApp", "Failed to verify saved post: " + task.getException().getMessage());
                            Toast.makeText(RegisterActivity.this, "Error: Failed to verify post creation", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("BlogApp", "Failed to save blog post: " + e.getMessage());
                    Toast.makeText(this, "Failed to create blog post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e("BlogApp", "Exception in createBlogPost: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error creating blog post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
