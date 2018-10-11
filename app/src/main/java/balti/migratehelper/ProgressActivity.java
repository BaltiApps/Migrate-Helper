package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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

public class ProgressActivity extends AppCompatActivity {

    Button okOnFinish, close;
    ProgressBar progressBar;
    TextView messageView, messageHead, progressPercentage;
    TextView errorView;
    ImageView icon;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    String type;
    boolean wasContactBeingRestored = false;

    int dpiValue = 0;

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
        errorView = findViewById(R.id.errorLogTextView);
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
                startService(new Intent(ProgressActivity.this, UninstallService.class).putExtra("dpiValue", dpiValue));
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

            if (intent.hasExtra("errors"))
                errorView.setText(intent.getStringExtra("errors"));

            dpiValue = intent.getIntExtra("dpiValue", 0);

            okOnFinish.setText(R.string.uninstall);
            okOnFinish.setVisibility(View.VISIBLE);
            close.setVisibility(View.GONE);
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

            dpiValue = intent.getIntExtra("dpiValue", 0);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }
}


