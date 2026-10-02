package com.abhinavpinisetti.consensus.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.abhinavpinisetti.consensus.ui.overview.TripOverviewActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives data messages sent by the Cloud Functions in /functions (member joined, itinerary
 * generated) and shows a notification that opens the trip.
 */
public class ConsensusMessagingService extends FirebaseMessagingService {

    @Override
    @SuppressWarnings("deprecation")
    public void onNewToken(@NonNull String token) {
        UserRepository.get().saveFcmToken(token);
    }

    @Override
    public void onRegistered(@NonNull String token) {
        UserRepository.get().saveFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = data.get("title");
        String body = data.get("body");
        String tripId = data.get("tripId");
        if (title == null && message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
        }
        if (title == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true);

        if (tripId != null) {
            Intent intent = TripOverviewActivity.intent(this, tripId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            builder.setContentIntent(PendingIntent.getActivity(this, tripId.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }

        NotificationManagerCompat.from(this).notify((tripId == null ? 0 : tripId.hashCode()) + title.hashCode(), builder.build());
    }
}
