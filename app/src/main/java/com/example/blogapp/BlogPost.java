package com.example.blogapp;

public class BlogPost {
    private String title;
    private String content;
    private String imageUrl;
    private String locationName;
    private long timestamp;

    public BlogPost() {
        // Default constructor
        this.timestamp = System.currentTimeMillis();
    }

    public BlogPost(String title, String content, String imageUrl, String locationName) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.locationName = locationName;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLocationName() {
        return locationName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
