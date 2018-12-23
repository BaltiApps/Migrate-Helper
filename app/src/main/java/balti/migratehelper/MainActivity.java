package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Objects;

import static balti.migratehelper.CommonTools.ACTION_END_ALL;
import static balti.migratehelper.CommonTools.METADATA_HOLDER_DIR;

public class MainActivity extends AppCompatActivity {

    Button rootRestoreButton, disable, selectiveRestore, temporaryDisable;
    TextView lastLogs;
    ImageButton close;

    BroadcastReceiver progressReceiver, endOnDisable;
    IntentFilter progressReceiverIF;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    CommonTools commonTools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        commonTools = new CommonTools(this);

        getExternalCacheDir();

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
                        .setTitle(R.string.turn_off_internet_and_updates)
                        .setMessage(R.string.do_not_use_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(MainActivity.this, AppSelector.class). putExtra("all?", true));
                            }
                        })
                        .setCancelable(false)
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

        disable = findViewById(R.id.disable);
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                directUninstall();
            }
        });

        temporaryDisable = findViewById(R.id.temporary_disable);
        temporaryDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.temporary_disable_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                stopService(new Intent(MainActivity.this, StupidStartupService.class));
                                editor.putBoolean("temporaryDisable", true);
                                editor.commit();
                                sendBroadcast(new Intent(ACTION_END_ALL));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

            }
        });

        lastLogs = findViewById(R.id.last_logs_textView);
        lastLogs.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        lastLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog();
            }
        });

        close = findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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

        endOnDisable = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(endOnDisable, new IntentFilter(ACTION_END_ALL));

        sendBroadcast(new Intent("requestProgress"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        final String cpu_abi = Build.SUPPORTED_ABIS[0];

        if (!(cpu_abi.equals("armeabi-v7a") || cpu_abi.equals("arm64-v8a") || cpu_abi.equals("x86") || cpu_abi.equals("x86_64"))){


            new AlertDialog.Builder(this)
                    .setTitle(R.string.unsupported_device)
                    .setMessage(getString(R.string.cpu_arch_is) + "\n" + cpu_abi + "\n\n" + getString(R.string.currently_supported_cpu))
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.contact, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String body = "";

                            body = body + "CPU_ABI: " + cpu_abi + "\n\n";
                            body = body + "Brand: " + Build.BRAND + "\n";
                            body = body + "Manufacturer: " + Build.MANUFACTURER + "\n";
                            body = body + "Model: " + Build.MODEL + "\n";
                            body = body + "Device: " + Build.DEVICE + "\n";
                            body = body + "SDK: " + Build.VERSION.SDK_INT + "\n";
                            body = body + "Board: " + Build.BOARD + "\n";
                            body = body + "Hardware: " + Build.HARDWARE;

                            Intent email = new Intent(Intent.ACTION_SENDTO);
                            email.setData(Uri.parse("mailto:"));
                            email.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                            email.putExtra(Intent.EXTRA_SUBJECT, "Unsupported device");
                            email.putExtra(Intent.EXTRA_TEXT, body);

                            try {
                                startActivity(Intent.createChooser(email, getString(R.string.select_mail)));
                            }
                            catch (Exception e)
                            {
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }catch (Exception ignored){}
        try {
            unregisterReceiver(endOnDisable);
        }catch (Exception ignored){}
        super.onDestroy();
    }

    void directUninstall(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.sure)
                .setMessage(R.string.howToRestore)
                .setPositiveButton(R.string.goAhead, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent uninstallIntent = new Intent(MainActivity.this, UninstallService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(uninstallIntent);
                        }
                        else {
                            startService(uninstallIntent);
                        }
                        finishAffinity();
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null)
                .show();
    }

    void showLog(){
        View lView = View.inflate(this, R.layout.last_log_report, null);
        Button pLog = lView.findViewById(R.id.view_progress_log);
        Button eLog = lView.findViewById(R.id.view_error_log);
        Button report = lView.findViewById(R.id.report_logs);

        final AlertDialog ad = new AlertDialog.Builder(this, R.style.DarkAlert)
                .setTitle(R.string.lastLog)
                .setIcon(R.drawable.ic_log)
                .setView(lView)
                .setNegativeButton(R.string.close, null)
                .create();

        pLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(METADATA_HOLDER_DIR, "progressLog.txt");
                if (f.exists())
                    startActivity(
                            new Intent(MainActivity.this, SimpleLogDisplay.class)
                                    .putExtra("head", getString(R.string.progressLog))
                                    .putExtra("filePath", f.getAbsolutePath())
                    );
                else Toast.makeText(MainActivity.this, getString(R.string.progress_log_does_not_exist), Toast.LENGTH_SHORT).show();
            }
        });

        eLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(METADATA_HOLDER_DIR, "errorLog.txt");
                if (f.exists())
                    startActivity(
                            new Intent(MainActivity.this, SimpleLogDisplay.class)
                                    .putExtra("head", getString(R.string.errorLog))
                                    .putExtra("filePath", f.getAbsolutePath())
                    );
                else Toast.makeText(MainActivity.this, getString(R.string.error_log_does_not_exist), Toast.LENGTH_SHORT).show();
            }
        });

        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commonTools.reportLogs(false);
                ad.dismiss();
            }
        });

        ad.show();
    }

}
