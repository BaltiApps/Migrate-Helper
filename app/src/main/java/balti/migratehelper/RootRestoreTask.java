package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import static balti.migratehelper.CommonTools.CANCEL_RESTORE_INTENT_FILTER;
import static balti.migratehelper.Listener.PROGRESS_CHANNEL;

//import static balti.migratehelper.CommonTools.TEMP_DIR_NAME;


/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<File, Object, Integer> {

    static String METADATA_FILE_FIELD = "metadata_file";
    static String METADATA_FILE_NAME = "metadata_file_name";
    static int ON_FINISH_NOTIFICATION_ID = 101;
    static String DISPLAY_HEAD = "display head: ";
    static String INSTALLING_HEAD = "Installing app: ";
    static String RESTORE_DATA_HEAD = "Restoring data: ";
    static String ICON_STRING = "";
    static int RESTORE_PROCESS_ID = -9999999;
    int numberOfAppJobs;
    BroadcastReceiver cancelReceiver;
    private Context context;
    private ArrayList<String> errors;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder progress;
    private Intent restoreIntent;
    private Intent activityIntent;
    private int SUCCESS = 0;
    private int CODE_ERROR = 990;
    private int EXECUTION_ERROR = 999;
    private boolean isCancelled = false;
    private long startMillis;
    private long endMillis;
    private int dpiValue = 0;
    private boolean isContactAppPresent;
    private Process suProcessForVcfCheck;
    private BufferedWriter suProcessForVcfCheckWriter;
    private BufferedReader suProcessForVcfCheckReader;
    private File actualRestoreScript;
    private Process restoreProcess = null;

    RootRestoreTask(Context context, int numberOfAppJobs, boolean isContactAppPresent, int dpiValue, String actualRestoreScriptPath) {

        this.context = context;
        this.numberOfAppJobs = numberOfAppJobs;
        this.isContactAppPresent = isContactAppPresent;
        this.dpiValue = dpiValue;
        this.actualRestoreScript = new File(actualRestoreScriptPath);

        errors = new ArrayList<>(0);

        restoreIntent = new Intent(context.getString(R.string.actionRestoreOnProgress));
        activityIntent = new Intent(context, ProgressActivity.class);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            suProcessForVcfCheck = Runtime.getRuntime().exec("su");
            suProcessForVcfCheckWriter = new BufferedWriter(new OutputStreamWriter(suProcessForVcfCheck.getOutputStream()));
            suProcessForVcfCheckReader = new BufferedReader(new InputStreamReader(suProcessForVcfCheck.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        cancelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isCancelled = true;
                cancelProcess();
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(cancelReceiver, new IntentFilter(CANCEL_RESTORE_INTENT_FILTER));
    }


    boolean isVcfBeingRead() {

        if (suProcessForVcfCheck == null) return false;

        String command1 = "dumpsys activity services | grep com.google.android.contacts/com.google.android.apps.contacts.vcard.VCardService\n";
        String command2 = "dumpsys activity services | grep com.android.contacts/.vcard.VCardService\n";
        String command3 = "dumpsys activity services | grep com.android.contacts/.common.vcard.VCardService\n";

        try {
            suProcessForVcfCheckWriter.write(command1);
            suProcessForVcfCheckWriter.write(command2);
            suProcessForVcfCheckWriter.write(command3);
            suProcessForVcfCheckWriter.write("echo DONE\n");
            suProcessForVcfCheckWriter.flush();

            String output = "", line;
            while ((line = suProcessForVcfCheckReader.readLine()) != null) {
                output = output + line + "\n";
                if (line.trim().equals("DONE")) break;
            }

            return !output.trim().equals("DONE");


        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    protected Integer doInBackground(final File... files) {


        if (isContactAppPresent) {

            boolean isRunning = false;

            while (isVcfBeingRead()) {
                isRunning = true;
                publishProgress("waiting_for_contacts", 0, 0, context.getString(R.string.waiting_for_contacts), "", "");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (isRunning) {
                try {
                    suProcessForVcfCheckWriter.write("exit\n");
                    suProcessForVcfCheckWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        startMillis = timeInMillis();

        try {
            restoreProcess = Runtime.getRuntime().exec("su");

            BufferedWriter suRestoreProcessWriter = new BufferedWriter(new OutputStreamWriter(restoreProcess.getOutputStream()));
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(restoreProcess.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(restoreProcess.getErrorStream()));

            suRestoreProcessWriter.write("sh " + actualRestoreScript.getAbsolutePath() + "\n");
            suRestoreProcessWriter.write("exit\n");
            suRestoreProcessWriter.flush();

            String line;
            int c = -1;
            String head = "";

            while ((line = outputReader.readLine()) != null) {

                line = line.trim();

                if (line.startsWith(DISPLAY_HEAD)) {
                    c++;
                    head = line.substring(DISPLAY_HEAD.length()).trim();
                    if (head.contains(" ")) {
                        ICON_STRING = head.split(" ")[1].trim();
                        head = head.split(" ")[0];
                    }
                } else if (line.startsWith("--- RESTORE PID:")) {
                    try {
                        RESTORE_PROCESS_ID = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    publishProgress("restoring_app", c, numberOfAppJobs, head, line);
                    if (line.startsWith("Failed to find package"))
                        errors.add("restoreDataScript: " + line);
                }
            }

            while ((line = errorReader.readLine()) != null) {
                String lowerLine = line.toLowerCase().trim();
                if (lowerLine.startsWith("selinux") || (lowerLine.startsWith("pkg:") && lowerLine.split(" ").length == 2))
                    continue;
                errors.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
            errors.add(e.getMessage());
            return CODE_ERROR;
        }

        if (errors.size() == 0)
            return SUCCESS;
        else return EXECUTION_ERROR;


    }


    void cancelProcess() {
        try {
            Process killProcess = Runtime.getRuntime().exec("su");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(killProcess.getOutputStream()));
            writer.write("kill -9 " + RESTORE_PROCESS_ID + "\n");
            writer.write("kill -15 " + RESTORE_PROCESS_ID + "\n");
            writer.write("exit\n");
            writer.flush();

            killProcess.waitFor();

            try {
                restoreProcess.waitFor();
            } catch (Exception ignored) {
            }

        } catch (Exception ignored) {
        }
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

        activityIntent.putExtras(Objects.requireNonNull(restoreIntent.getExtras()));

        LocalBroadcastManager.getInstance(context).sendBroadcast(restoreIntent);

        progress.setProgress(max, p, max == 0)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(head);
        notificationManager.notify(RestoreService.RESTORE_SERVICE_NOTIFICATION_ID, progress.build());

    }

    @Override
    protected void onPostExecute(Integer o) {

        endMillis = timeInMillis();
        String totalTime = calendarDifference(startMillis, endMillis);

        super.onPostExecute(o);

        restoreIntent.putExtra("total_time", totalTime);
        restoreIntent.putExtra("dpiValue", dpiValue);

        // disabled from v2.0 to facilitate temporaryDisable
        /*try {
            Runtime.getRuntime().exec("su -c rm -rf " + TEMP_DIR_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        if (o == SUCCESS && !isCancelled) {

            Toast.makeText(context, context.getString(R.string.finished), Toast.LENGTH_SHORT).show();

            String log = (dpiValue > 0) ? context.getString(R.string.change_dpi_and_reboot_prompt) : context.getString(R.string.uninstall_prompt);

            restoreIntent.putExtra("type", "finishedOk");
            restoreIntent.putExtra("log", log);
            restoreIntent.putExtra("head", context.getString(R.string.finished));

            activityIntent.putExtras(restoreIntent);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.finished))
                    .setContentText(log)
                    .setProgress(0, 0, false);
        } else if (isCancelled) {

            Toast.makeText(context, context.getString(R.string.restoreCancelled), Toast.LENGTH_SHORT).show();

            String log = (dpiValue > 0) ? context.getString(R.string.change_dpi_and_reboot_prompt) : context.getString(R.string.uninstall_prompt);

            restoreIntent.putExtra("type", "restoreCancelled");
            restoreIntent.putExtra("log", context.getString(R.string.restoreCancelled) + "\n" + log);

            errors.add("\n" + context.getString(R.string.restoreCancelled));
            restoreIntent.putStringArrayListExtra("errors", errors);
            restoreIntent.putExtra("head", context.getString(R.string.restoreCancelled));

            activityIntent.putExtras(restoreIntent);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.restoreCancelled))
                    .setContentText(log)
                    .setProgress(0, 0, false);

        } else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();

            String log = (dpiValue > 0) ? context.getString(R.string.change_dpi_and_reboot_prompt) : context.getString(R.string.uninstall_prompt);

            restoreIntent.putExtra("type", "finishedErrors");
            restoreIntent.putExtra("log", log);

            errors.add("\n" + context.getString(R.string.failed) + " " + o);
            restoreIntent.putStringArrayListExtra("errors", errors);
            restoreIntent.putExtra("head", context.getString(R.string.finished_with_errors));

            activityIntent.putExtras(restoreIntent);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.finished_with_errors))
                    .setProgress(0, 0, false);

            if (errors.size() > 0)
                progress.setContentText(errors.get(0));
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(restoreIntent);

        progress.setOngoing(true);

        //String uninstallTitle = (dpiValue > 0)? context.getString(R.string.change_dpi_and_reboot) : context.getString(R.string.uninstall);
        PendingIntent uninstallPendingIntent = PendingIntent.getActivity(context, 79,
                new Intent(context, UninstallActivity.class).putExtra("dpiValue", dpiValue), 0);
        NotificationCompat.Action uninstallAction = new NotificationCompat.Action(0, context.getString(R.string.finish), uninstallPendingIntent);

        progress.addAction(uninstallAction);

        notificationManager.notify(ON_FINISH_NOTIFICATION_ID, progress.build());

        LocalBroadcastManager.getInstance(context).unregisterReceiver(cancelReceiver);
    }

    long timeInMillis() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    String calendarDifference(long start, long end) {
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

        } catch (Exception ignored) {
        }

        return diff;
    }

}
