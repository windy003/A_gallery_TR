package com.example.photogallery;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * 回收站中的照片/视频实体类
 * 存储软删除的媒体文件信息
 */
@Entity(tableName = "deleted_photos")
public class DeletedPhoto implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private long mediaStoreId;      // MediaStore中的原始ID
    private String path;            // 文件路径
    private String name;            // 文件名
    private long dateAdded;         // 原始添加时间（秒）
    private long size;              // 文件大小（字节）
    private int mediaType;          // 媒体类型（1=图片, 2=视频）
    private long duration;          // 视频时长（毫秒），图片为0
    private long deletedAt;         // 删除时间（毫秒时间戳）
    private long expiresAt;         // 过期时间（毫秒时间戳）

    public DeletedPhoto() {
    }

    /**
     * 从Photo对象创建DeletedPhoto
     */
    public static DeletedPhoto fromPhoto(Photo photo) {
        DeletedPhoto deletedPhoto = new DeletedPhoto();
        deletedPhoto.mediaStoreId = photo.getId();
        deletedPhoto.path = photo.getPath();
        deletedPhoto.name = photo.getName();
        deletedPhoto.dateAdded = photo.getDateAdded();
        deletedPhoto.size = photo.getSize();
        deletedPhoto.mediaType = photo.getMediaType();
        deletedPhoto.duration = photo.getDuration();

        long currentTime = System.currentTimeMillis();
        deletedPhoto.deletedAt = currentTime;
        // 24小时后过期
        deletedPhoto.expiresAt = currentTime + (24 * 60 * 60 * 1000);

        return deletedPhoto;
    }

    /**
     * 转换回Photo对象（用于恢复）
     */
    public Photo toPhoto() {
        return new Photo(mediaStoreId, path, name, dateAdded, size, mediaType, duration);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getMediaStoreId() {
        return mediaStoreId;
    }

    public void setMediaStoreId(long mediaStoreId) {
        this.mediaStoreId = mediaStoreId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isVideo() {
        return mediaType == Photo.TYPE_VIDEO;
    }

    /**
     * 获取剩余时间（毫秒）
     */
    public long getRemainingTime() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
