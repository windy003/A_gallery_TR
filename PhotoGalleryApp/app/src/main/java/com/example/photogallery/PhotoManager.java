package com.example.photogallery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoManager {
    private Context context;
    private static final int DELAY_DAYS = 3; // 延迟天数

    public PhotoManager(Context context) {
        this.context = context;
    }

    /**
     * 从MediaStore获取所有图片
     */
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
            }
            cursor.close();
        }

        return photos;
    }

    /**
     * 根据图片的 DATE_ADDED + DELAY_DAYS 对图片进行分组
     *
     * @return Map<日期字符串, 图片列表>，按日期倒序排列
     */
    public Map<String, List<Photo>> getPhotosByDisplayDate() {
        Map<String, List<Photo>> photoMap = new LinkedHashMap<>();
        List<Photo> allPhotos = getAllPhotos();

        for (Photo photo : allPhotos) {
            String displayDate = getDisplayDate(photo.getDateAdded());

            if (!photoMap.containsKey(displayDate)) {
                photoMap.put(displayDate, new ArrayList<>());
            }
            photoMap.get(displayDate).add(photo);
        }

        return photoMap;
    }

    /**
     * 获取指定日期的所有图片
     *
     * @param dateString 日期字符串，格式：yyyy-MM-dd
     * @return 该日期对应的图片列表
     */
    public List<Photo> getPhotosForDate(String dateString) {
        List<Photo> photos = new ArrayList<>();
        List<Photo> allPhotos = getAllPhotos();

        for (Photo photo : allPhotos) {
            String displayDate = getDisplayDate(photo.getDateAdded());
            if (displayDate.equals(dateString)) {
                photos.add(photo);
            }
        }

        return photos;
    }

    /**
     * 根据图片的DATE_ADDED计算显示日期（DATE_ADDED + DELAY_DAYS天）
     *
     * @param dateAddedSeconds DATE_ADDED时间戳（秒）
     * @return 日期字符串，格式：yyyy-MM-dd
     */
    public String getDisplayDate(long dateAddedSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateAddedSeconds * 1000); // 转换为毫秒
        calendar.add(Calendar.DAY_OF_YEAR, DELAY_DAYS);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * 获取今天的日期字符串
     */
    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取指定天数后的日期字符串
     */
    public static String getDateAfterDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * 获取图片应该显示的日期（用于UI显示）
     *
     * @param photo 图片对象
     * @return 日期字符串
     */
    public String getPhotoDisplayDate(Photo photo) {
        return getDisplayDate(photo.getDateAdded());
    }
}
