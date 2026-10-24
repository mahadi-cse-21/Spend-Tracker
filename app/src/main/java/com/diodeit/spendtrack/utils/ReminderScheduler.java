package com.diodeit.spendtrack.utils;

import com.diodeit.spendtrack.utils.Tr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Calendar;

public class ReminderScheduler {

    public static void setDailyReminder(Context context, boolean enable) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        if (alarmManager == null) return;

        if (enable) {
            // Check for Exact Alarm Permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, Tr.t("রিমাইন্ডার কাজ করার জন্য 'Alarms & Reminders' পারমিশন প্রয়োজন"), Toast.LENGTH_LONG).show();
                    try {
                        Intent intentSettings = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intentSettings);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }

            // ★ Read user-selected time
            PreferenceManager prefManager = new PreferenceManager(context);
            int hour = prefManager.getReminderHour();
            int minute = prefManager.getReminderMinute();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // If time has already passed today, start tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Schedule the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        } else {
            // Cancel the alarm
            alarmManager.cancel(pendingIntent);
        }
    }
}