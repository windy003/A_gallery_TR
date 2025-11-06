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

        // Check if photo is in any date folder
        boolean isInDateFolder = isPhotoInAnyDateFolder(photo.getPath());
        holder.textViewAdded.setVisibility(isInDateFolder ? View.VISIBLE : View.GONE);

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
     * Check if a photo is in any date folder
     */
    private boolean isPhotoInAnyDateFolder(String photoPath) {
        List<String> dateFolders = dateFolderManager.getAllDateFolders();
        for (String date : dateFolders) {
            List<String> photoPaths = dateFolderManager.getPhotosForDate(date);
            if (photoPaths.contains(photoPath)) {
                return true;
            }
        }
        return false;
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
