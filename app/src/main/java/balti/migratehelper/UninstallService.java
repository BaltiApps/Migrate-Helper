package balti.migratehelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

import static balti.migratehelper.CommonTools.METADATA_HOLDER_DIR;
import static balti.migratehelper.CommonTools.TEMP_DIR_NAME_NEW;
import static balti.migratehelper.CommonTools.TEMP_DIR_NAME_OLD;
import static balti.migratehelper.Listener.PROGRESS_CHANNEL;

//import static balti.migratehelper.CommonTools.TEMP_DIR_NAME;

public class UninstallService extends Service {

    int UNINSTALL_START_ID = 234;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification uninstallNotif = new NotificationCompat.Builder(this, PROGRESS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentText(getString(R.string.finishing))
                .build();

        startForeground(UNINSTALL_START_ID, uninstallNotif);

        int dpiValue = intent.getIntExtra("dpiValue", 0);
        boolean doReboot = intent.getBooleanExtra("doReboot", false);
        boolean doUninstall = intent.getBooleanExtra("doUninstall", true);
        try {

            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).cancelAll();
            finishTasks(dpiValue, doUninstall, doReboot);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    void finishTasks(int dpiValue, boolean doUninstall, boolean doReboot) throws IOException, InterruptedException {

        String sourceDir = getApplicationInfo().sourceDir;
        sourceDir = sourceDir.substring(0, sourceDir.lastIndexOf('/'));

        Process fullProcess = Runtime.getRuntime().exec("su");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fullProcess.getOutputStream()));

        if (dpiValue > 0) {
            writer.write("wm density " + dpiValue + "\n");
        }

        if (doUninstall) {

            writer.write("mount -o rw,remount " + TEMP_DIR_NAME_OLD + "\n");
            writer.write("mount -o rw,remount " + TEMP_DIR_NAME_NEW + "\n");
            writer.write("mount -o rw,remount /data\n");
            writer.write("rm -rf " + TEMP_DIR_NAME_OLD + "\n");
            writer.write("rm -rf " + TEMP_DIR_NAME_NEW + "\n");
            writer.write("rm -rf " + METADATA_HOLDER_DIR + "\n");
            writer.write("rm -rf /data/data/*.tar.gz\n");

            if (sourceDir.startsWith("/system")) {
                disableApp();
                writer.write("mount -o rw,remount /system\n");
                writer.write("mount -o rw,remount /system/app/MigrateHelper\n");
                writer.write("rm -rf " + getApplicationInfo().dataDir + " " + sourceDir + "\n");
            } else {
                writer.write("pm uninstall " + getPackageName() + "\n");
            }

            writer.write("rm -rf /sdcard/Android/data/balti.migratehelper/helper\n");

        }

        if (doReboot) {
            writer.write("reboot\n");
        }

        writer.write("exit\n");

        stopService(new Intent(this, StupidStartupService.class));

        writer.flush();
        fullProcess.waitFor();

        stopSelf();

    }

    void disableApp() {

        SharedPreferences.Editor editor = getSharedPreferences("main", MODE_PRIVATE).edit();
        editor.putBoolean("isDisabled", true);
        editor.commit();

        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
