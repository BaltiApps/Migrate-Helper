package balti.migratehelper;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;

public class UninstallService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        int dpiValue = intent.getIntExtra("dpiValue", 0);
        try {

            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).cancelAll();
            uninstall(this, dpiValue);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    void uninstall(Context context, int dpiValue) throws IOException, InterruptedException {

        String sourceDir = context.getApplicationInfo().sourceDir;

        if (sourceDir.startsWith("/system"))
            disableApp(context);


        if (dpiValue > 0) {
            Runtime.getRuntime().exec("su -c wm density " + dpiValue).waitFor();
        }

        File tempScript = new File(context.getFilesDir() + "/tempScript.sh");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
        String command = "#!/sbin/sh\n\n" +
                "mount -o rw,remount /system\n" +
                "mount -o rw,remount /data\n" +
                "mount -o rw,remount /system/app/MigrateHelper\n" +
                "mount -o rw,remount /data/data/balti.migratehelper\n" +
                "rm -rf " + context.getApplicationInfo().dataDir + " " + sourceDir + " " + TEMP_DIR_NAME + "\n" +
                "mount -o ro,remount /system\n";

        if (dpiValue > 0)
            command = command + "reboot\n";

        writer.write(command);
        writer.close();

        context.stopService(new Intent(context, StupidStartupService.class));
        Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath()).waitFor();

        stopSelf();

    }

    void disableApp(Context context){

        SharedPreferences.Editor editor = context.getSharedPreferences("main", MODE_PRIVATE).edit();
        editor.putBoolean("isDisabled", true);
        editor.commit();

        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
