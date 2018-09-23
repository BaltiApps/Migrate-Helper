package balti.migratehelper;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;

public class ProgressActivity extends AppCompatActivity {

    Button okOnFinish, close;
    ProgressBar progressBar;
    TextView messageView, messageHead, progressPercentage;
    ImageView icon;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    String action;

    String lastMsg = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        icon = findViewById(R.id.logoIcon);
        messageHead = findViewById(R.id.messageHead);
        messageView = findViewById(R.id.messageView);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);
        okOnFinish = findViewById(R.id.okOnFinish);
        close = findViewById(R.id.close);

        messageView.setGravity(Gravity.BOTTOM);
        messageView.setMovementMethod(new ScrollingMovementMethod());

        action = "";

        if (getIntent().getExtras() != null){
            handleProgress(getIntent());
        }

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleProgress(intent);
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, progressReceiverIF);

        okOnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    uninstall();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    void handleProgress(Intent intent){

        messageHead.setTextColor(getResources().getColor(R.color.colorAccent));
        String msg = "";
        try {
            msg = intent.getStringExtra("message");
            action = intent.getStringExtra("job");
        }
        catch (Exception e){ e.printStackTrace(); }

        if (action.startsWith(getString(R.string.finished_with_errors))){

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            icon.setImageResource(R.drawable.ic_error);
            messageHead.setText(action);
            if (!lastMsg.equals(msg)) {
                lastMsg = msg;
                messageView.append("\n\n" + lastMsg);
            }
            messageHead.setTextColor(Color.RED);
            close.setVisibility(View.VISIBLE);
        }
        else if (action.startsWith(getString(R.string.finished))){

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            icon.setImageResource(R.drawable.ic_finished);
            messageHead.setText(action);
            if (!lastMsg.equals(msg)) {
                lastMsg = msg;
                messageView.append("\n\n" + lastMsg);
            }
            okOnFinish.setVisibility(View.VISIBLE);
        }
        else {
            String d[] = action.split(" ");
            String status = d[0];
            String icon = d[1];
            messageHead.setText(status);
            new SetAppIcon(this.icon).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, icon.trim());
            progressBar.setMax(intent.getIntExtra("n", 0));
            updateProgress(intent.getIntExtra("c", 0));

            if (!msg.equals("") && !lastMsg.equals(msg)) {
                lastMsg = msg;
                messageView.append(msg + "\n");
            }
        }
    }

    void updateProgress(int c){
        int n = progressBar.getMax();
        if (n != 0)
            progressPercentage.setText((int)((c*100.0)/n) + "%");
        else progressPercentage.setText("");
        progressBar.setProgress(c);
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

    void disableApp(){

        ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).cancelAll();

        SharedPreferences.Editor editor = getSharedPreferences("main", MODE_PRIVATE).edit();
        editor.putBoolean("isDisabled", true);
        editor.commit();

        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, balti.migratehelper.MainActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }
}


