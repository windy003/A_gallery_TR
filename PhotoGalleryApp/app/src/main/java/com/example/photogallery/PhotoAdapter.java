package com.example.photogallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private Context context;
    private List<Photo> photos;
    private OnPhotoClickListener listener;
    private DateFolderManager dateFolderManager;

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    public PhotoAdapter(Context context, List<Photo> photos, OnPhotoClickListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
        this.dateFolderManager = new DateFolderManager(context);
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);

        Glide.with(context)
                .load(new File(photo.getPath()))
                .centerCrop()
                .into(holder.imageViewPhoto);

        // Get all date folders containing this photo
        List<String> dateFolders = getDateFoldersForPhoto(photo.getPath());
        if (dateFolders.isEmpty()) {
            holder.textViewAdded.setVisibility(View.GONE);
        } else {
            holder.textViewAdded.setVisibility(View.VISIBLE);
            // Format dates as MM-DD and join with space
            StringBuilder dateText = new StringBuilder();
            for (int i = 0; i < dateFolders.size(); i++) {
                if (i > 0) {
                    dateText.append(" ");
                }
                dateText.append(formatDateShort(dateFolders.get(i)));
            }
            holder.textViewAdded.setText(dateText.toString());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    /**
     * Get all date folders containing this photo
     */
    private List<String> getDateFoldersForPhoto(String photoPath) {
        List<String> result = new ArrayList<>();
        List<String> allDateFolders = dateFolderManager.getAllDateFolders();
        for (String date : allDateFolders) {
            List<String> photoPaths = dateFolderManager.getPhotosForDate(date);
            if (photoPaths.contains(photoPath)) {
                result.add(date);
            }
        }
        return result;
    }

    /**
     * Format date from yyyy-MM-dd to MM-dd
     */
    private String formatDateShort(String fullDate) {
        if (fullDate != null && fullDate.length() >= 10) {
            // Extract MM-dd from yyyy-MM-dd
            return fullDate.substring(5);
        }
        return fullDate;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPhoto;
        TextView textViewAdded;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
            textViewAdded = itemView.findViewById(R.id.textViewAdded);
        }
    }
}
