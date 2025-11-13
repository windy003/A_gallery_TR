package com.example.photogallery;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

/**
 * 处理小部件定时更新和设备重启后恢复的广播接收器
 */
public class WidgetUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "WidgetUpdateReceiver";
    private static final String ACTION_UPDATE_WIDGET = "com.example.photogallery.ACTION_UPDATE_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "收到广播: " + action + " (已禁用自动更新)");

        // 已禁用所有自动更新逻辑
        // 小部件只在用户打开app时才更新状态
        // 这样可以避免在app不能常驻后台的手机上出现状态不一致的问题
    }

    /**
     * 更新小部件（不扫描照片，只检查日期）
     */
    private void updateWidget(Context context) {
        try {
            // 直接更新小部件，使用已保存的状态
            CompletedDateWidget.updateAllWidgets(context);
        } catch (Exception e) {
            Log.e(TAG, "更新小部件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置定时更新任务（每天零点更新）
     */
    public static void scheduleWidgetUpdate(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "无法获取AlarmManager");
                return;
            }

            Intent intent = new Intent(context, WidgetUpdateReceiver.class);
            intent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 计算下一个零点的时间
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // 如果今天的零点已经过了，设置为明天零点
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            long triggerTime = calendar.getTimeInMillis();

            // 使用setRepeating设置每天重复的定时任务
            // 小米系统可能会对定时任务进行优化，使用setExactAndAllowWhileIdle确保准时执行
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // API 23+: 使用setExactAndAllowWhileIdle，即使设备休眠也能执行
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
                Log.d(TAG, "已设置精确定时任务（API 23+），下次执行: " + calendar.getTime());
            } else {
                // API 19-22: 使用setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
                Log.d(TAG, "已设置精确定时任务，下次执行: " + calendar.getTime());
            }

            // 注意：由于小米系统的限制，单次定时任务可能被清除
            // 因此在onReceive中需要重新设置下一次的定时任务
        } catch (Exception e) {
            Log.e(TAG, "设置定时任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 取消定时更新任务
     */
    public static void cancelWidgetUpdate(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }

            Intent intent = new Intent(context, WidgetUpdateReceiver.class);
            intent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "已取消定时任务");
        } catch (Exception e) {
            Log.e(TAG, "取消定时任务失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
