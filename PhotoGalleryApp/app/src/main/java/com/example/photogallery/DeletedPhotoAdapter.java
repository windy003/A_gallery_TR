package com.example.photogallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * 回收站项目适配器
 */
public class DeletedPhotoAdapter extends RecyclerView.Adapter<DeletedPhotoAdapter.ViewHolder> {

    private Context context;
    private List<DeletedPhoto> deletedPhotos;
    private OnItemActionListener listener;
    private OnItemClickListener clickListener;

    public interface OnItemActionListener {
        void onRestore(DeletedPhoto item, int position);
        void onDelete(DeletedPhoto item, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public DeletedPhotoAdapter(Context context, List<DeletedPhoto> deletedPhotos, OnItemActionListener listener) {
        this.context = context;
        this.deletedPhotos = deletedPhotos;
        this.listener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_deleted_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletedPhoto item = deletedPhotos.get(position);

        // 设置文件名
        holder.textViewName.setText(item.getName());

        // 设置文件大小
        holder.textViewSize.setText(formatFileSize(item.getSize()));

        // 设置剩余时间
        long remainingTime = item.getRemainingTime();
        holder.textViewRemainingTime.setText(formatRemainingTime(remainingTime));

        // 加载缩略图
        File file = new File(item.getPath());
        if (file.exists()) {
            Glide.with(context)
                    .load(file)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imageViewThumbnail);
        } else {
            holder.imageViewThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 显示视频图标
        if (item.isVideo()) {
            holder.imageViewVideoIcon.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewVideoIcon.setVisibility(View.GONE);
        }

        // 设置按钮监听器
        holder.buttonRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestore(item, holder.getAdapterPosition());
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item, holder.getAdapterPosition());
            }
        });

        // 点击缩略图查看全屏
        holder.imageViewThumbnail.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(holder.getAdapterPosition());
            }
        });
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

    public void updateData(List<DeletedPhoto> newData) {
        this.deletedPhotos = newData;
        notifyDataSetChanged();
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    private String formatRemainingTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "已过期";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "剩余 %d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "剩余 %d分钟", minutes);
        } else {
            return String.format(Locale.getDefault(), "剩余 %d秒", seconds);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumbnail;
        ImageView imageViewVideoIcon;
        TextView textViewName;
        TextView textViewSize;
        TextView textViewRemainingTime;
        Button buttonRestore;
        Button buttonDelete;

        ViewHolder(View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.imageViewThumbnail);
            imageViewVideoIcon = itemView.findViewById(R.id.imageViewVideoIcon);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewSize = itemView.findViewById(R.id.textViewSize);
            textViewRemainingTime = itemView.findViewById(R.id.textViewRemainingTime);
            buttonRestore = itemView.findViewById(R.id.buttonRestore);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
