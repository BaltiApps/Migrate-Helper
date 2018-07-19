package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<Void, Object, Integer> {

    private Context context;
    private String errors;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder progress;
    private int n;
    private int installN, restoreN;
    private Intent restoreIntent;
    private UIDClass uidClass;

    RootRestoreTask(Context context) {
        this.context = context;
        errors = "";
        n = 0;
        installN = restoreN = 0;
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        uidClass = new UIDClass(context);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel("PROGRESS", "Permission restore progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            progress = new NotificationCompat.Builder(context, "PROGRESS");
        else progress = new NotificationCompat.Builder(context);

        progress.setContentTitle(context.getString(R.string.requesting_root))
                .setSmallIcon(R.drawable.ic_fix)
                .setProgress(0, 0, false);
        progress.setContentIntent(PendingIntent.getActivity(context, 5, new Intent(context, MainActivity.class), 0));
        notificationManager.notify(100, progress.build());
    }

    private int restoreApp(Context context){
        int exitVal;

        try {

            File appList = new File(context.getExternalCacheDir(), "appList.txt");

            File script = new File(context.getFilesDir(), "app_script.sh");
            BufferedWriter tempWriter = new BufferedWriter(new FileWriter(script));
            String cmd = "#!/sbin/sh" + "\n\n" +
                    "cd /data/balti.migrate\n" +
                    "ls *-app.sh > " + appList.getAbsolutePath() + "\n" +
                    "chmod 777 " + appList.getAbsolutePath() + "\n";
            tempWriter.write(cmd);
            tempWriter.close();
            script.setExecutable(true);

            Process listProcess = Runtime.getRuntime().exec("su -c sh " + script.getAbsolutePath());

            if (listProcess.waitFor() == 0){

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
                    line = "sh /data/balti.migrate/" + line + "\n";
                    line = line + "echo \"INSTALLING_APPS: " + appName + " (" + c + "/" + installN + ")\"";
                    bufferedWriter.write(line + "\n", 0, line.length() + 1);
                }
                bufferedWriter.close();
                appInstallScript.setExecutable(true);
                appInstallScript.setReadable(true);

                script.delete();
                appList.delete();

                Log.d("MigrateHelper", "pt1");

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

                Log.d("MigrateHelper", "pt2 : " + errors + " eval: " + exitVal);

            }

            else exitVal = -50;


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = -200;
            Log.d("MigrateHelper", "pt3: " + e.getMessage());
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
                    "cd /data/balti.migrate\n" +
                    "ls *-data.sh > " + dataList.getAbsolutePath() + "\n" +
                    "chmod 777 " + dataList.getAbsolutePath() + "\n";
            tempWriter.write(cmd);
            tempWriter.close();
            script.setExecutable(true);

            Process listProcess = Runtime.getRuntime().exec("su -c sh " + script.getAbsolutePath());

            if (listProcess.waitFor() == 0){

                File dataRestoreScript = new File(context.getFilesDir(), "dataRestoreScript.sh");
                BufferedReader bufferedReader = new BufferedReader(new FileReader(dataList));
                BufferedReader bufferedReader2 = new BufferedReader(new FileReader(dataList));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dataRestoreScript));
                String line;

                restoreN = 1;

                while (bufferedReader2.readLine() != null){
                    restoreN++;
                }

                int c = 0;
                String appDataName;

                bufferedWriter.write("#!sbin/sh\n\n");
                while ((line = bufferedReader.readLine()) != null){
                    appDataName = line.substring(0, line.indexOf('-'));
                    c++;
                    line = "sh /data/balti.migrate/" + line + "\n";
                    line = line + "echo \"RESTORING: " + appDataName + " (" + c + "/" + restoreN + ")\"";
                    bufferedWriter.write(line + "\n", 0, line.length() + 1);
                }
                bufferedWriter.close();
                dataRestoreScript.setExecutable(true);
                dataRestoreScript.setReadable(true);

                Log.d("MigrateHelper", "pt4");

                script.delete();
                dataList.delete();

                Process restoreProcess = Runtime.getRuntime().exec("su -c sh " + dataRestoreScript.getAbsolutePath());
                BufferedReader err = new BufferedReader(new InputStreamReader(restoreProcess.getErrorStream()));
                BufferedReader output = new BufferedReader(new InputStreamReader(restoreProcess.getInputStream()));

                Log.d("MigrateHelper", "pt5");
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
                Log.d("MigrateHelper", "pt6 : " + errors + " eval: " + exitVal);

            }

            else exitVal = -50;


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = -200;
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
                exitVal = -50;
                errors = errors + "No apps to fix!";
            }
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter(script));
                writer.write("#!sbin/sh\n\n");
                n = uidClass.core.size();
                for (int i = 0; i < n; i++) {
                    writer.write(uidClass.core.elementAt(i));
                    writer.write("echo PERM: " + i + "\n");
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
                        publishProgress(c, n, context.getString(R.string.fixing_perm), line);
                    }
                }
                while ((line = err.readLine()) != null) {
                    localErrors = localErrors + line + "\n";
                }

                if ((exitVal = fixProcess.exitValue()) != 0)
                    errors = errors + localErrors + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = -200;
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

        super.onPostExecute(o);
        (new File(uidClass.permissionListPath)).delete();

        if ( o == 0 && errors.equals("")) {

            Toast.makeText(context, context.getString(R.string.finished), Toast.LENGTH_SHORT).show();

            restoreIntent.putExtra("message", context.getString(R.string.uninstall_prompt));
            restoreIntent.putExtra("job", context.getString(R.string.finished));

            progress.setContentTitle(context.getString(R.string.finished))
                    .setContentText("")
                    .setProgress(0, 0, false);
            notificationManager.notify(100, progress.build());

        } else if ( o == -100) {
            Toast.makeText(context, context.getString(R.string.notRooted), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("message", context.getString(R.string.notRooted));
            restoreIntent.putExtra("job", context.getString(R.string.finished_with_errors));
            progress.setContentIntent(PendingIntent.getActivity(context, 10, new Intent(context, ProgressActivity.class), 0))
                    .setContentTitle(context.getString(R.string.notRooted))
                    .setContentText("")
                    .setProgress(0, 0, false);
            notificationManager.notify(100, progress.build());
        } else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("message", errors + "\n" + context.getString(R.string.failed));
            restoreIntent.putExtra("job", context.getString(R.string.finished_with_errors));
            progress.setContentIntent(PendingIntent.getActivity(context, 20, new Intent(context, ProgressActivity.class).putExtra("job", context.getString(R.string.finished_with_errors)).putExtra("message", errors), 0))
                    .setContentTitle(context.getString(R.string.failed))
                    .setContentText(errors)
                    .setProgress(0, 0, false);
            notificationManager.notify(100, progress.build());
        }
        context.sendBroadcast(restoreIntent);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Process checkSu = null;
        try {
            restoreIntent.putExtra("job", context.getString(R.string.requesting_root));
            context.sendBroadcast(restoreIntent);
            checkSu = Runtime.getRuntime().exec("su -c echo");
            int r;
            checkSu.waitFor();
            if (checkSu.exitValue() == 0) {
                r = restoreApp(context);
                if (r == 0){
                    r = restoreData(context);
                    if (r == 0)
                        return restorePermission(context);
                    else return r;
                }
                else return r;
            }
            else return -100;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return -100;
    }

}
