package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Objects;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;
import static balti.migratehelper.Listener.PROGRESS_CHANNEL;

;

/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<File, Object, Integer> {

    private Context context;
    private String errors;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder progress;

    private Intent restoreIntent;
    private Intent activityIntent;

    int numberOfAppJobs;

    private int SUCCESS = 0;
    private int CODE_ERROR = 990;
    private int EXECUTION_ERROR = 999;

    private long startMillis;
    private long endMillis;
    private String installScriptPath, restoreDataScriptPath;

    static String METADATA_FILE_FIELD = "metadata_file";
    static String METADATA_FILE_NAME = "metadata_file_name";

    static int ON_FINISH_NOTIFICATION_ID = 101;

    static String DISPLAY_HEAD = "display head: ";
    static String INSTALLING_HEAD = "Installing app: ";
    static String RESTORE_DATA_HEAD = "Restoring data: ";

    RootRestoreTask(Context context, long startMillis, String installScriptPath, String restoreDataScriptPath, int numberOfAppJobs) {
        this.context = context;
        this.startMillis = startMillis;
        this.installScriptPath = installScriptPath;
        this.restoreDataScriptPath = restoreDataScriptPath;
        this.numberOfAppJobs = numberOfAppJobs;
        errors = "";
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        activityIntent = new Intent(context, ProgressActivity.class);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @Override
    protected Integer doInBackground(File... files) {

        File restoreScript = files[0];

        try {
            Process restoreProcess = Runtime.getRuntime().exec("su -c sh " + restoreScript.getAbsolutePath());
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(restoreProcess.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(restoreProcess.getErrorStream()));

            String line;
            int c = -1;
            String head = "", icon = "";
            while ((line = outputReader.readLine()) != null) {
                if (line.startsWith(DISPLAY_HEAD)) {
                    c++;
                    head = line.substring(DISPLAY_HEAD.length()).trim();
                    if (head.contains(" ")) {
                        icon = head.split(" ")[1];
                        head = head.split(" ")[0];
                    }
                }
                else {
                    publishProgress("restoring_app", c, numberOfAppJobs, head, line, icon);
                }
            }

            while ((line = errorReader.readLine()) != null) {
                String lowerLine = line.toLowerCase().trim();
                if (lowerLine.startsWith("selinux"))
                    continue;
                errors = errors + line + "\n";
            }

        }
        catch (Exception e){
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            return CODE_ERROR;
        }

        if (errors.equals(""))
            return SUCCESS;
        else return EXECUTION_ERROR;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel(PROGRESS_CHANNEL, PROGRESS_CHANNEL, NotificationManager.IMPORTANCE_LOW);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            progress = new NotificationCompat.Builder(context, PROGRESS_CHANNEL);
        else progress = new NotificationCompat.Builder(context);

        progress.setSmallIcon(R.drawable.ic_notification_icon);
        progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        notificationManager.cancel(ON_FINISH_NOTIFICATION_ID);

    }


    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);

        int p, max;
        String head;

        restoreIntent.putExtra("type", (String) values[0]);
        restoreIntent.putExtra("p", p = (int) values[1]);
        restoreIntent.putExtra("n", max = (int) values[2]);
        restoreIntent.putExtra("head", head = (String) values[3]);
        restoreIntent.putExtra("log", (String) values[4]);
        restoreIntent.putExtra("icon", (String) values[5]);

        activityIntent.putExtras(Objects.requireNonNull(restoreIntent.getExtras()));

        LocalBroadcastManager.getInstance(context).sendBroadcast(restoreIntent);

        progress.setProgress(max, p, false)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(head);
        notificationManager.notify(RestoreService.RESTORE_SERVICE_NOTIFICATION_ID, progress.build());

    }

    @Override
    protected void onPostExecute(Integer o) {

        endMillis = timeInMillis();
        String totalTime = calendarDifference(startMillis, endMillis);

        super.onPostExecute(o);

        try {
            Runtime.getRuntime().exec("su -c rm -rf " + TEMP_DIR_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ( o == SUCCESS) {

            Toast.makeText(context, context.getString(R.string.finished), Toast.LENGTH_SHORT).show();

            restoreIntent.putExtra("type", "finishedOk");
            restoreIntent.putExtra("log", context.getString(R.string.uninstall_prompt));
            restoreIntent.putExtra("head", context.getString(R.string.finished) );
            restoreIntent.putExtra("total_time", totalTime);

            activityIntent.putExtras(restoreIntent);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.finished))
                    .setProgress(0, 0, false);
        }
        else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();

            restoreIntent.putExtra("type", "finishedErrors");
            restoreIntent.putExtra("log", errors + "\n" + context.getString(R.string.failed) + " " + o);
            restoreIntent.putExtra("head", context.getString(R.string.finished_with_errors));
            restoreIntent.putExtra("total_time", totalTime);

            activityIntent.putExtras(restoreIntent);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.finished_with_errors))
                    .setContentText(errors)
                    .setProgress(0, 0, false);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(restoreIntent);

        notificationManager.notify(ON_FINISH_NOTIFICATION_ID, progress.build());
    }

    /*String getNextCommand(Vector<JSONObject> jsonObjects) throws JSONException {

        JSONObject jsonObject = jsonObjects.get(totalCount);
        String appName = jsonObject.getString("app_name");
        String packageName = jsonObject.getString("package_name");
        String apkName = jsonObject.getString("apk");
        String icon = jsonObject.getString("icon");

        Log.d("migrate_helper", jsonObject.getString("app_name") + " " + jsonObject.getBoolean(APP_CHECK) + " " + jsonObject.getBoolean(DATA_CHECK));

        String command = "echo \"" + statusHead + appName + " " + icon + "\"\n";

        boolean appCheck = jsonObject.getBoolean(APP_CHECK);
        if (!apkName.equals("NULL") && appCheck)
            command += "sh " + installScriptPath + " " + TEMP_DIR_NAME + " " + apkName + "\n";

        command += "echo \"" + installedStatusHead + packageName + "\"\n";

        return command;
    }*/

    long timeInMillis(){
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    String calendarDifference(long start, long end){
        String diff = "";

        try {

            long longDiff = end - start;
            longDiff = longDiff / 1000;

            long d = longDiff / (60 * 60 * 24);
            if (d != 0) diff = diff + d + "days ";
            longDiff = longDiff % (60 * 60 * 24);

            long h = longDiff / (60 * 60);
            if (h != 0) diff = diff + h + "hrs ";
            longDiff = longDiff % (60 * 60);

            long m = longDiff / 60;
            if (m != 0) diff = diff + m + "mins ";
            longDiff = longDiff % 60;

            long s = longDiff;
            diff = diff + s + "secs";

        }
        catch (Exception ignored){}

        return diff;
    }

}
