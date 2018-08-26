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

/**
 * Created by sayantan on 27/10/17.
 */

public class RestoreService extends Service {


    BroadcastReceiver progressReceiver, requestListener;
    IntentFilter progressReceiverIF, requestListenerIF;

    Intent returnIntent;

    static RootRestoreTask ROOT_RESTORE_TASK;

    static int RESTORE_SERVICE_NOTIFICATION_ID = 100;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                returnIntent = intent;
                try {
                    if (intent.getStringExtra("job").startsWith(context.getString(R.string.finished)) || intent.getStringExtra("job").startsWith(context.getString(R.string.finished_with_errors))) {
                        stopSelf();
                    }
                } catch (Exception e){ e.printStackTrace();}
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
            dummy = new NotificationCompat.Builder(this, "PROGRESS");
        }
        else {
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(requestListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }
}
