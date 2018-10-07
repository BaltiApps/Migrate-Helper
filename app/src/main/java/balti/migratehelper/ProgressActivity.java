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

    String type;
    boolean wasContactBeingRestored = false;

    String lastMsg = "";

    SetAppIcon setAppIcon;

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

        type = "";

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
        try {
            type = intent.getStringExtra("type");
        }
        catch (Exception e){ e.printStackTrace(); }

        if (wasContactBeingRestored && !type.equals("waiting_for_contacts")) {
            messageView.setText("");
            wasContactBeingRestored = false;
        }

        if (type.equals("finishedErrors")){

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored){}

            icon.setImageResource(R.drawable.ic_error);
            messageHead.setText(intent.getStringExtra("head"));

            if (intent.hasExtra("total_time"))
                messageHead.append("\n(" + intent.getStringExtra("total_time") + ")");

            messageHead.setTextColor(Color.RED);

            appendLog("log", intent);

            close.setText(R.string.close);
        }
        else if (type.equals("finishedOk")){

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored){}

            icon.setImageResource(R.drawable.ic_finished);
            messageHead.setText(intent.getStringExtra("head"));

            if (intent.hasExtra("total_time"))
                messageHead.append("\n(" + intent.getStringExtra("total_time") + ")");

            appendLog("log", intent);

            okOnFinish.setVisibility(View.VISIBLE);
            close.setVisibility(View.GONE);

            updateProgress(progressBar.getMax());
        }
        else if (type.equals("restoring_app")) {

            String head = intent.getStringExtra("head");
            String icon = intent.getStringExtra("icon");
            messageHead.setText(head);
            progressBar.setMax(intent.getIntExtra("n", 0));
            updateProgress(intent.getIntExtra("p", 0));

            appendLog("log", intent);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored){}
            setAppIcon = new SetAppIcon(this.icon);
            setAppIcon.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, icon.trim());
        }
        else if (type.equals("waiting_for_contacts")) {

            wasContactBeingRestored = true;

            messageHead.setText(R.string.waiting_for_contacts);
            messageView.setText(R.string.waiting_for_contacts_desc);

            progressBar.setIndeterminate(true);

            icon.setImageResource(R.drawable.ic_waiting);

        }
    }

    void appendLog(String key, Intent intent){

        if (intent.hasExtra(key)) {
            String msg = intent.getStringExtra(key);
            if (!lastMsg.equals(msg)) {
                lastMsg = msg;
                if (lastMsg.equals(getString(R.string.uninstall_prompt)))
                    messageView.append("\n\n" + lastMsg + "\n");
                else messageView.append(lastMsg + "\n");
            }
        }
    }

    void updateProgress(int c){
        int n = progressBar.getMax();
        progressBar.setIndeterminate(false);
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


