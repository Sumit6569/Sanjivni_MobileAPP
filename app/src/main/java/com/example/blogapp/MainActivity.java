package com.example.blogapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blogapp.adapters.BlogAdapter;
import com.example.blogapp.adapters.ImageCarouselAdapter;
import com.example.blogapp.adapters.PostAdapter;
import com.example.blogapp.databinding.ActivityMainBinding;
import com.example.blogapp.models.BlogPost;
import com.example.blogapp.models.Post;
import com.example.blogapp.utils.NotificationHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener, PostAdapter.OnPostClickListener {
                   
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private List<Post> postList;
    private PostAdapter postAdapter;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private BlogAdapter blogAdapter;
    private List<BlogPost> blogPosts;
    private DatabaseReference databaseReference;

    private final ActivityResultLauncher<Intent> createPostLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Report created successfully!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance();
            
            // Initialize Firebase Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            databaseReference = database.getReference("posts");
            
            // Initialize RecyclerView
            blogPosts = new ArrayList<>();
            blogAdapter = new BlogAdapter(blogPosts);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerView.setAdapter(blogAdapter);

            // Start listening for notifications
            NotificationHelper.listenForNotifications(this);

            // Setup Navigation Drawer
            setSupportActionBar(binding.toolbar);
            toggle = new ActionBarDrawerToggle(
                this, 
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            );
            binding.drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            binding.navigationView.setNavigationItemSelectedListener(this);

            // Setup FAB
            binding.fabAddPost.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateBlogActivity.class);
                startActivity(intent);
            });

            // Check if user is logged in
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            // Get FCM token
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d("FCM_TOKEN", "Token: " + token);
                        // Display token in a dialog for easy copying
                        new AlertDialog.Builder(this)
                            .setTitle("FCM Token")
                            .setMessage(token)
                            .setPositiveButton("Copy", (dialog, which) -> {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("FCM Token", token);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(this, "Token copied to clipboard", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Close", null)
                            .show();
                    } else {
                        Log.e("FCM_TOKEN", "Failed to get token", task.getException());
                    }
                });

            // Request notification permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
                }
            }

            // Load posts
            loadBlogPosts();
            
            // Setup carousel
            setupCarousel();
            
        } catch (Exception e) {
            Log.e("BlogApp", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupCarousel() {
        List<Integer> carouselImages = new ArrayList<>();
        // Add carousel images
        carouselImages.add(R.drawable.carousel_image1);
        carouselImages.add(R.drawable.carousel_image2);
        carouselImages.add(R.drawable.carousel_image3);

        ImageCarouselAdapter carouselAdapter = new ImageCarouselAdapter(carouselImages);
        binding.imageCarousel.setAdapter(carouselAdapter);
        
        // Auto-scroll functionality
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                int currentItem = binding.imageCarousel.getCurrentItem();
                int totalItems = carouselAdapter.getItemCount();
                binding.imageCarousel.setCurrentItem((currentItem + 1) % totalItems, true);
                handler.postDelayed(this, 3000); // Change image every 3 seconds
            }
        };
        handler.postDelayed(runnable, 3000);
    }

    private void loadBlogPosts() {
        try {
            Log.d("BlogApp", "Starting to load blog posts");
            
            if (databaseReference == null) {
                Log.e("BlogApp", "Database reference is null!");
                return;
            }

            // Show loading state
            binding.recyclerView.setVisibility(View.GONE);
            
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        Log.d("BlogApp", "Received data snapshot with " + dataSnapshot.getChildrenCount() + " posts");
                        
                        // Clear existing posts
                        blogPosts.clear();
                        
                        // Iterate through all posts
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            try {
                                // Log raw data for debugging
                                Log.d("BlogApp", "Post raw data: " + postSnapshot.getValue());
                                
                                BlogPost post = new BlogPost();
                                
                                // Manually get values to handle potential type mismatches
                                Object titleObj = postSnapshot.child("title").getValue();
                                Object contentObj = postSnapshot.child("content").getValue();
                                Object imageUrlObj = postSnapshot.child("imageUrl").getValue();
                                Object locationNameObj = postSnapshot.child("locationName").getValue();
                                Object userIdObj = postSnapshot.child("userId").getValue();
                                Object timestampObj = postSnapshot.child("timestamp").getValue();
                                
                                post.setTitle(titleObj != null ? titleObj.toString() : "");
                                post.setContent(contentObj != null ? contentObj.toString() : "");
                                post.setImageUrl(imageUrlObj != null ? imageUrlObj.toString() : "");
                                post.setLocationName(locationNameObj != null ? locationNameObj.toString() : "");
                                post.setUserId(userIdObj != null ? userIdObj.toString() : "");
                                post.setTimestamp(timestampObj != null ? Long.parseLong(timestampObj.toString()) : System.currentTimeMillis());
                                
                                blogPosts.add(post);
                                Log.d("BlogApp", "Added post: " + post.getTitle());
                            } catch (Exception e) {
                                Log.e("BlogApp", "Error parsing post: " + e.getMessage());
                            }
                        }
                        
                        // Sort posts by timestamp (newest first)
                        blogPosts.sort((p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                        
                        // Update UI
                        binding.recyclerView.setVisibility(View.VISIBLE);
                        blogAdapter.notifyDataSetChanged();
                        
                        // Log result
                        Log.d("BlogApp", "Loaded " + blogPosts.size() + " posts");
                        
                    } catch (Exception e) {
                        Log.e("BlogApp", "Error in onDataChange: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error loading posts", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("BlogApp", "Database error: " + databaseError.getMessage());
                    Toast.makeText(MainActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            });
            
        } catch (Exception e) {
            Log.e("BlogApp", "Error in loadBlogPosts: " + e.getMessage());
            Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();
        
        if (id == R.id.nav_emergency_contacts) {
            Toast.makeText(this, "Emergency Contacts clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_disaster_tips) {
            Toast.makeText(this, "Disaster Preparedness Tips clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_report_issue) {
            Toast.makeText(this, "Report an Issue clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_checkin) {
            Toast.makeText(this, "Check-in Safety clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_volunteer) {
            Toast.makeText(this, "Volunteer and Donate clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            // Sign out from Firebase
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPostClick(Post post) {
        // Handle post click
        Intent intent = new Intent(this, ViewPostActivity.class);
        intent.putExtra("postId", post.getPostId());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
                // Start listening for notifications
                NotificationHelper.listenForNotifications(this);
            } else {
                Log.d("MainActivity", "Notification permission denied");
                Toast.makeText(this, "Notifications are disabled. You may miss updates about new posts.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
