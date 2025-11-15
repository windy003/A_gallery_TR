package com.example.photogallery;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * 回收站数据访问对象
 */
@Dao
public interface DeletedPhotoDao {

    /**
     * 插入一个删除的照片/视频
     */
    @Insert
    long insert(DeletedPhoto deletedPhoto);

    /**
     * 获取所有回收站中的项目
     */
    @Query("SELECT * FROM deleted_photos ORDER BY deletedAt DESC")
    List<DeletedPhoto> getAll();

    /**
     * 获取回收站中的项目数量
     */
    @Query("SELECT COUNT(*) FROM deleted_photos")
    int getCount();

    /**
     * 根据ID获取删除的照片
     */
    @Query("SELECT * FROM deleted_photos WHERE id = :id")
    DeletedPhoto getById(int id);

    /**
     * 根据MediaStore ID获取删除的照片
     */
    @Query("SELECT * FROM deleted_photos WHERE mediaStoreId = :mediaStoreId")
    DeletedPhoto getByMediaStoreId(long mediaStoreId);

    /**
     * 删除单个项目（永久删除或恢复后）
     */
    @Delete
    void delete(DeletedPhoto deletedPhoto);

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM deleted_photos WHERE id = :id")
    void deleteById(int id);

    /**
     * 获取所有已过期的项目
     */
    @Query("SELECT * FROM deleted_photos WHERE expiresAt <= :currentTime")
    List<DeletedPhoto> getExpired(long currentTime);

    /**
     * 删除所有已过期的项目
     */
    @Query("DELETE FROM deleted_photos WHERE expiresAt <= :currentTime")
    int deleteExpired(long currentTime);

    /**
     * 清空回收站
     */
    @Query("DELETE FROM deleted_photos")
    void deleteAll();
}
