package com.example.hobbycircle.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.hobbycircle.R;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.NotificationHelper;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        if (!Constants.ACTION_EVENT_REMINDER.equals(action)) {
            return;
        }

        String title = intent.getStringExtra(Constants.EXTRA_EVENT_TITLE);
        String location = intent.getStringExtra(Constants.EXTRA_EVENT_LOCATION);
        long eventTimeMillis = intent.getLongExtra(Constants.EXTRA_EVENT_TIME, 0L);

        String body;
        if (location != null && !location.trim().isEmpty()) {
            body = context.getString(R.string.notification_reminder_body_location, location);
        } else if (eventTimeMillis > 0L) {
            body = context.getString(R.string.notification_reminder_body_time);
        } else {
            body = context.getString(R.string.notification_reminder_body_default);
        }

        NotificationHelper helper = new NotificationHelper(context);
        String safeTitle = title != null && !title.trim().isEmpty()
                ? title
                : context.getString(R.string.notification_reminder_title_default);
        helper.showReminderNotification(safeTitle, body);
    }
}