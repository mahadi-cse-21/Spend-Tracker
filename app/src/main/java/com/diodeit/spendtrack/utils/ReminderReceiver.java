package com.diodeit.spendtrack.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);

        // ★ Crucial: Re-schedule the alarm for tomorrow at 8 PM
        PreferenceManager prefManager = new PreferenceManager(context);
        if (prefManager.isDailyReminderEnabled()) {
            ReminderScheduler.setDailyReminder(context, true);
        }
    }

    private void showNotification(Context context) {
        String channelId = "daily_reminder_channel";
        String channelName = "Daily Reminder";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent to open the app when clicking the notification
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, flags);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_receipt_long)
                .setContentTitle("স্পেন্ডট্র্যাক রিমাইন্ডার")
                .setContentText("আজকের খরচ কি যোগ করেছেন? নাকি আমাকে ভুলে গেলেন। এখনই অ্যাপে গিয়ে হিসাব আপডেট করুন। ")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }
}