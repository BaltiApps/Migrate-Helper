package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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


    class SetAppIcon extends AsyncTask<String, Void, Drawable> {


        @Override
        protected Drawable doInBackground(String... strings) {

            Bitmap bmp = null;
            Drawable drawable = null;
            String[] bytes = strings[0].split("_");

            try {
                byte imageData[] = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    imageData[i] = Byte.parseByte(bytes[i]);
                }
                bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                drawable = new BitmapDrawable(getResources(), bmp);
                //Log.d("migrate", "icon: " + bmp);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            if (drawable != null) {
                messageHead.setCompoundDrawables(drawable, null, null, null);
            }
            else {
                messageHead.setCompoundDrawables(null, null, null, null);
            }
        }
    }

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
            messageHead.setText(action);
            icon.setImageDrawable(getDrawable(R.drawable.ic_error));
            messageView.append("\n\n" + intent.getStringExtra("message"));
            messageHead.setTextColor(Color.RED);
            close.setVisibility(View.VISIBLE);
        }
        else if (action.startsWith(getString(R.string.finished))){
            messageHead.setText(action);
            icon.setImageDrawable(getDrawable(R.drawable.ic_finished));
            messageView.append("\n\n" + intent.getStringExtra("message"));
            okOnFinish.setVisibility(View.VISIBLE);
        }
        else {
            String msg = intent.getStringExtra("message");
            if (!action.equals(UNCHANGED_STATUS)) {
                String d[] = action.split(" ");
                String status = d[0];
                String icon = d[1];
                messageHead.setText(status);
                new SetAppIcon().execute(icon.trim());
                progressBar.setMax(intent.getIntExtra("n", 0));
                updateProgress(intent.getIntExtra("c", 0));
            }
            if (!msg.equals(""))
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
                "rm -rf " + getApplicationInfo().sourceDir + " " + getApplicationInfo().dataDir + "\n" +
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


