package com.example.blogapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.example.blogapp.databinding.ActivityCreateBlogBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateBlogActivity extends AppCompatActivity {
    private static final String TAG = "CreateDistarReport";
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_LOCATION = 101;
    
    private ActivityCreateBlogBinding binding;
    private Uri selectedImageUri;
    private String currentPhotoPath;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLocation = "";

    // Launcher for gallery picker
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    loadImage();
                }
            });

    // Launcher for camera
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "Camera result received");
                    if (currentPhotoPath != null) {
                        selectedImageUri = Uri.fromFile(new File(currentPhotoPath));
                        loadImage();
                    }
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted");
                    openCamera();
                } else {
                    Log.d(TAG, "Camera permission denied");
                    Toast.makeText(this, "Camera permission is required to take photos", 
                        Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateBlogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupToolbar();
        setupClickListeners();
        binding.btnAddLocation.setOnClickListener(v -> requestLocation());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create New Report");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {
        binding.imageView.setOnClickListener(v -> showImagePickerDialog());
        binding.btnPost.setOnClickListener(v -> createPost());
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
            Log.d(TAG, "Photo file created: " + photoFile.getAbsolutePath());
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.blogapp.fileprovider",
                    photoFile);
            Log.d(TAG, "PhotoURI: " + photoURI);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            
            try {
                cameraLauncher.launch(takePictureIntent);
                Log.d(TAG, "Camera launched successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error launching camera", e);
                Toast.makeText(this, "Error launching camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Convert coordinates to address
                        getAddressFromLocation(location);
                    } else {
                        Toast.makeText(this, "Could not get location. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(Location location) {
        // Show loading state
        binding.locationInput.setText("Getting address...");
        
        // Run geocoding in background thread
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1
                );

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder locationText = new StringBuilder();

                        // Try to get the most detailed address possible
                        String featureName = address.getFeatureName();
                        String subLocality = address.getSubLocality();
                        String locality = address.getLocality();
                        String subAdminArea = address.getSubAdminArea();
                        String adminArea = address.getAdminArea();

                        // Build address from most specific to least specific
                        if (featureName != null && !featureName.matches("\\d+") && !locationText.toString().contains(featureName)) {
                            locationText.append(featureName).append(", ");
                        }
                        
                        if (subLocality != null && !subLocality.isEmpty()) {
                            locationText.append(subLocality).append(", ");
                        }
                        
                        if (locality != null && !locality.isEmpty()) {
                            locationText.append(locality).append(", ");
                        } else if (subAdminArea != null && !subAdminArea.isEmpty()) {
                            locationText.append(subAdminArea).append(", ");
                        }
                        
                        if (adminArea != null && !adminArea.isEmpty()) {
                            locationText.append(adminArea);
                        }

                        // Remove trailing comma and space if present
                        String finalLocation = locationText.toString().trim();
                        if (finalLocation.endsWith(",")) {
                            finalLocation = finalLocation.substring(0, finalLocation.length() - 1).trim();
                        }

                        // Store both the formatted address and the coordinates
                        currentLocation = finalLocation;
                        binding.locationInput.setText("📍 " + finalLocation);
                        Toast.makeText(this, "Location captured successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        handleGeocodeError();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(this::handleGeocodeError);
            }
        }).start();
    }

    private void handleGeocodeError() {
        Toast.makeText(this, "Could not get address. Please check your internet connection and try again.", 
                Toast.LENGTH_LONG).show();
        binding.locationInput.setText("Location not available");
        currentLocation = "";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createPost() {
        String title = binding.titleInput.getText().toString().trim();
        String content = binding.contentInput.getText().toString().trim();

        if (title.isEmpty()) {
            binding.titleInput.setError("Title is required");
            return;
        }

        if (content.isEmpty()) {
            binding.contentInput.setError("Content is required");
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new blog post
        BlogPost newPost = new BlogPost(
                title,
                content,
                selectedImageUri.toString(),
                currentLocation
        );

        // Send the blog post data back to MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("title", newPost.getTitle());
        resultIntent.putExtra("content", newPost.getContent());
        resultIntent.putExtra("imageUrl", newPost.getImageUrl());
        resultIntent.putExtra("location", newPost.getLocationName());
        resultIntent.putExtra("timestamp", newPost.getTimestamp());
        
        setResult(Activity.RESULT_OK, resultIntent);
        Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "Created image file: " + currentPhotoPath);
        return image;
    }

    private void loadImage() {
        if (selectedImageUri != null) {
            Log.d(TAG, "Loading image from URI: " + selectedImageUri);
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(binding.imageView);
            binding.addPhotoText.setVisibility(View.GONE);
        }
    }
}
