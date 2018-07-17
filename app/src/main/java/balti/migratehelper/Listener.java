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
import android.widget.Toast;

/**
 * Created by sayantan on 21/10/17.
 */

public class Listener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel initialNotifChannel = new NotificationChannel("FIXER", "App restored notification", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel progressChannel = new NotificationChannel("PROGRESS", "Permission restore progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(initialNotifChannel);
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences main = context.getSharedPreferences("main", Context.MODE_PRIVATE);
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context.getPackageName(), MainActivity.class.getName());
            if (!main.getBoolean("isDisabled", false)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, StupidStartupService.class));
                }
                else {
                    context.startService(new Intent(context, StupidStartupService.class));
                }
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
            else {
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
        else if (intent.getAction().equals(context.getString(R.string.actionSu)))
            restoreSu(context);
        else if (intent.getAction().equals(context.getString(R.string.actionTWRP)))
            restoreTWRP(context);
    }

    public void restoreSu(Context context) {
        context.startActivity(new Intent(context, MainActivity.class).setAction("rootRestore"));
    }

    public void restoreTWRP(Context context) {
        context.startActivity(new Intent(context, MainActivity.class).setAction("twrpRestore"));
    }
}
