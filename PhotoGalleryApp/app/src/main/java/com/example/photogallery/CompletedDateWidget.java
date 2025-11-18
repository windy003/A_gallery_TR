package com.example.photogallery;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 显示最近完成日期的桌面小部件
 */
public class CompletedDateWidget extends AppWidgetProvider {
    private static final String PREFS_NAME = "IconManagerPrefs";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有的widget实例
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * 更新单个widget
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 获取保存的完成状态
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isCompleted = prefs.getBoolean("is_completed", false);

        // 获取当天日期（格式: MM/dd）
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        Log.d("CompletedDateWidget", "更新widget - 完成状态: " + isCompleted + ", 当天日期: " + todayDate);

        // 构建widget的RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_completed_date);

        if (isCompleted) {
            // 已完成状态：显示已完成图标 + 当天日期
            views.setImageViewResource(R.id.widget_icon, R.drawable.ywc);
            views.setTextViewText(R.id.widget_date_text, todayDate);
            views.setViewVisibility(R.id.widget_date_text, android.view.View.VISIBLE);
        } else {
            // 未完成状态：只显示默认图标，不显示日期
            views.setImageViewResource(R.id.widget_icon, R.drawable.a2048x2048);
            views.setViewVisibility(R.id.widget_date_text, android.view.View.GONE);
        }

        // 设置点击事件 - 点击widget打开应用
        Intent intent = new Intent(context, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // 更新widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * 静态方法：从外部触发所有widget的更新（直接更新，不使用广播）
     */
    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, CompletedDateWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            Log.d("CompletedDateWidget", "更新小部件，数量: " + appWidgetIds.length);

            // 直接调用更新方法，不使用广播（避免被小米系统拦截）
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
                Log.d("CompletedDateWidget", "已更新widget ID: " + appWidgetId);
            }
        } catch (Exception e) {
            Log.e("CompletedDateWidget", "更新小部件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
