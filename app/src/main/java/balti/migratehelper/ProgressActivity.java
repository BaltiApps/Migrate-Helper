package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProgressActivity extends AppCompatActivity {

    Button okOnFinish, close;
    ProgressBar progressBar;
    TextView messageView, messageHead, progressPercentage;
    ImageView icon;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    String action;

    static String UNCHANGED_STATUS = "UNCHANGED_STATUS";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);

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
        registerReceiver(progressReceiver, progressReceiverIF);

        okOnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableApp();
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

        action = intent.getStringExtra("job");
        if (action.startsWith(getString(R.string.finished_with_errors))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_error));
            messageView.setText(intent.getStringExtra("message"));
            messageHead.setTextColor(Color.RED);
            close.setVisibility(View.VISIBLE);
        }
        else if (action.startsWith(getString(R.string.finished))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_finished));
            messageView.setText(intent.getStringExtra("message"));
            okOnFinish.setVisibility(View.VISIBLE);
        }
        else {
            if (!action.equals(UNCHANGED_STATUS)) {
                progressBar.setMax(intent.getIntExtra("n", 0));
                messageHead.setText(action);
                updateProgress(intent.getIntExtra("c", 0));
            }
            messageView.append(intent.getStringExtra("message") + "\n");
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

    void disableApp(){

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
        unregisterReceiver(progressReceiver);
    }
}


