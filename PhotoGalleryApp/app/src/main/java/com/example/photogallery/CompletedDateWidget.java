package com.example.photogallery;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * 显示最近完成日期的桌面小部件
 */
public class CompletedDateWidget extends AppWidgetProvider {
    private static final String PREFS_NAME = "IconManagerPrefs";
    private static final String KEY_COMPLETED_DATE = "completed_date";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有的widget实例
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // 监听自定义的更新广播
        if ("com.example.photogallery.UPDATE_WIDGET".equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CompletedDateWidget.class)
            );
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    /**
     * 更新单个widget
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 获取保存的完成状态和日期
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isCompleted = prefs.getBoolean("is_completed", false);
        String completedDate = prefs.getString(KEY_COMPLETED_DATE, "");

        // 构建widget的RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_completed_date);

        if (isCompleted) {
            // 已完成状态：显示已完成图标 + 日期
            views.setImageViewResource(R.id.widget_icon, R.drawable.ywc);

            // 设置日期文本（只显示月/日，节省空间）
            if (!completedDate.isEmpty()) {
                // 从 yyyy/MM/dd 格式提取 MM/dd
                String[] parts = completedDate.split("/");
                if (parts.length == 3) {
                    String shortDate = parts[1] + "/" + parts[2]; // MM/dd
                    views.setTextViewText(R.id.widget_date_text, shortDate);
                } else {
                    views.setTextViewText(R.id.widget_date_text, completedDate);
                }
                views.setViewVisibility(R.id.widget_date_text, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_date_text, android.view.View.GONE);
            }
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
     * 静态方法：从外部触发所有widget的更新
     */
    public static void updateAllWidgets(Context context) {
        Intent intent = new Intent(context, CompletedDateWidget.class);
        intent.setAction("com.example.photogallery.UPDATE_WIDGET");
        context.sendBroadcast(intent);
    }
}
