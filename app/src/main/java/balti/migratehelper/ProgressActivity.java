package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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
    TextView messageView, messageHead;
    ImageView icon;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    String action;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);

        icon = findViewById(R.id.logoIcon);
        messageHead = findViewById(R.id.messageHead);
        messageView = findViewById(R.id.messageView);
        progressBar = findViewById(R.id.progressBar);
        okOnFinish = findViewById(R.id.okOnFinish);
        close = findViewById(R.id.close);

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
                uninstallThisApp();
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

        messageHead.setText(intent.getStringExtra("job"));
        messageHead.setTextColor(getResources().getColor(R.color.colorPrimary));

        if (!intent.getStringExtra("job").equals(action)) {
            action = intent.getStringExtra("job");
            messageView.setText("");
            progressBar.setMax(intent.getIntExtra("n", 0));
        }

        if (intent.getStringExtra("job").equals(getString(R.string.requesting_root))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_requesting_root));
        }
        else if (action.equals(getString(R.string.finished_with_errors))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_error));
            messageView.setText(intent.getStringExtra("message"));
            messageHead.setTextColor(Color.RED);
            close.setVisibility(View.VISIBLE);
        }
        else if (action.equals(getString(R.string.finished))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_finished));
            messageView.setText("");
            messageView.setText(intent.getStringExtra("message"));
            okOnFinish.setVisibility(View.VISIBLE);
        }
        else if (action.equals(getString(R.string.installing_apps))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_installing_apps));
            progressBar.setProgress(intent.getIntExtra("c", 0));
            messageView.append(intent.getStringExtra("message") + "\n");
        }
        else if (action.equals(getString(R.string.restoring_data))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_restoring_data));
            progressBar.setProgress(intent.getIntExtra("c", 0));
            messageView.append(intent.getStringExtra("message") + "\n");
        }
        else if (action.equals(getString(R.string.fixing_perm))){
            icon.setImageDrawable(getDrawable(R.drawable.ic_fixing_permissions));
            progressBar.setProgress(intent.getIntExtra("c", 0));
            messageView.append(intent.getStringExtra("message") + "\n");
        }
    }

    void uninstallThisApp(){

        try {

            File tempScript = new File(getFilesDir() + "/tempScript.sh");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
            String command = "#!sbin/sh\n\n" +
                    "mount -o rw,remount /system\n" +
                    "rm -rf /system/app/MigrateHelper /data/data/balti.migratehelper\n";
            writer.write(command);
            writer.close();
            Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }
}


