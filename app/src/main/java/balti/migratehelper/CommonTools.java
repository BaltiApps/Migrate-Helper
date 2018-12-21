package balti.migratehelper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static balti.migratehelper.AppSelector.METADATA_HOLDER_DIR;

public class CommonTools {

    Context context;

    static String UNINSTALL_INTENT_FILTER = "migrate_helper_uninstall_broadcast";
    static String DEBUG_TAG = "migrate_helper_tag";

    static String TEMP_DIR_NAME = "/data/balti.migrate";

    public CommonTools(Context context) {
        this.context = context;
    }

    String unpackAssetToInternal(String assetFileName, String targetFileName){

        AssetManager assetManager = context.getAssets();
        File unpackFile = new File(context.getFilesDir(), targetFileName);
        String path = "";

        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open(assetFileName);
            FileOutputStream writer = new FileOutputStream(unpackFile);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
            unpackFile.setExecutable(true);
            path = unpackFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            path = "";
        }

        return path;
    }

    void reportLogs(boolean isErrorLogMandatory){
        final File progressLog = new File(METADATA_HOLDER_DIR, "progressLog.txt");
        final File errorLog = new File(METADATA_HOLDER_DIR, "errorLog.txt");
        final File package_data = new File(METADATA_HOLDER_DIR, "package-data.txt");
        final File theRestoreScript = new File(METADATA_HOLDER_DIR, "the_restore_script.sh");

        if (isErrorLogMandatory && !errorLog.exists()){
            new AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        else if (errorLog.exists() || progressLog.exists() || theRestoreScript.exists() || package_data.exists()) {

            View errorReportView = View.inflate(context, R.layout.error_report_layout, null);

            final CheckBox shareProgress, shareErrors, shareScript, sharePackageData;
            shareProgress = errorReportView.findViewById(R.id.share_progress_checkbox);
            shareErrors = errorReportView.findViewById(R.id.share_errors_checkbox);
            shareScript = errorReportView.findViewById(R.id.share_script_checkbox);
            sharePackageData = errorReportView.findViewById(R.id.share_package_data);

            if (!progressLog.exists()){
                shareProgress.setChecked(false);
                shareProgress.setEnabled(false);
            }
            else {
                shareProgress.setEnabled(true);
                shareProgress.setChecked(true);
            }

            if (!package_data.exists()){
                sharePackageData.setChecked(false);
                sharePackageData.setEnabled(false);
            }
            else {
                sharePackageData.setEnabled(true);
                sharePackageData.setChecked(true);
            }

            if (!theRestoreScript.exists()){
                shareScript.setChecked(false);
                shareScript.setEnabled(false);
            }
            else {
                shareScript.setEnabled(true);
                shareScript.setChecked(true);
            }

            if (isErrorLogMandatory && errorLog.exists()){
                shareErrors.setChecked(true);
                shareErrors.setEnabled(false);
            }
            else if (!errorLog.exists()){
                shareErrors.setChecked(false);
                shareErrors.setEnabled(false);
            }
            else {
                shareErrors.setChecked(true);
                shareErrors.setEnabled(true);
            }

            new AlertDialog.Builder(context)
                    .setView(errorReportView)
                    .setPositiveButton(R.string.agree_and_send, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String body = getDeviceSpecifications();

                            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            emailIntent.setType("text/plain");
                            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate");
                            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                            ArrayList<Uri> uris = new ArrayList<>(0);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                if (shareErrors.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", errorLog));
                                if (shareProgress.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", progressLog));
                                if (shareScript.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", theRestoreScript));
                                if (sharePackageData.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", package_data));
                            }
                            else {
                                if (shareErrors.isChecked())
                                    uris.add(Uri.fromFile(errorLog));
                                if (shareProgress.isChecked())
                                    uris.add(Uri.fromFile(progressLog));
                                if (shareScript.isChecked())
                                    uris.add(Uri.fromFile(theRestoreScript));
                                if (sharePackageData.isChecked())
                                    uris.add(Uri.fromFile(package_data));
                            }

                            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                            try {
                                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.select_mail)));
                                Toast.makeText(context, context.getString(R.string.select_mail), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) { Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show(); }

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        else {

            String msg = "";
            if (!progressLog.exists())
                msg += context.getString(R.string.progress_log_does_not_exist) + "\n";
            if (!errorLog.exists())
                msg += context.getString(R.string.error_log_does_not_exist) + "\n";
            if (!theRestoreScript.exists())
                msg += context.getString(R.string.restore_script_does_not_exist) + "\n";

            new AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }


    }

    String getDeviceSpecifications(){

        String body = "";

        body = body + "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n";
        body = body + "Brand: " + Build.BRAND + "\n";
        body = body + "Manufacturer: " + Build.MANUFACTURER + "\n";
        body = body + "Model: " + Build.MODEL + "\n";
        body = body + "Device: " + Build.DEVICE + "\n";
        body = body + "SDK: " + Build.VERSION.SDK_INT + "\n";
        body = body + "Board: " + Build.BOARD + "\n";
        body = body + "Hardware: " + Build.HARDWARE;

        return body;
    }

    boolean isServiceRunning(String name){
        boolean result = false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (name.equals(service.service.getClassName()))
                result = true;
        }
        return result;
    }
}
