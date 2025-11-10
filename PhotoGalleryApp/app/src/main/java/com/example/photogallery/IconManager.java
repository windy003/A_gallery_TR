package com.example.photogallery;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IconManager {
    private static final String PACKAGE_NAME = "com.example.photogallery";
    private static final String DEFAULT_ALIAS = PACKAGE_NAME + ".MainActivityDefault";
    private static final String COMPLETED_ALIAS = PACKAGE_NAME + ".MainActivityCompleted";

    private Context context;
    private PhotoManager photoManager;

    public IconManager(Context context) {
        this.context = context;
        this.photoManager = new PhotoManager(context);
    }

    /**
     * 检查并更新应用图标
     * 逻辑：所有日期文件夹都在当天之后 -> 显示已完成图标(ywc.png)
     *      存在当天或之前的日期文件夹 -> 显示默认图标(a2048x2048.png)
     */
    public void updateAppIcon() {
        boolean shouldShowCompleted = areAllDateFoldersAfterToday();
        boolean isCurrentlyCompleted = isCompletedIconEnabled();

        // 只在状态改变时才切换图标，避免频繁切换导致桌面位置变动
        if (shouldShowCompleted && !isCurrentlyCompleted) {
            enableCompletedIcon();
        } else if (!shouldShowCompleted && isCurrentlyCompleted) {
            enableDefaultIcon();
        }
        // 如果状态没变，不做任何操作
    }

    /**
     * 检查当前是否启用了已完成图标
     */
    private boolean isCompletedIconEnabled() {
        PackageManager pm = context.getPackageManager();
        int completedState = pm.getComponentEnabledSetting(
            new ComponentName(context, COMPLETED_ALIAS)
        );

        // COMPONENT_ENABLED_STATE_ENABLED = 1 (显式启用)
        // COMPONENT_ENABLED_STATE_DEFAULT = 0 (使用manifest默认值，即disabled)
        return completedState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    /**
     * 检查所有日期文件夹是否都在当天之后
     * 基于PhotoManager的真实日期分组（DATE_ADDED + 3天）
     */
    private boolean areAllDateFoldersAfterToday() {
        java.util.Map<String, List<Photo>> photosByDate = photoManager.getPhotosByDisplayDate();

        // 如果没有日期文件夹，认为是已完成状态
        if (photosByDate == null || photosByDate.isEmpty()) {
            return true;
        }

        String today = getTodayDateString();

        // 检查是否所有日期文件夹都在今天之后
        for (String dateFolder : photosByDate.keySet()) {
            if (dateFolder.compareTo(today) <= 0) {
                // 存在当天或之前的日期文件夹，未完成
                return false;
            }
        }

        // 所有日期文件夹都在今天之后，已完成
        return true;
    }

    /**
     * 启用已完成图标 (ywc.png)
     */
    private void enableCompletedIcon() {
        PackageManager pm = context.getPackageManager();

        // 禁用默认图标
        pm.setComponentEnabledSetting(
            new ComponentName(context, DEFAULT_ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );

        // 启用已完成图标
        pm.setComponentEnabledSetting(
            new ComponentName(context, COMPLETED_ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
    }

    /**
     * 启用默认图标 (a2048x2048.png)
     */
    private void enableDefaultIcon() {
        PackageManager pm = context.getPackageManager();

        // 启用默认图标
        pm.setComponentEnabledSetting(
            new ComponentName(context, DEFAULT_ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );

        // 禁用已完成图标
        pm.setComponentEnabledSetting(
            new ComponentName(context, COMPLETED_ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }

    /**
     * 获取今天的日期字符串 (格式: yyyy-MM-dd)
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
