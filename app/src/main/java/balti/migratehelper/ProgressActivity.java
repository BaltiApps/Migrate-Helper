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

import java.util.ArrayList;

import static balti.migratehelper.CommonTools.ACTION_END_ALL;
import static balti.migratehelper.CommonTools.CANCEL_RESTORE_INTENT_FILTER;

public class ProgressActivity extends AppCompatActivity {

    Button okOnFinish, close, reportLog, cancelButton;
    ProgressBar progressBar;
    TextView messageView, messageHead, progressPercentage;
    TextView errorView;
    ImageView iconHolder;

    BroadcastReceiver progressReceiver, endOnDisable;
    IntentFilter progressReceiverIF;

    String type;
    boolean wasContactBeingRestored = false;

    int dpiValue = 0;

    String lastMsg = "";
    String lastIcon = "";

    SetAppIcon setAppIcon;
    CommonTools commonTools;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        iconHolder = findViewById(R.id.logoIcon);
        messageHead = findViewById(R.id.messageHead);
        messageView = findViewById(R.id.messageView);
        errorView = findViewById(R.id.errorLogTextView);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);
        okOnFinish = findViewById(R.id.okOnFinish);
        close = findViewById(R.id.close);
        reportLog = findViewById(R.id.reportLogButton);
        cancelButton = findViewById(R.id.cancelRestore);

        reportLog.setVisibility(View.GONE);

        messageView.setGravity(Gravity.BOTTOM);
        messageView.setMovementMethod(new ScrollingMovementMethod());

        type = "";

        commonTools = new CommonTools(this);

        if (getIntent().getExtras() != null) {
            handleProgress(getIntent());
        }

        reportLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                commonTools.reportLogs(true);

            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleProgress(intent);
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, progressReceiverIF);


        endOnDisable = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(endOnDisable, new IntentFilter(ACTION_END_ALL));

        okOnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProgressActivity.this, UninstallActivity.class).putExtra("dpiValue", dpiValue));
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalBroadcastManager.getInstance(ProgressActivity.this).sendBroadcast(new Intent(CANCEL_RESTORE_INTENT_FILTER));
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        trySettingAppIcon();
    }

    void handleProgress(Intent intent) {

        messageHead.setTextColor(getResources().getColor(R.color.colorAccent));
        try {
            type = intent.getStringExtra("type");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (wasContactBeingRestored && !type.equals("waiting_for_contacts")) {
            messageView.setText("");
            wasContactBeingRestored = false;
        }

        if (type.equals("finishedErrors")) {

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            reportLog.setVisibility(View.VISIBLE);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored) {
            }

            iconHolder.setImageResource(R.drawable.ic_error);
            messageHead.setText(intent.getStringExtra("head"));

            if (intent.hasExtra("total_time"))
                messageHead.append("\n(" + intent.getStringExtra("total_time") + ")");

            appendLog("log", intent);

            messageHead.setTextColor(Color.RED);

            if (intent.hasExtra("errors")) {
                ArrayList<String> receivedErrors = intent.getStringArrayListExtra("errors");
                for (String err : receivedErrors) {
                    errorView.append(err + "\n");
                }
            }

            dpiValue = intent.getIntExtra("dpiValue", 0);

            cancelButton.setVisibility(View.GONE);
            okOnFinish.setText(R.string.finish);
            close.setVisibility(View.GONE);
            okOnFinish.setVisibility(View.VISIBLE);

        } else if (type.equals("restoreCancelled")) {

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            reportLog.setVisibility(View.VISIBLE);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored) {
            }

            iconHolder.setImageResource(R.drawable.ic_cancelled);
            messageHead.setText(intent.getStringExtra("head"));

            if (intent.hasExtra("total_time"))
                messageHead.append("\n(" + intent.getStringExtra("total_time") + ")");

            appendLog("log", intent);

            messageHead.setTextColor(Color.RED);

            if (intent.hasExtra("errors")) {
                ArrayList<String> receivedErrors = intent.getStringArrayListExtra("errors");
                for (String err : receivedErrors) {
                    errorView.append(err + "\n");
                }
            }

            dpiValue = intent.getIntExtra("dpiValue", 0);

            cancelButton.setVisibility(View.GONE);
            okOnFinish.setText(R.string.finish);
            close.setVisibility(View.GONE);
            okOnFinish.setVisibility(View.VISIBLE);

        } else if (type.equals("finishedOk")) {

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            reportLog.setVisibility(View.GONE);

            try {
                setAppIcon.cancel(true);
            } catch (Exception ignored) {
            }

            iconHolder.setImageResource(R.drawable.ic_finished);
            messageHead.setText(intent.getStringExtra("head"));

            if (intent.hasExtra("total_time"))
                messageHead.append("\n(" + intent.getStringExtra("total_time") + ")");

            appendLog("log", intent);

            dpiValue = intent.getIntExtra("dpiValue", 0);

            cancelButton.setVisibility(View.GONE);
            okOnFinish.setText(R.string.finish);
            close.setVisibility(View.GONE);
            updateProgress(progressBar.getMax());
            okOnFinish.setVisibility(View.VISIBLE);

        } else if (type.equals("restoring_app")) {

            String head = intent.getStringExtra("head");
            messageHead.setText(head);
            progressBar.setMax(intent.getIntExtra("n", 0));
            updateProgress(intent.getIntExtra("p", 0));

            appendLog("log", intent);

            trySettingAppIcon();

        } else if (type.equals("waiting_for_contacts")) {

            wasContactBeingRestored = true;

            messageHead.setText(R.string.waiting_for_contacts);
            messageView.setText(R.string.waiting_for_contacts_desc);

            progressBar.setIndeterminate(true);

            iconHolder.setImageResource(R.drawable.ic_waiting);

        }
    }

    void appendLog(String key, Intent intent) {

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

    void updateProgress(int c) {
        int n = progressBar.getMax();
        progressBar.setIndeterminate(false);
        if (n != 0)
            progressPercentage.setText((int) ((c * 100.0) / n) + "%");
        else progressPercentage.setText("");
        progressBar.setProgress(c);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(endOnDisable);
        } catch (Exception ignored) {
        }
    }

    void trySettingAppIcon() {

        try {

            if (RootRestoreTask.ICON_STRING != null && !lastIcon.equals(RootRestoreTask.ICON_STRING)) {

                try {
                    setAppIcon.cancel(true);
                } catch (Exception ignored) {
                }

                setAppIcon = new SetAppIcon(iconHolder);
                lastIcon = RootRestoreTask.ICON_STRING;
                setAppIcon.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, lastIcon);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


