package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by sayantan on 23/10/17.
 */

public class RootRestoreTask extends AsyncTask<Void, Integer, Integer> {

    Context context;
    String errors;
    NotificationManager notificationManager;
    NotificationCompat.Builder progress;
    int n;
    Intent restoreIntent;
    UIDClass uidClass;

    public RootRestoreTask(Context context) {
        this.context = context;
        errors = "";
        n = 0;
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        uidClass = new UIDClass(context);
        notificationManager.cancel(5);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel progressChannel = new NotificationChannel("PROGRESS", "Permission restore progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setSound(null, null);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(progressChannel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            progress = new NotificationCompat.Builder(context, "PROGRESS");
        else progress = new NotificationCompat.Builder(context);

        notificationManager.cancel(100);

        progress.setContentTitle(context.getString(R.string.requestingRootPermission))
                .setSmallIcon(R.drawable.ic_fix)
                .setProgress(0, 0, false);
        progress.setContentIntent(PendingIntent.getActivity(context, 5, new Intent(context, MainActivity.class), 0));
        notificationManager.notify(1, progress.build());
    }

    public int restoreSu(Context context) {
        uidClass.generateSpecificUids();
        int exitVal;
        File script = new File(context.getFilesDir().getAbsolutePath() + "/permissionFix.sh");
        try {

            if (uidClass.core.size() == 0) {
                exitVal = -50;
                errors = errors + "No apps to fix!";
            }
            else {
                BufferedWriter writer = new BufferedWriter(new FileWriter(script));
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
                        publishProgress(c);
                    }
                }
                while ((line = err.readLine()) != null) {
                    errors = errors + line + "\n";
                }
                exitVal = fixProcess.exitValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            exitVal = -200;
        }
        return exitVal;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        restoreIntent.putExtra("job", "rootOnProgress");
        restoreIntent.putExtra("c", values[0]);
        restoreIntent.putExtra("n", n);
        context.sendBroadcast(restoreIntent);
        progress.setProgress(n, values[0], false)
                .setContentTitle(context.getString(R.string.onProgress));
        notificationManager.notify(1, progress.build());
    }

    @Override
    protected void onPostExecute(Integer o) {
        super.onPostExecute(o);
        restoreIntent.putExtra("job", "finished");
        (new File(uidClass.permissionListPath)).delete();
        if ( o == 0) {
            Toast.makeText(context, context.getString(R.string.permissionsFixed), Toast.LENGTH_SHORT).show();
            try {
                uninstall();
            } catch (IOException e) {
                e.printStackTrace();
            }
            restoreIntent.putExtra("messages", context.getString(R.string.permissionsFixed));
            restoreIntent.putExtra("wasError", false);
            notificationManager.cancel(1);
        } else if ( o == -100) {
            Toast.makeText(context, context.getString(R.string.notRooted), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("messages", context.getString(R.string.notRooted));
            restoreIntent.putExtra("wasError", true);
            progress.setContentIntent(PendingIntent.getActivity(context, 10, new Intent(context, MainActivity.class), 0))
                    .setContentTitle(context.getString(R.string.notRooted))
                    .setContentText("")
                    .addAction(new NotificationCompat.Action(0, context.getString(R.string.notifTWRPOption), PendingIntent.getBroadcast(context, 20, new Intent(context.getString(R.string.actionTWRP)), PendingIntent.FLAG_UPDATE_CURRENT)))
                    .setProgress(0, 0, false);
            notificationManager.notify(100, progress.build());
        } else {
            Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("messages", errors + "\n" + context.getString(R.string.failed));
            restoreIntent.putExtra("wasError", true);
            progress.setContentIntent(PendingIntent.getActivity(context, 20, new Intent(context, MainActivity.class).setAction("showNotificationMessage").putExtra("wasError", true).putExtra("messages", errors), 0))
                    .setContentTitle(context.getString(R.string.failed))
                    .setContentText(errors)
                    .addAction(new NotificationCompat.Action(0, context.getString(R.string.notifTWRPOption), PendingIntent.getBroadcast(context, 20, new Intent(context.getString(R.string.actionTWRP)), PendingIntent.FLAG_UPDATE_CURRENT)))
                    .setProgress(0, 0, false);
            notificationManager.notify(100, progress.build());
        }
        context.sendBroadcast(restoreIntent);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Process checkSu = null;
        try {
            checkSu = Runtime.getRuntime().exec("su -c echo");
            checkSu.waitFor();
            if (checkSu.exitValue() == 0)
                return restoreSu(context);
            else return -100;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return -100;
    }


    void uninstall() throws IOException {
        File tempScript = new File(context.getFilesDir() + "/tempScript.sh");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
        String command = "#!/sbin/sh\n\n" +
                "mount -o rw,remount /system\n" +
                "rm -rf /system/app/PermissionFixer /data/data/balti.migratehelper\n";
        writer.write(command);
        writer.close();

        Runtime.getRuntime().exec("su -c sh " + tempScript.getAbsolutePath());
    }
}
