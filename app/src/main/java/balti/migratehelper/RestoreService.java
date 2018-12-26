package balti.migratehelper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static balti.migratehelper.CommonTools.METADATA_HOLDER_DIR;
import static balti.migratehelper.Listener.PROGRESS_CHANNEL;

/**
 * Created by sayantan on 27/10/17.
 */

public class RestoreService extends Service {


    static RootRestoreTask ROOT_RESTORE_TASK;
    static int RESTORE_SERVICE_NOTIFICATION_ID = 100;
    BroadcastReceiver progressReceiver, requestListener;
    IntentFilter progressReceiverIF, requestListenerIF;
    Intent returnIntent;
    BufferedWriter progressWriter, errorWriter;
    String lastProgressLog = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {

            progressWriter = new BufferedWriter(new FileWriter(new File(METADATA_HOLDER_DIR, "progressLog.txt")));
            errorWriter = new BufferedWriter(new FileWriter(new File(METADATA_HOLDER_DIR, "errorLog.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                returnIntent = intent;
                if (intent.hasExtra("type")) {

                    String type = intent.getStringExtra("type");

                    if (type.equals("finishedErrors") || type.equals("restoreCancelled")) {

                        try {

                            if (intent.hasExtra("errors")) {
                                ArrayList<String> receivedErrors = intent.getStringArrayListExtra("errors");
                                for (String err : receivedErrors) {
                                    errorWriter.write(err + "\n");
                                }
                            }


                            if (intent.hasExtra("log"))
                                progressWriter.write("\n\n" + intent.getStringExtra("log") + "\n");

                            progressWriter.close();
                            errorWriter.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        stopSelf();

                    } else if (type.equals("finishedOk")) {

                        try {

                            if (intent.hasExtra("log"))
                                progressWriter.write("\n\n" + intent.getStringExtra("log") + "\n");

                            progressWriter.close();
                            errorWriter.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        stopSelf();

                    } else if (type.equals("restoring_app") && intent.hasExtra("log")
                            && !intent.getStringExtra("log").equals(lastProgressLog)) {

                        try {
                            progressWriter.write((lastProgressLog = intent.getStringExtra("log")) + "\n");
                        } catch (IOException ignored) {
                        }

                    }

                }
            }
        };


        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, progressReceiverIF);

        requestListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (returnIntent != null) sendBroadcast(returnIntent);
            }
        };
        requestListenerIF = new IntentFilter("requestProgress");
        LocalBroadcastManager.getInstance(this).registerReceiver(requestListener, requestListenerIF);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationCompat.Builder dummy;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dummy = new NotificationCompat.Builder(this, PROGRESS_CHANNEL);
        } else {
            dummy = new NotificationCompat.Builder(this);
        }
        dummy.setSmallIcon(R.drawable.ic_notification_icon);

        startForeground(RESTORE_SERVICE_NOTIFICATION_ID, dummy.build());


        stopService(new Intent(this, StupidStartupService.class));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(requestListener);
        } catch (Exception ignored) {
        }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        } catch (Exception ignored) {
        }

        try {
            progressWriter.close();
        } catch (Exception ignored) {
        }
        try {
            errorWriter.close();
        } catch (Exception ignored) {
        }
    }
}
