package balti.migratehelper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button rootRestoreButton, disable;

    ImageButton close;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.too_fast)
                    .setMessage(R.string.too_fast_desc)
                    .setPositiveButton(android.R.string.ok, null)
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

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(new Intent(MainActivity.this, RestoreService.class));
                                }
                                else {
                                    startService(new Intent(MainActivity.this, RestoreService.class));
                                }

                            }
                        })
                        .setNegativeButton(R.string.later, null)
                        .show();
            }
        });

        disable = findViewById(R.id.disable);
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableAlert();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent toSendIntent = new Intent(MainActivity.this, ProgressActivity.class);
                toSendIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                startActivity(toSendIntent);
                finish();
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        registerReceiver(progressReceiver, progressReceiverIF);

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
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }

    void disableAlert(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure)
                .setMessage(R.string.howToRestore)
                .setPositiveButton(R.string.goAhead, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disableApp();
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

        SharedPreferences.Editor editor = getSharedPreferences("main", MODE_PRIVATE).edit();
        editor.putBoolean("isDisabled", true);
        editor.commit();

        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    void uninstall() throws IOException, InterruptedException {
        File tempScript = new File(getFilesDir() + "/tempScript.sh");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
        String command = "#!/sbin/sh\n\n" +
                "mount -o rw,remount /system\n" +
                "mount -o rw,remount /data\n" +
                "mount -o rw,remount /system/app/MigrateHelper\n" +
                "mount -o rw,remount /data/data/balti.migratehelper\n" +
                "rm -rf /system/app/MigrateHelper /data/data/balti.migratehelper\n" +
                "mount -o ro,remount /system\n";
        writer.write(command);
        writer.close();

        stopService(new Intent(this, StupidStartupService.class));
        Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath()).waitFor();

    }
}
