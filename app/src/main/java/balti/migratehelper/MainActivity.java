package balti.migratehelper;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;

public class MainActivity extends AppCompatActivity {

    Button rootRestoreButton, disable, selectiveRestore;

    ImageButton close;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !main.getBoolean("android_version_warning", false)){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.too_fast)
                    .setMessage(R.string.too_fast_desc)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            editor.putBoolean("android_version_warning", true);
                            editor.commit();
                        }
                    })
                    .show();
        }

        rootRestoreButton = findViewById(R.id.restoreSu);
        rootRestoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.do_not_use)
                        .setMessage(R.string.do_not_use_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(MainActivity.this, AppSelector.class). putExtra("all?", true));
                            }
                        })
                        .setNegativeButton(R.string.later, null)
                        .show();
            }
        });

        selectiveRestore = findViewById(R.id.selective_restore);
        selectiveRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AppSelector.class). putExtra("all?", false));
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent toSendIntent = new Intent(MainActivity.this, ProgressActivity.class);
                toSendIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                startActivity(toSendIntent);
                try {
                    LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(progressReceiver);
                }catch (Exception ignored){}
                finish();
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, progressReceiverIF);

        disable = findViewById(R.id.disable);
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableAlert();
            }
        });

        close = findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        sendBroadcast(new Intent("requestProgress"));
    }

    @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }catch (Exception ignored){}
        super.onDestroy();
    }

    void disableAlert(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure)
                .setMessage(R.string.howToRestore)
                .setPositiveButton(R.string.goAhead, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            uninstall();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        finish();
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show();
    }

    void disableApp(){

        ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).cancelAll();

        SharedPreferences.Editor editor = getSharedPreferences("main", MODE_PRIVATE).edit();
        editor.putBoolean("isDisabled", true);
        editor.commit();

        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    void uninstall() throws IOException, InterruptedException {

        String sourceDir = getApplicationInfo().sourceDir;

        if (sourceDir.startsWith("/system")) {

            disableApp();

            File tempScript = new File(getFilesDir() + "/tempScript.sh");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
            String command = "#!/sbin/sh\n\n" +
                    "mount -o rw,remount /system\n" +
                    "mount -o rw,remount /data\n" +
                    "mount -o rw,remount /system/app/MigrateHelper\n" +
                    "mount -o rw,remount /data/data/balti.migratehelper\n" +
                    "rm -rf " + getApplicationInfo().dataDir + " " + sourceDir + " " + TEMP_DIR_NAME + "\n" +
                    "mount -o ro,remount /system\n";
            writer.write(command);
            writer.close();

            stopService(new Intent(this, StupidStartupService.class));
            Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath()).waitFor();
        }
        else {
            Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + getPackageName()));
            startActivity(uninstallIntent);
        }

    }
}
