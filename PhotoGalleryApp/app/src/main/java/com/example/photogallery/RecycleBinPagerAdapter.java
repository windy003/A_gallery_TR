package com.example.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.List;

/**
 * 回收站全屏查看适配器
 */
public class RecycleBinPagerAdapter extends RecyclerView.Adapter<RecycleBinPagerAdapter.ViewHolder> {

    private Context context;
    private List<DeletedPhoto> deletedPhotos;
    private OnImageClickListener clickListener;

    public interface OnImageClickListener {
        void onImageClick();
    }

    public RecycleBinPagerAdapter(Context context, List<DeletedPhoto> deletedPhotos) {
        this.context = context;
        this.deletedPhotos = deletedPhotos;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TouchImageView imageView = new TouchImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletedPhoto item = deletedPhotos.get(position);

        holder.imageView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick();
            }
        });

        File file = new File(item.getPath());
        if (file.exists()) {
            // 获取屏幕尺寸
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int maxSize = Math.min(4096, Math.max(screenWidth, screenHeight) * 3);

            Glide.with(context)
                    .asBitmap()
                    .load(file)
                    .override(maxSize, maxSize)
                    .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.CENTER_INSIDE)
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            holder.imageView.setImageBitmap(resource.getWidth(), resource.getHeight());
                            holder.imageView.setImageBitmap(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return deletedPhotos != null ? deletedPhotos.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < deletedPhotos.size()) {
            deletedPhotos.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, deletedPhotos.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TouchImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = (TouchImageView) itemView;
        }
    }
}
