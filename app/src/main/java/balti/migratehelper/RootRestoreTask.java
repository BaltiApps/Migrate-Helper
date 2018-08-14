package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Vector;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;
import static balti.migratehelper.GetJsonFromData.APP_CHECK;
import static balti.migratehelper.GetJsonFromData.DATA_CHECK;
import static balti.migratehelper.ProgressActivity.UNCHANGED_STATUS;

/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<Vector<JSONObject>, Object, Integer> {

    private Context context;
    private String errors;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder progress;

    private Intent restoreIntent;

    private int numberOfJobs;

    private int SUCCESS = 0;
    private int CODE_ERROR = 990;
    private int EXECUTION_ERROR = 999;

    private long startMillis;
    private long endMillis;
    private String installScriptPath, restoreDataScriptPath;

    static int totalCount = 0;
    private String statusHead = "helper status: ";
    private String installedStatusHead = "installed status: ";
    private String restoreDataHead = "Restoring data: ";
    static String METADATA_FILE_FIELD = "metadata_file";
    static String METADATA_FILE_NAME = "metadata_file_name";

    //private File restoreScript;

    static int ON_FINISH_NOTIFICATION_ID = 101;
    UIDClass uidClass;

    RootRestoreTask(Context context, long startMillis, int numberOfJobs, String installScriptPath, String restoreDataScriptPath) {
        this.context = context;
        this.startMillis = startMillis;
        this.numberOfJobs = numberOfJobs;
        this.installScriptPath = installScriptPath;
        this.restoreDataScriptPath = restoreDataScriptPath;
        errors = "";
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        uidClass = new UIDClass(context);
    }

    @Override
    protected Integer doInBackground(Vector<JSONObject>... jsonData) {

        Vector<JSONObject> jsonObjects = jsonData[0];

        Process restoreProcess;

        try {
            restoreProcess = Runtime.getRuntime().exec("su");
            BufferedWriter inputWriter = new BufferedWriter(new OutputStreamWriter(restoreProcess.getOutputStream()));
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(restoreProcess.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(restoreProcess.getErrorStream()));

            String line;

            int c = 0;
            String status = "";


            String command = getNextCommand(jsonObjects);

            inputWriter.write(command);
            inputWriter.flush();


            while ((line = outputReader.readLine()) != null){
                if (line.startsWith(statusHead)) {
                    c++;
                    status = line.substring(statusHead.length());
                    String apkName = jsonObjects.get(totalCount).getString("apk");
                    boolean appCheck = jsonObjects.get(totalCount).getBoolean(APP_CHECK);
                    if (appCheck) {
                        if (!apkName.equals("NULL"))
                            line = "Installing: " + apkName;
                        else line = "Skipping installation: " + status;
                    }
                    else {
                        status = UNCHANGED_STATUS;
                        line = "";
                    }
                }
                else if (line.startsWith(installedStatusHead)){

                    String data = jsonObjects.get(totalCount).getString("data");
                    String packageName = jsonObjects.get(totalCount).getString("package_name");
                    int uid = uidClass.getUid(packageName);

                    boolean dataCheck = jsonObjects.get(totalCount).getBoolean(DATA_CHECK);
                    if (!data.equals("NULL") && uid != -1 && dataCheck) {
                        inputWriter.write("echo \"" + restoreDataHead + data + "\"\n");
                        inputWriter.write("sh " + restoreDataScriptPath + " " + TEMP_DIR_NAME + " " + data + " " + packageName + " " + uid + "\n");
                        inputWriter.flush();
                    }

                    inputWriter.write("rm " + jsonObjects.get(totalCount).getString(METADATA_FILE_FIELD) + "\n");
                    inputWriter.write("rm " + TEMP_DIR_NAME + "/" + jsonObjects.get(totalCount).getString(METADATA_FILE_NAME) + "\n");
                    inputWriter.flush();

                    if (totalCount < numberOfJobs - 1) {
                        totalCount++;
                        inputWriter.write(getNextCommand(jsonObjects));
                        inputWriter.flush();
                    }
                    else {
                        inputWriter.write("exit\n");
                        inputWriter.flush();
                    }

                    line = "";
                }
                else if (line.startsWith(restoreDataHead)){
                    line = line + "\n\n";
                }
                else {
                    status = UNCHANGED_STATUS;
                }

                publishProgress(c, numberOfJobs, status, line);

            }
            while ((line = errorReader.readLine())!= null){
                String lowerLine = line.toLowerCase().trim();
                if (lowerLine.startsWith("selinux"))
                    continue;
                errors = errors + line + "\n";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CODE_ERROR;
        }

        if (errors.equals("")) {
            try {
                Runtime.getRuntime().exec("su -c rm -r " + TEMP_DIR_NAME);
            } catch (Exception ignored) {}

            return SUCCESS;
        }
        else return EXECUTION_ERROR;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel("PROGRESS", "Permission restore progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            progress = new NotificationCompat.Builder(context, "PROGRESS");
        else progress = new NotificationCompat.Builder(context);

        progress.setSmallIcon(R.drawable.ic_notification_icon);
        notificationManager.cancel(ON_FINISH_NOTIFICATION_ID);
    }


    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        restoreIntent.putExtra("job", (String)values[2]);
        restoreIntent.putExtra("c", (int)values[0]);
        restoreIntent.putExtra("n", (int)values[1]);
        restoreIntent.putExtra("message", (String)values[3]);
        context.sendBroadcast(restoreIntent);
        progress.setProgress((int)values[1], (int)values[0], false)
                .setContentTitle((String)values[2]);
        notificationManager.notify(RestoreService.RESTORE_SERVICE_NOTIFICATION_ID, progress.build());
    }

    @Override
    protected void onPostExecute(Integer o) {

        endMillis = timeInMillis();
        String totalTime = "(" + calendarDifference(startMillis, endMillis) + ")";

        super.onPostExecute(o);

        if ( o == SUCCESS) {

            Toast.makeText(context, context.getString(R.string.finished), Toast.LENGTH_SHORT).show();

            restoreIntent.putExtra("message", context.getString(R.string.uninstall_prompt));
            restoreIntent.putExtra("job", context.getString(R.string.finished) + "\n" + totalTime);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, ProgressActivity.class).putExtra("job", context.getString(R.string.finished)), 0))
                    .setContentTitle(context.getString(R.string.finished))
                    .setProgress(0, 0, false);
        }
        else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("message", errors + "\n" + context.getString(R.string.failed) + " " + o);
            restoreIntent.putExtra("job", context.getString(R.string.finished_with_errors) + "\n" + totalTime);
            progress.setContentIntent(PendingIntent.getActivity(context, 20, new Intent(context, ProgressActivity.class).putExtra("job", context.getString(R.string.finished_with_errors)).putExtra("message", errors + "\n" + context.getString(R.string.failed) + " " + o), 0))
                    .setContentTitle(context.getString(R.string.failed))
                    .setContentText(errors)
                    .setProgress(0, 0, false);
        }
        context.sendBroadcast(restoreIntent);

        notificationManager.notify(ON_FINISH_NOTIFICATION_ID, progress.build());
    }

    String getNextCommand(Vector<JSONObject> jsonObjects) throws JSONException {

        JSONObject jsonObject = jsonObjects.get(totalCount);
        String appName = jsonObject.getString("app_name");
        String packageName = jsonObject.getString("package_name");
        String apkName = jsonObject.getString("apk");
        String icon = jsonObject.getString("icon");

        String command = "echo \"" + statusHead + appName + " " + icon + "\"\n";

        boolean appCheck = jsonObject.getBoolean(APP_CHECK);
        if (!apkName.equals("NULL") && appCheck)
            command += "sh " + installScriptPath + " " + TEMP_DIR_NAME + " " + apkName + "\n";

        command += "echo \"" + installedStatusHead + packageName + "\"\n";

        return command;
    }

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
