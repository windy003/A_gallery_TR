package com.example.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateFolderManager {
    private static final String PREFS_NAME = "DateFolderPrefs";
    private static final String KEY_DATE_FOLDERS = "date_folders";
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;

    // Map: 日期字符串 -> 图片路径列表
    private Map<String, List<String>> dateFolders;

    public DateFolderManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadDateFolders();
    }

    private void loadDateFolders() {
        String json = prefs.getString(KEY_DATE_FOLDERS, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            dateFolders = gson.fromJson(json, type);
        } else {
            dateFolders = new HashMap<>();
        }
    }

    private void saveDateFolders() {
        String json = gson.toJson(dateFolders);
        prefs.edit().putString(KEY_DATE_FOLDERS, json).apply();
    }

    public void addPhotoToDateFolder(String photoPath, String date) {
        if (!dateFolders.containsKey(date)) {
            dateFolders.put(date, new ArrayList<>());
        }
        List<String> photos = dateFolders.get(date);
        if (!photos.contains(photoPath)) {
            photos.add(photoPath);
            saveDateFolders();
        }
    }

    public void removePhotoFromDateFolder(String photoPath, String date) {
        if (dateFolders.containsKey(date)) {
            List<String> photos = dateFolders.get(date);
            photos.remove(photoPath);
            if (photos.isEmpty()) {
                dateFolders.remove(date);
            }
            saveDateFolders();
        }
    }

    public List<String> getPhotosForDate(String date) {
        return dateFolders.getOrDefault(date, new ArrayList<>());
    }

    public List<String> getAllDateFolders() {
        return new ArrayList<>(dateFolders.keySet());
    }

    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String getDateAfterDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    public boolean hasPhotosForDate(String date) {
        return dateFolders.containsKey(date) && !dateFolders.get(date).isEmpty();
    }
}
