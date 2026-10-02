package com.abhinavpinisetti.consensus.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.abhinavpinisetti.consensus.R;

public final class Notifications {

    private Notifications() {}

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
