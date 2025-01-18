package com.example.blogapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.blogapp.databinding.ItemBlogBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {
    private List<BlogPost> blogPosts;
    private OnBlogClickListener listener;

    public interface OnBlogClickListener {
        void onBlogClick(BlogPost post, int position);
    }

    public BlogAdapter(List<BlogPost> blogPosts, OnBlogClickListener listener) {
        this.blogPosts = blogPosts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlogBinding binding = ItemBlogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BlogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {
        holder.bind(blogPosts.get(position));
    }

    @Override
    public int getItemCount() {
        return blogPosts.size();
    }

    public void updatePosts(List<BlogPost> newPosts) {
        this.blogPosts = newPosts;
        notifyDataSetChanged();
    }

    class BlogViewHolder extends RecyclerView.ViewHolder {
        private final ItemBlogBinding binding;

        BlogViewHolder(ItemBlogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BlogPost post) {
            binding.titleText.setText(post.getTitle());
            binding.contentPreview.setText(post.getContent());
            
            // Display location if available
            String location = post.getLocationName();
            if (location != null && !location.isEmpty()) {
                binding.locationText.setVisibility(View.VISIBLE);
                binding.locationText.setText("📍 " + location);
            } else {
                binding.locationText.setVisibility(View.GONE);
            }

            // Format and display timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            binding.dateText.setText(sdf.format(new Date(post.getTimestamp())));

            // Load image if available
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.blogImage.setVisibility(View.VISIBLE);
                Glide.with(binding.blogImage.getContext())
                        .load(post.getImageUrl())
                        .centerCrop()
                        .into(binding.blogImage);
            } else {
                binding.blogImage.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBlogClick(post, getAdapterPosition());
                }
            });
        }
    }
}
