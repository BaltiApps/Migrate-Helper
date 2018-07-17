package balti.migratehelper;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

/**
 * Created by sayantan on 27/10/17.
 */

public class RestoreService extends Service {


    BroadcastReceiver progressReceiver, requestListener;
    IntentFilter progressReceiverIF, requestListenerIF;

    Intent returnIntent;

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
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        registerReceiver(progressReceiver, progressReceiverIF);

        requestListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (returnIntent != null) sendBroadcast(returnIntent);
            }
        };
        requestListenerIF = new IntentFilter("requestProgress");
        registerReceiver(requestListener, requestListenerIF);
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
        dummy.setSmallIcon(R.drawable.ic_fix);

        RootRestoreTask task = new RootRestoreTask(this);
        task.execute();
        startForeground(1, dummy.build());


        stopService(new Intent(this, StupidStartupService.class));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(requestListener);
        unregisterReceiver(progressReceiver);
    }
}
