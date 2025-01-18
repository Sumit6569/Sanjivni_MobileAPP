package com.example.blogapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.blogapp.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener, 
                   BlogAdapter.OnBlogClickListener {
                   
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private BlogAdapter blogAdapter;
    private List<BlogPost> blogPosts;
    private ImageCarouselAdapter carouselAdapter;
    private List<Integer> carouselImages;

    private final ActivityResultLauncher<Intent> createBlogLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Get blog post data from the result
                    Intent data = result.getData();
                    String title = data.getStringExtra("title");
                    String content = data.getStringExtra("content");
                    String imageUrl = data.getStringExtra("imageUrl");
                    String location = data.getStringExtra("location");
                    long timestamp = data.getLongExtra("timestamp", System.currentTimeMillis());

                    // Create new blog post and add it to the list
                    BlogPost newPost = new BlogPost(title, content, imageUrl, location);
                    blogPosts.add(0, newPost); // Add at the beginning of the list
                    blogAdapter.notifyItemInserted(0);
                    binding.recyclerView.smoothScrollToPosition(0);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupDrawer();
        setupCarousel();
        setupRecyclerView();
        setupFab();
    }

    private void setupCarousel() {
        carouselImages = new ArrayList<>();
        // Add carousel images (JPG files)
        carouselImages.add(R.drawable.carousel_image1);
        carouselImages.add(R.drawable.carousel_image2);
        carouselImages.add(R.drawable.carousel_image3);

        carouselAdapter = new ImageCarouselAdapter(carouselImages);
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

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
    }

    private void setupDrawer() {
        toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupRecyclerView() {
        blogPosts = new ArrayList<>();
        blogAdapter = new BlogAdapter(blogPosts, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(blogAdapter);
        
        // Load initial blog posts
        loadBlogPosts();
    }

    private void setupFab() {
        binding.fabAddPost.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateBlogActivity.class);
            createBlogLauncher.launch(intent);
        });
    }

    private void loadBlogPosts() {
        // For now, we'll just keep the posts in memory
        // You might want to implement persistent storage later
        if (blogPosts.isEmpty()) {
            // Only add sample posts if the list is empty
            blogPosts.add(new BlogPost("Welcome to BlogApp", 
                "Start creating your own blog posts by clicking the + button!", 
                "", ""));
            blogAdapter.notifyDataSetChanged();
        }
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
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBlogClick(BlogPost post, int position) {
        // Handle blog post click
        // You can open a detail view activity here
        Toast.makeText(this, "Clicked: " + post.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
