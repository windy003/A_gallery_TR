package com.example.photogallery;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoManager {
    private Context context;
    private DateFolderManager dateFolderManager;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "PhotoManagerPrefs";
    private static final String KEY_PROCESSED_PHOTOS = "processed_photos";

    public PhotoManager(Context context) {
        this.context = context;
        this.dateFolderManager = new DateFolderManager(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Photo> getAllPhotos() {
        List<Photo> photos = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
        };

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        // 获取已处理的图片路径集合
        Set<String> processedPhotos = prefs.getStringSet(KEY_PROCESSED_PHOTOS, new HashSet<>());
        Set<String> newProcessedPhotos = new HashSet<>(processedPhotos);
        boolean hasNewPhotos = false;

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String path = cursor.getString(pathColumn);
                String name = cursor.getString(nameColumn);
                long dateAdded = cursor.getLong(dateColumn);
                long size = cursor.getLong(sizeColumn);

                Photo photo = new Photo(id, path, name, dateAdded, size);
                photo.setUri(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                photos.add(photo);

                // 检查是否为新图片，如果是则自动添加到3天后的文件夹
                if (!processedPhotos.contains(path)) {
                    String threeDaysLaterDate = DateFolderManager.getDateAfterDays(3);
                    dateFolderManager.addPhotoToDateFolder(path, threeDaysLaterDate);
                    newProcessedPhotos.add(path);
                    hasNewPhotos = true;
                }
            }
            cursor.close();
        }

        // 保存已处理的图片列表
        if (hasNewPhotos) {
            prefs.edit().putStringSet(KEY_PROCESSED_PHOTOS, newProcessedPhotos).apply();
        }

        return photos;
    }
}
