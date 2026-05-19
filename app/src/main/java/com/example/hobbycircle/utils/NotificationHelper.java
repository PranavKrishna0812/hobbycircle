package com.example.hobbycircle.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.hobbycircle.R;
import com.example.hobbycircle.receivers.ReminderReceiver;

public class NotificationHelper {

    private final Context appContext;

    public NotificationHelper(Context context) {
        this.appContext = context.getApplicationContext();
        createNotificationChannel();
    }

    public void scheduleEventReminder(String eventId, String title, String location, long eventTimeMillis) {
        try {
            long triggerAtMillis = eventTimeMillis - (60 * 60 * 1000L); // 1 hour before event
            if (triggerAtMillis <= System.currentTimeMillis()) {
                triggerAtMillis = System.currentTimeMillis() + 10_000L; // fallback: 10 sec later
            }

            Intent intent = new Intent(appContext, ReminderReceiver.class);
            intent.setAction(Constants.ACTION_EVENT_REMINDER);
            intent.putExtra(Constants.EXTRA_EVENT_ID, safe(eventId));
            intent.putExtra(Constants.EXTRA_EVENT_TITLE, safe(title));
            intent.putExtra(Constants.EXTRA_EVENT_LOCATION, safe(location));
            intent.putExtra(Constants.EXTRA_EVENT_TIME, eventTimeMillis);

            int requestCode = safe(eventId).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
        } catch (Exception e) {
            // Keep app stable even if scheduling fails.
        }
    }

    public void cancelEventReminder(String eventId) {
        try {
            Intent intent = new Intent(appContext, ReminderReceiver.class);
            int requestCode = safe(eventId).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            // Keep app stable even if cancel fails.
        }
    }

    public void showReminderNotification(String title, String message) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, Constants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(safe(title).isEmpty()
                            ? appContext.getString(R.string.notification_reminder_title_default)
                            : safe(title))
                    .setContentText(safe(message).isEmpty()
                            ? appContext.getString(R.string.notification_reminder_body_default)
                            : safe(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(appContext);
            managerCompat.notify(Constants.NOTIFICATION_ID_EVENT_REMINDER, builder.build());
        } catch (SecurityException e) {
            // Notification permission not granted on Android 13+.
        } catch (Exception e) {
            // Keep app stable even if notification fails.
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    Constants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(appContext.getString(R.string.notification_channel_description));

            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}