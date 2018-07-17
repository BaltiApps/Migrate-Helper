package balti.migratehelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

/**
 * Created by sayantan on 23/10/17.
 */

public class StupidStartupService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notif(this);
        return super.onStartCommand(intent, flags, startId);
    }

    void notif(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationCompat.Builder notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel initialNotifChannel = new NotificationChannel("FIXER", "App restored notification", NotificationManager.IMPORTANCE_HIGH);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(initialNotifChannel);
            notification = new NotificationCompat.Builder(context, "FIXER");
        }
        else {
            notification = new NotificationCompat.Builder(context);
            notification.setPriority(Notification.PRIORITY_MAX);
        }

        notification.setSmallIcon(R.drawable.ic_fix)
                .setContentTitle(context.getString(R.string.notifHeader))
                .setContentText(context.getString(R.string.notifBody));

        PendingIntent view = PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), 0);

        notification.setContentIntent(view);

        notification.setVisibility(Notification.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_ALL)
                .setOngoing(true);

        startForeground(232, notification.build());
    }

}
