package balti.migratehelper;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button rootRestoreButton, okOnFinish, disable;
    ProgressBar progressBar;
    TextView messageView;

    LinearLayout buttons;

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

        rootRestoreButton = (Button) findViewById(R.id.restoreSu);
        rootRestoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        messageView = (TextView) findViewById(R.id.message);
        buttons = (LinearLayout) findViewById(R.id.buttons);

        okOnFinish = (Button) findViewById(R.id.okOnFinish);
        okOnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttons.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                messageView.setVisibility(View.GONE);
                okOnFinish.setVisibility(View.GONE);
            }
        });

        disable = (Button) findViewById(R.id.disable);
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableAlert();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("job").equals("rootOnProgress")){
                    buttons.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    messageView.setVisibility(View.GONE);
                    okOnFinish.setVisibility(View.GONE);
                    progressBar.setProgress(intent.getIntExtra("c", 0)*100/intent.getIntExtra("n", 1));
                }
                else if (intent.getStringExtra("job").equals("finished")){
                    onFinish(intent);
                }
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        registerReceiver(progressReceiver, progressReceiverIF);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }

    void onFinish(Intent intent){
        buttons.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        messageView.setVisibility(View.VISIBLE);
        okOnFinish.setVisibility(View.VISIBLE);
        messageView.setText(intent.getStringExtra("messages"));
        if (intent.getBooleanExtra("wasError", false)){
            messageView.setTextColor(Color.RED);
        }
        else {
            messageView.setTextColor(Color.BLUE);
        }
        stopService(new Intent(this, RestoreService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(this, RestoreService.class));
                }
                else {
                    startService(new Intent(this, RestoreService.class));
                }
            }
            else {
                Toast.makeText(this, getString(R.string.permissionDenied), Toast.LENGTH_SHORT).show();
            }

    }

    void disableAlert(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure)
                .setMessage(R.string.howToRestore)
                .setPositiveButton(R.string.goAhead, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putBoolean("isDisabled", true);
                        editor.commit();
                        try {
                            uninstall();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        disableApp();
                        finish();
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show();
    }

    void disableApp(){
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    void uninstall() throws IOException {
        File tempScript = new File(getFilesDir() + "/tempScript.sh");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
        String command = "#!/sbin/sh\n\n" +
                "mount -o rw,remount /system/app/MigrateHelper\n" +
                "mount -o rw,remount /data/data/balti.migratehelper\n" +
                "rm -rf /system/app/MigrateHelper /data/data/balti.migratehelper\n";
        writer.write(command);
        writer.close();

        stopService(new Intent(this, StupidStartupService.class));
        Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath());
    }

    /*void uninstall(){
        Uri packageUri = Uri.parse("package:" + getPackageName());
        Intent unistallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
        startActivity(unistallIntent);
    }*/
}
