package balti.migratehelper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by sayantan on 24/10/17.
 */

public class TWRPRestoreTask extends AsyncTask<Void, Void, Boolean> {

    Context context;
    String errors;
    NotificationManager notificationManager;
    NotificationCompat.Builder progress;
    Intent restoreIntent;
    String destination;

    public TWRPRestoreTask(Context context) {
        this.context = context;
        errors = "";
        restoreIntent  = new Intent(context.getString(R.string.actionRestoreOnProgress));
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        destination = Environment.getExternalStorageDirectory() + "/Migrate/Fix_Permission_TWRP";
        (new File(destination)).mkdirs();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
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

        notificationManager.cancel(200);

        progress.setContentTitle(context.getString(R.string.requestingRootPermission))
                .setSmallIcon(R.drawable.ic_fix)
                .setProgress(0, 0, true);
        progress.setContentIntent(PendingIntent.getActivity(context, 5, new Intent(context, MainActivity.class), 0));
        notificationManager.notify(2, progress.build());
    }

    public boolean restoreTWRP(Context context) {
        publishProgress();
        UIDClass uidClass = new UIDClass(context);
        uidClass.generateAllUids();
        boolean success;
        File script = new File(destination + "/permissionFix.sh");
        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(script));
            writer.write("#!sbin/sh\n\n");

            for (int i = 0; i < uidClass.core.size(); i++) {
                writer.write(uidClass.core.elementAt(i));
            }

            String ext = "";

            try {
                Process mountDisplay = Runtime.getRuntime().exec("mount");
                //mountDisplay.waitFor();


                String mount_points = "", line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(mountDisplay.getInputStream()));
                while ((line = reader.readLine()) != null)
                {
                    mount_points = mount_points + line + '\n';
                }

                try {
                    BufferedReader bufferedReader = new BufferedReader(new StringReader(mount_points));

                    while ((line = bufferedReader.readLine()) != null){
                        if (line.startsWith("/dev/block")){
                            line = line + " ";
                            String block = line.substring(0, line.indexOf(' '));
                            if (block.endsWith("system")){
                                ext = line.substring(line.indexOf("type") + 5, line.indexOf(' ', line.indexOf("type") + 6));
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
                errors = errors + e.getMessage() + "\n";
            }

            String source = context.getApplicationInfo().sourceDir;
            writer.write("mount -o rw,remount -t " + ext + " /system\n");
            writer.write("rm -rf " + source.substring(0, source.lastIndexOf('/')) + "\n");
            writer.write("rm -rf " + "/data/data/" + context.getApplicationInfo().packageName + "\n");
            writer.write("mount -o ro,remount -t " + ext + " /system\n");
            writer.write("rm /data/permissionList\n");
            writer.write("rm /tmp/permissionFix.sh\n");

            writer.close();

            String path = destination + "/META-INF/com/google/android/";
            File pathFile = new File(path);
            pathFile.mkdirs();

            AssetManager assetManager = context.getAssets();
            File update_binary = new File(path + "update-binary");
            File updater_script = new File(path + "updater-script");

            FileOutputStream fileOutputStream = new FileOutputStream(update_binary);
            InputStream inputStream = assetManager.open("update-binary");
            byte buffer[] = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) > 0)
            {
                fileOutputStream.write(buffer, 0, read);
            }
            fileOutputStream.close();

            fileOutputStream = new FileOutputStream(updater_script);
            inputStream = assetManager.open("updater-script");
            while ((read = inputStream.read(buffer)) > 0)
            {
                fileOutputStream.write(buffer, 0, read);
            }
            fileOutputStream.close();

            errors = javaZipAll(destination);

            if(errors.equals(""))
                success = true;
            else success = false;

        } catch (IOException e) {
            e.printStackTrace();
            errors = errors + e.getMessage() + "\n";
            success = false;
        }
        return success;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        restoreIntent.putExtra("job", "twrpOnProgress");
        context.sendBroadcast(restoreIntent);
    }

    @Override
    protected void onPostExecute(Boolean o) {
        super.onPostExecute(o);
        restoreIntent.putExtra("job", "finished");
        if ( o ) {
            String message = context.getString(R.string.zipLocation) + "\n" + destination + "/fixPermissions.zip";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("messages", message);
            restoreIntent.putExtra("wasError", false);
            //notificationManager.cancel(2);
            progress.setContentIntent(PendingIntent.getActivity(context, 10, new Intent(context, MainActivity.class).setAction("showNotificationMessage").putExtra("wasError", false).putExtra("messages", message), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.zipMade))
                    .setContentText(message)
                    .setProgress(0, 0, false);
            notificationManager.notify(200, progress.build());
        } else {
            Toast.makeText(context, context.getString(R.string.twrpFailed), Toast.LENGTH_SHORT).show();
            restoreIntent.putExtra("messages", errors);
            restoreIntent.putExtra("wasError", true);
            progress.setContentIntent(PendingIntent.getActivity(context, 20, new Intent(context, MainActivity.class).setAction("showNotificationMessage").putExtra("wasError", true).putExtra("messages", errors), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(context.getString(R.string.twrpFailed))
                    .setContentText(errors)
                    .setProgress(0, 0, false);
            notificationManager.notify(200, progress.build());
        }
        context.sendBroadcast(restoreIntent);
    }

    private String javaZipAll(String path){

        String error = "";
        try {

            File directory = new File(path);
            Vector<File> files = getAllFiles(directory, new Vector<File>(1));

            File zipFile = new File(path.substring(0, path.lastIndexOf('/')) + "/fixPermissions.zip");
            zipFile.delete();

            FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            for (File file : files)
            {

                String fn = file.getAbsolutePath();
                fn = fn.substring(directory.getAbsolutePath().length() + 1);
                ZipEntry zipEntry;

                if (file.isDirectory()) {
                    zipEntry = new ZipEntry(fn + "/");
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.closeEntry();
                    continue;
                }
                else {
                    zipEntry = new ZipEntry(fn);
                    zipOutputStream.putNextEntry(zipEntry);

                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fileInputStream.read(buffer)) > 0)
                    {
                        zipOutputStream.write(buffer, 0, read);
                    }
                    zipOutputStream.closeEntry();
                    fileInputStream.close();
                    file.delete();
                }
            }
            zipOutputStream.close();
            fullDelete(path);

            destination = destination.substring(0, destination.lastIndexOf('/'));

        }
        catch (Exception e) {
            error = e.getMessage();
        }
        return error;
    }

    Vector<File> getAllFiles(File directory, Vector<File> allFiles){
        File files[] = directory.listFiles();
        for (File f : files){
            if (f.isFile())
                allFiles.addElement(f);
            else {
                allFiles.addElement(f);
                getAllFiles(f, allFiles);
            }
        }
        return allFiles;
    }


    void fullDelete(String path){
        File file = new File(path);
        if (file.exists()) {
            if (!file.isDirectory())
                file.delete();
            else {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++)
                    fullDelete(files[i].getAbsolutePath());
                file.delete();
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return restoreTWRP(context);
    }
}
