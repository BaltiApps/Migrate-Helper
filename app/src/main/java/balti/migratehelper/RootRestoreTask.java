package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.Calendar;

/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<Void, Object, Integer> {

    private Context context;
    private String errors;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder progress;

    private Intent restoreIntent;
    private UIDClass uidClass;

    private Process checkSu;
    private BroadcastReceiver cancelSuReceiver;

    private int permN;
    private int installN, restoreN;

    private boolean suCancelled;

    private int ERROR_CODE_NO_PERMISSION_LIST = -50;
    private int ERROR_CODE_SCRIPT_EXECUTION = -80;
    private int ERROR_CODE_EXECUTION = -200;
    private int ERROR_CODE_SU_CHECK = -100;
    private int ERROR_CODE_SU_CANCELLED = -300;
    private int SUCCESS = 0;

    private String TEMP_DIR_NAME = "/data/balti.migrate";
    private String tarBinaryFilePath = "";
    private String initError = "";

    private long startMillis;
    private long endMillis;

    RootRestoreTask(Context context) {
        this.context = context;
        errors = "";
        permN = 0;
        installN = restoreN = 0;
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        cancelSuReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    suCancelled = true;
                    if (checkSu != null)
                    checkSu.destroy();
                }
                catch (Exception ignored){}
            }
        };
        context.registerReceiver(cancelSuReceiver, new IntentFilter("cancel_su_broadcast"));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        startMillis = timeInMillis();

        uidClass = new UIDClass(context);

        suCancelled = false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel("PROGRESS", "Permission restore progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            progress = new NotificationCompat.Builder(context, "PROGRESS");
        else progress = new NotificationCompat.Builder(context);

        progress.setSmallIcon(R.drawable.ic_fix);
        notificationManager.cancel(101);

        unpackBinaries();
    }

    void unpackBinaries(){

        AssetManager assetManager = context.getAssets();

        File tarBinary = new File(context.getFilesDir(), "tar");

        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open("tar");
            FileOutputStream writer = new FileOutputStream(tarBinary);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
            tarBinary.setExecutable(true);
            tarBinaryFilePath = tarBinary.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            tarBinaryFilePath = "";
        }

    }

    private int restoreApp(Context context){
        int exitVal;

        try {

            File appList = new File(context.getExternalCacheDir(), "appList.txt");

            File script = new File(context.getFilesDir(), "app_script.sh");
            BufferedWriter tempWriter = new BufferedWriter(new FileWriter(script));
            String cmd = "#!/sbin/sh" + "\n\n" +
                    "cd " + TEMP_DIR_NAME + "\n" +
                    "ls *-app.sh > " + appList.getAbsolutePath() + "\n" +
                    "chmod 777 " + appList.getAbsolutePath() + "\n";
            tempWriter.write(cmd);
            tempWriter.close();
            script.setExecutable(true);

            Process listProcess = Runtime.getRuntime().exec("su -c sh " + script.getAbsolutePath());

            if (listProcess.waitFor() == SUCCESS){

                File appInstallScript = new File(context.getFilesDir(), "appInstallScript.sh");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(appList));
                BufferedReader bufferedReader2 = new BufferedReader(new FileReader(appList));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(appInstallScript));
                String line;

                installN = 0;

                while (bufferedReader2.readLine() != null){
                    installN++;
                }

                int c = 0;
                String appName;

                bufferedWriter.write("#!sbin/sh\n\n");
                while ((line = bufferedReader.readLine()) != null){
                    appName = line.substring(0, line.indexOf('-'));
                    c++;
                    line = "sh " + TEMP_DIR_NAME + "/" + line + "\n";
                    line = line + "echo \"INSTALLING_APPS: " + appName + " (" + c + "/" + installN + ")\"";
                    bufferedWriter.write(line + "\n", 0, line.length() + 1);
                }
                bufferedWriter.close();
                appInstallScript.setExecutable(true);
                appInstallScript.setReadable(true);

                script.delete();
                appList.delete();

                Process installProcess = Runtime.getRuntime().exec("su -c sh " + appInstallScript.getAbsolutePath());
                BufferedReader err = new BufferedReader(new InputStreamReader(installProcess.getErrorStream()));
                BufferedReader output = new BufferedReader(new InputStreamReader(installProcess.getInputStream()));

                c = 0;
                while ((line = output.readLine()) != null){
                    if (line.startsWith("INSTALLING_APPS:")){
                        c++;
                        publishProgress(c, installN, context.getString(R.string.installing_apps), line);
                    }
                }
                while ((line = err.readLine()) != null) {
                    errors = errors + line + "\n";
                }

                exitVal = installProcess.exitValue();

            }

            else exitVal = ERROR_CODE_SCRIPT_EXECUTION;


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = ERROR_CODE_EXECUTION;
        }

        return exitVal;
    }

    private int restoreData(Context context){
        int exitVal;

        try {
            File dataList = new File(context.getExternalCacheDir(), "dataList.txt");

            File script = new File(context.getFilesDir(), "data_script.sh");
            BufferedWriter tempWriter = new BufferedWriter(new FileWriter(script));
            String cmd = "#!sbin/sh" + "\n\n" +
                    "cd " + TEMP_DIR_NAME + "\n" +
                    "ls *-data.sh > " + dataList.getAbsolutePath() + "\n" +
                    "chmod 777 " + dataList.getAbsolutePath() + "\n";
            tempWriter.write(cmd);
            tempWriter.close();
            script.setExecutable(true);

            Process listProcess = Runtime.getRuntime().exec("su -c sh " + script.getAbsolutePath());

            if (listProcess.waitFor() == SUCCESS){

                File dataRestoreScript = new File(context.getFilesDir(), "dataRestoreScript.sh");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(dataList));
                BufferedReader bufferedReader2 = new BufferedReader(new FileReader(dataList));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dataRestoreScript));
                String line;

                restoreN = 0;

                while (bufferedReader2.readLine() != null){
                    restoreN++;
                }

                int c = 0;
                String appDataName;

                bufferedWriter.write("#!sbin/sh\n\n");
                while ((line = bufferedReader.readLine()) != null){
                    appDataName = line.substring(0, line.indexOf('-'));
                    c++;
                    line = "sh " + TEMP_DIR_NAME + "/" + line + "\n";
                    line = line + "echo \"RESTORING: " + appDataName + " (" + c + "/" + restoreN + ")\"";
                    bufferedWriter.write(line + "\n", 0, line.length() + 1);
                }
                bufferedWriter.close();
                dataRestoreScript.setExecutable(true);
                dataRestoreScript.setReadable(true);

                script.delete();
                dataList.delete();

                Process restoreProcess = Runtime.getRuntime().exec("su -c sh " + dataRestoreScript.getAbsolutePath());
                BufferedReader err = new BufferedReader(new InputStreamReader(restoreProcess.getErrorStream()));
                BufferedReader output = new BufferedReader(new InputStreamReader(restoreProcess.getInputStream()));
                c = 0;
                while ((line = output.readLine()) != null){
                    if (line.startsWith("RESTORING:")){
                        c++;
                        publishProgress(c, restoreN, context.getString(R.string.restoring_data), line);
                    }
                }
                while ((line = err.readLine()) != null) {
                    if (!line.startsWith("chmod"))
                        errors = errors + line + "\n";
                }

                exitVal = restoreProcess.exitValue();

            }

            else exitVal = ERROR_CODE_SCRIPT_EXECUTION;


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = ERROR_CODE_EXECUTION;
        }

        return exitVal;
    }

    private int restorePermission(Context context) {
        uidClass.generateSpecificUids();
        int exitVal;
        String localErrors = "";
        File script = new File(context.getFilesDir().getAbsolutePath() + "/permissionFix.sh");
        try {

            if (uidClass.core.size() == 0) {
                exitVal = ERROR_CODE_NO_PERMISSION_LIST;
                errors = errors + "No apps to fix!";
            }
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter(script));
                writer.write("#!sbin/sh\n\n");
                permN = uidClass.core.size();
                for (int i = 0; i < permN; i++) {
                    writer.write(uidClass.core.elementAt(i));
                }
                writer.close();
                Process fixProcess = Runtime.getRuntime().exec("su -c sh " + script.getAbsolutePath());
                BufferedReader err = new BufferedReader(new InputStreamReader(fixProcess.getErrorStream()));
                BufferedReader output = new BufferedReader(new InputStreamReader(fixProcess.getInputStream()));
                String line;
                int c = 0;
                while ((line = output.readLine()) != null){
                    if (line.startsWith("PERM:")){
                        c++;
                        publishProgress(c, permN, context.getString(R.string.fixing_perm), line);
                    }
                }
                while ((line = err.readLine()) != null) {
                    localErrors = localErrors + line + "\n";
                }

                if ((exitVal = fixProcess.exitValue()) != SUCCESS)
                    errors = errors + localErrors + "\n";

                if (errors.equals(""))
                    Runtime.getRuntime().exec("su -c rm -r " + TEMP_DIR_NAME);
            }
        } catch (IOException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = ERROR_CODE_EXECUTION;
        }
        return exitVal;
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
        notificationManager.notify(100, progress.build());
    }

    @Override
    protected void onPostExecute(Integer o) {

        endMillis = timeInMillis();
        String totalTime = "(" + calendarDifference(startMillis, endMillis) + ")";

        super.onPostExecute(o);
        (new File(uidClass.permissionListPath)).delete();

        if ( o == SUCCESS && errors.equals("")) {

            Toast.makeText(context, context.getString(R.string.finished), Toast.LENGTH_SHORT).show();

            restoreIntent.putExtra("message", context.getString(R.string.uninstall_prompt));
            restoreIntent.putExtra("job", context.getString(R.string.finished) + "\n" + totalTime);

            progress.setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, ProgressActivity.class).putExtra("job", context.getString(R.string.finished)), 0))
                    .setContentTitle(context.getString(R.string.finished))
                    .setProgress(0, 0, false);

        } else if ( o == ERROR_CODE_SU_CHECK) {
            Toast.makeText(context, context.getString(R.string.initError), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("message", context.getString(R.string.initError) + "\n\n" + initError);
            restoreIntent.putExtra("job", context.getString(R.string.finished_with_errors));
            progress.setContentIntent(null)
                    .setContentTitle(context.getString(R.string.initError))
                    .setProgress(0, 0, false);
        }
        else if (o == ERROR_CODE_SU_CANCELLED){

            restoreIntent.putExtra("message", context.getString(R.string.su_cancelled));
            restoreIntent.putExtra("job", context.getString(R.string.cancelled) + "\n" + totalTime);
            progress.setContentIntent(null)
                    .setContentTitle(context.getString(R.string.cancelled))
                    .setProgress(0, 0, false);
        }
        else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("message", errors + "\n" + context.getString(R.string.failed) + " " + o);
            restoreIntent.putExtra("job", context.getString(R.string.finished_with_errors) + "\n" + totalTime);
            progress.setContentIntent(PendingIntent.getActivity(context, 20, new Intent(context, ProgressActivity.class).putExtra("job", context.getString(R.string.finished_with_errors)).putExtra("message", errors), 0))
                    .setContentTitle(context.getString(R.string.failed))
                    .setContentText(errors)
                    .setProgress(0, 0, false);
        }
        context.sendBroadcast(restoreIntent);

        notificationManager.notify(101, progress.build());

        context.unregisterReceiver(cancelSuReceiver);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            broadcastRequestingSu();
            String cmd = "su -c cp " + tarBinaryFilePath + " " + TEMP_DIR_NAME + "/tar && echo ROOT_OK";
            checkSu = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkSu.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(checkSu.getErrorStream()));
            int r;

            String output = reader.readLine();
            String errLine;
            while ((errLine = err.readLine()) != null)
                initError = initError + errLine + "\n";

            if (output == null) {
                return ERROR_CODE_SU_CHECK;
            } else if (output.equals("ROOT_OK") && !suCancelled) {
                r = restoreApp(context);
                if (r == SUCCESS) {
                    r = restoreData(context);
                    if (r == SUCCESS)
                        return restorePermission(context);
                    else return r;
                } else return r;
            }


        }
        catch (InterruptedIOException e){
            if (suCancelled)
                return ERROR_CODE_SU_CANCELLED;
            else {
                e.printStackTrace();
                return ERROR_CODE_SU_CHECK;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ERROR_CODE_EXECUTION;
    }

    private void broadcastRequestingSu(){
        restoreIntent.putExtra("job", context.getString(R.string.requesting_root));
        restoreIntent.putExtra("message", context.getString(R.string.initMessage));
        progress.setContentIntent(PendingIntent.getActivity(context, 5, new Intent(context, ProgressActivity.class), 0))
                .setContentTitle(context.getString(R.string.requesting_root))
                .setProgress(0, 0, false);

        context.sendBroadcast(restoreIntent);
        notificationManager.notify(100, progress.build());
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
