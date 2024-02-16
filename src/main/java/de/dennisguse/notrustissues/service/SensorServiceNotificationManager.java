package de.dennisguse.notrustissues.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import de.dennisguse.notrustissues.MainActivity;
import de.dennisguse.notrustissues.R;
import de.dennisguse.notrustissues.util.IntentUtils;

public class SensorServiceNotificationManager {

    public static final int NOTIFICATION_ID = 12;

    private static final String CHANNEL_ID = SensorServiceNotificationManager.class.getSimpleName();

    private final NotificationCompat.Builder notificationBuilder;

    private final NotificationManager notificationManager;

    SensorServiceNotificationManager(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setAllowBubbles(true);

        notificationManager.createNotificationChannel(notificationChannel);

        int pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addParentStack(MainActivity.class)
                .addNextIntent(IntentUtils.newIntent(context, MainActivity.class))
                .getPendingIntent(0, pendingIntentFlags);

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_logo_24dp);
    }

    Notification getNotification() {
        return notificationBuilder.build();
    }
}
