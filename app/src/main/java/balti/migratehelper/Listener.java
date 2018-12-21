package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by sayantan on 21/10/17.
 */

public class Listener extends BroadcastReceiver {

    static String PROGRESS_CHANNEL = "App restore progress";
    static String INIT_CHANNEL = "Initial progress";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel initialNotifChannel = new NotificationChannel(INIT_CHANNEL, INIT_CHANNEL, NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel progressChannel = new NotificationChannel(PROGRESS_CHANNEL, PROGRESS_CHANNEL, NotificationManager.IMPORTANCE_LOW);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(initialNotifChannel);
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences main = context.getSharedPreferences("main", Context.MODE_PRIVATE);

            if (!main.getBoolean("temporaryDisable", false)) {

                PackageManager packageManager = context.getPackageManager();
                ComponentName componentName = new ComponentName(context.getPackageName(), MainActivity.class.getName());
                if (!main.getBoolean("isDisabled", false)) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(new Intent(context, StupidStartupService.class));
                    } else {
                        context.startService(new Intent(context, StupidStartupService.class));
                    }
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
        }
    }
}
