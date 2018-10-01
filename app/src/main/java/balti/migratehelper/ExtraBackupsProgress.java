package balti.migratehelper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import static balti.migratehelper.AppSelector.TEMP_DIR_NAME;
import static balti.migratehelper.GetJsonFromData.APP_CHECK;
import static balti.migratehelper.GetJsonFromData.DATA_CHECK;
import static balti.migratehelper.RootRestoreTask.DISPLAY_HEAD;
import static balti.migratehelper.RootRestoreTask.INSTALLING_HEAD;
import static balti.migratehelper.RootRestoreTask.RESTORE_DATA_HEAD;

public class ExtraBackupsProgress extends AppCompatActivity implements OnDBRestoreComplete {

    TextView headTitle;
    ProgressBar headProgressBar;

    LinearLayout contactsView;
    ImageView contactsDone, contactsCancel;
    ProgressBar contactsProgress;

    LinearLayout smsView;
    ImageView smsDone, smsCancel;
    ProgressBar smsProgress;
    TextView smsStatusText;

    LinearLayout callsView;
    ImageView callsDone, callsCancel;
    ProgressBar callsProgress;
    TextView callsStatusText;

    LinearLayout applications;
    ImageView appsDone;
    ProgressBar appsProgress;
    TextView appsStatusText;

    static int totalTasks = 0;
    static int intProgressTask = 0;

    private static int CONTACTS_RESTORE = 10;

    static GetJsonFromDataPackets getJsonFromDataPackets;
    static int numberOfApps = 0;
    static String installScriptPath;
    static String restoreDataScriptPath;
    static boolean extraSelectBoolean;

    static boolean isShowContacts = false;
    static boolean isShowSms = false;
    static boolean isShowCalls = false;

    BroadcastReceiver startRestoreFromExtraBackups;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    ContactsPacket contactsPackets[];
    int contactCount = 0;

    SmsPacket smsPacket[];
    AlertDialog smsPermissionDialog;
    String actualDefaultSmsAppName = "";
    int SET_THIS_AS_DEFAULT_SMS_APP = 2;
    int SET_ORIGINAL_APP_AS_DEFAULT_SMS_APP = 3;
    int SMS_RESTORE_JOB = 20;

    CallsPacket callsPackets[];
    AlertDialog callsPermissionDialog;
    int CALLS_PERMISSION_REQUEST = 4;
    int CALLS_RESTORE_JOB = 40;

    long startTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.extra_backup_progress);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        headTitle = findViewById(R.id.extra_backup_progress_title);
        headProgressBar = findViewById(R.id.extra_backup_progress_head_progress);

        contactsView = findViewById(R.id.extra_backup_progress_item_contacts);
        contactsProgress = findViewById(R.id.extra_backup_progress_item_contacts_progress);
        contactsDone = findViewById(R.id.extra_backup_progress_item_contacts_done);
        contactsCancel = findViewById(R.id.extra_backup_progress_item_contacts_cancel);

        smsView = findViewById(R.id.extra_backup_progress_item_sms);
        smsProgress = findViewById(R.id.extra_backup_progress_item_sms_progress);
        smsStatusText = findViewById(R.id.extra_backup_progress_item_sms_progress_in_words);
        smsDone = findViewById(R.id.extra_backup_progress_item_sms_done);
        smsCancel = findViewById(R.id.extra_backup_progress_item_sms_cancel);

        callsView = findViewById(R.id.extra_backup_progress_item_calls);
        callsProgress = findViewById(R.id.extra_backup_progress_item_calls_progress);
        callsStatusText = findViewById(R.id.extra_backup_progress_item_calls_progress_in_words);
        callsDone = findViewById(R.id.extra_backup_progress_item_calls_done);
        callsCancel = findViewById(R.id.extra_backup_progress_item_calls_cancel);

        applications = findViewById(R.id.extra_backup_progress_item_applications);
        appsProgress = findViewById(R.id.extra_backup_progress_item_app_progress);
        appsStatusText = findViewById(R.id.extra_backup_progress_item_app_progress_in_words);
        appsDone = findViewById(R.id.extra_backup_progress_item_app_done);

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent toSendIntent = new Intent(ExtraBackupsProgress.this, ProgressActivity.class);
                toSendIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                startActivity(toSendIntent);
                finish();
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, progressReceiverIF);

        startRestoreFromExtraBackups = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                triggerRestoreProcessStart();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(startRestoreFromExtraBackups, new IntentFilter("startRestoreFromExtraBackups"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("extraBackupsProgressReady"));
    }

    static void setData(GetJsonFromDataPackets getJsonFromDataPackets, int numberOfApps, String installScriptPath, String restoreDataScriptPath, boolean extraSelectBoolean){

        ExtraBackupsProgress.getJsonFromDataPackets = getJsonFromDataPackets;
        ExtraBackupsProgress.numberOfApps = numberOfApps;
        ExtraBackupsProgress.installScriptPath = installScriptPath;
        ExtraBackupsProgress.restoreDataScriptPath = restoreDataScriptPath;
        ExtraBackupsProgress.extraSelectBoolean = extraSelectBoolean;

        if (extraSelectBoolean) {

            for (ContactsPacket cp : getJsonFromDataPackets.contactPackets) {
                if (isShowContacts = cp.selected) {
                    totalTasks++;
                    break;
                }
            }

            for (SmsPacket sp : getJsonFromDataPackets.smsPackets) {
                if (isShowSms = sp.selected) {
                    totalTasks++;
                    break;
                }
            }

            for (CallsPacket clp : getJsonFromDataPackets.callsPackets){
                if (isShowCalls = clp.selected) {
                    totalTasks++;
                    break;
                }
            }
        }
        else {
            isShowContacts = false;
            isShowSms = false;
            isShowCalls = false;
        }

        if (numberOfApps > 0) totalTasks++;

    }

    void triggerRootRestoreTask(){

        new FilterAcceptedApps().execute();

    }

    void triggerRestoreProcessStart(){

        try {

            startTime = timeInMillis();

            headProgressBar.setMax(totalTasks);

            contactsPackets = getJsonFromDataPackets.contactPackets;
            smsPacket = getJsonFromDataPackets.smsPackets;
            callsPackets = getJsonFromDataPackets.callsPackets;

            if (numberOfApps > 0) applications.setVisibility(View.VISIBLE);

            if (extraSelectBoolean) {

                if (isShowContacts) contactsView.setVisibility(View.VISIBLE);
                if (isShowSms) smsView.setVisibility(View.VISIBLE);
                if (isShowCalls) callsView.setVisibility(View.VISIBLE);

                restoreContacts();
            }
            else {
                triggerRootRestoreTask();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void restoreContacts() {

        if (isShowContacts) {

            headProgressBar.setProgress(++intProgressTask);
            headTitle.setText(R.string.contacts);

            contactsProgress.setVisibility(View.VISIBLE);

            View contactsView = View.inflate(this, R.layout.contacts_dialog_view, null);
            LinearLayout holder = contactsView.findViewById(R.id.contact_files_display_holder);
            for (ContactsPacket packet : contactsPackets) {
                if (packet.selected) {
                    TextView textView = new TextView(this);
                    textView.setText(packet.vcfFile.getName());
                    holder.addView(textView);
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_contacts_dialog_header)
                    .setView(contactsView)
                    .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i1) {
                            nextContact();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // next process 0

                            contactsProgress.setVisibility(View.GONE);
                            contactsDone.setVisibility(View.GONE);
                            contactsCancel.setVisibility(View.VISIBLE);

                            restoreSms();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        else {

            // next process 0

            restoreSms();
        }
    }

    private void nextContact(){
        int j;
        for (j = contactCount; j < contactsPackets.length; j++) {
            ContactsPacket packet = contactsPackets[j];
            if (packet.selected) {
                contactCount = j+1;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(FileProvider.getUriForFile(ExtraBackupsProgress.this, "migrate.helper.provider", packet.vcfFile), "text/x-vcard");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(i, CONTACTS_RESTORE);
                return;
            }
        }
        if (j >= contactsPackets.length){

            // next process 0

            contactsProgress.setVisibility(View.GONE);
            contactsDone.setVisibility(View.VISIBLE);
            contactsCancel.setVisibility(View.GONE);

            restoreSms();
        }
    }

    private void restoreSms(){

        if (isShowSms){

            headProgressBar.setProgress(++intProgressTask);
            headTitle.setText(R.string.sms);

            smsPermissionDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.smsPermission)
                    .setMessage(getText(R.string.smsPermission_desc))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (!Telephony.Sms.getDefaultSmsPackage(ExtraBackupsProgress.this).equals(getPackageName())){
                                actualDefaultSmsAppName = Telephony.Sms.getDefaultSmsPackage(ExtraBackupsProgress.this);
                                setDefaultSms(getPackageName(), SET_THIS_AS_DEFAULT_SMS_APP);
                            }
                            else {
                                nextSms();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // next process 1

                            smsDone.setVisibility(View.GONE);
                            smsCancel.setVisibility(View.VISIBLE);

                            restoreCalls();

                        }
                    })
                    .setCancelable(false)
                    .create();


            if (!Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName()))
                smsPermissionDialog.show();
            else nextSms();
        }
        else {

            // next process 1

            restoreCalls();

        }

    }

    private void setDefaultSms(String packageName, int requestCode){
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        startActivityForResult(intent, requestCode);
    }

    private void nextSms(){

        int j;
        List<File> acceptedFiles = new ArrayList<>(0);

        if (!Telephony.Sms.getDefaultSmsPackage(ExtraBackupsProgress.this).equals(getPackageName())) {
            smsPermissionDialog.show();
            return;
        }

        for (j = 0; j < smsPacket.length; j++) {
            SmsPacket packet = smsPacket[j];
            if (packet.selected) {
                acceptedFiles.add(packet.smsDBFile);
            }
        }

        String projection[] = new String[]{
                "smsAddress",
                "smsBody",
                "smsType",
                "smsDate",
                "smsDateSent",
                "smsCreator",
                "smsPerson",
                "smsProtocol",
                "smsSeen",
                "smsServiceCenter",
                "smsStatus",
                "smsSubject",
                "smsThreadId",
                "smsError",
                "smsRead",
                "smsLocked",
                "smsReplyPathPresent"
        };

        String mirror[][] = new String[][]{
                {Telephony.Sms.ADDRESS, "s"},
                {Telephony.Sms.BODY, "s"},
                {Telephony.Sms.TYPE, "s"},
                {Telephony.Sms.DATE, "s"},
                {Telephony.Sms.DATE_SENT, "s"},
                {Telephony.Sms.CREATOR, "s"},
                {Telephony.Sms.PERSON, "s"},
                {Telephony.Sms.PROTOCOL, "s"},
                {Telephony.Sms.SEEN, "s"},
                {Telephony.Sms.SERVICE_CENTER, "s"},
                {Telephony.Sms.STATUS, "s"},
                {Telephony.Sms.SUBJECT, "s"},

                {Telephony.Sms.ERROR_CODE, "i"},
                {Telephony.Sms.READ, "i"},
                {Telephony.Sms.LOCKED, "i"},
                {Telephony.Sms.REPLY_PATH_PRESENT, "i"}
        };

        String tableName = "sms";
        Uri uri = Telephony.Sms.CONTENT_URI;

        RestoreA_DB restoreA_DB = new RestoreA_DB(acceptedFiles, projection, mirror, smsProgress, smsStatusText, tableName, uri, this, SMS_RESTORE_JOB);
        restoreA_DB.execute();
    }

    private void restoreCalls(){

        if (isShowCalls) {

            headProgressBar.setProgress(++intProgressTask);
            headTitle.setText(R.string.calls);

            callsPermissionDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.callsPermission)
                    .setMessage(getText(R.string.callsPermission_desc))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (ContextCompat.checkSelfPermission(ExtraBackupsProgress.this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(ExtraBackupsProgress.this, new String[]{Manifest.permission.WRITE_CALL_LOG}, CALLS_PERMISSION_REQUEST);
                            }
                            else {
                                nextCalls();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // next process 2

                            callsDone.setVisibility(View.GONE);
                            callsCancel.setVisibility(View.VISIBLE);

                            triggerRootRestoreTask();

                        }
                    })
                    .setCancelable(false)
                    .create();

            if (ContextCompat.checkSelfPermission(ExtraBackupsProgress.this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
                callsPermissionDialog.show();
            else nextCalls();
        }
        else {

            // next process 2

            triggerRootRestoreTask();

        }

    }

    private void nextCalls(){

        int j;
        List<File> acceptedFiles = new ArrayList<>(0);

        if (ContextCompat.checkSelfPermission(ExtraBackupsProgress.this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            callsPermissionDialog.show();
            return;
        }

        for (j = 0; j < callsPackets.length; j++) {
            CallsPacket packet = callsPackets[j];
            if (packet.selected) {
                acceptedFiles.add(packet.callsDBFile);
            }
        }

        String projection[] = new String[]{
                "callsCountryIso",
                "callsDataUsage",
                "callsFeatures",
                "callsGeocodedLocation",
                "callsIsRead",
                "callsNumber",
                "callsNumberPresentation",
                "callsPhoneAccountComponentName",
                "callsType",
                "callsVoicemailUri",
                "callsDate",
                "callsDuration",
                "callsNew"
        };

        String mirror[][] = new String[][]{
                {CallLog.Calls.COUNTRY_ISO, "s"},
                {CallLog.Calls.DATA_USAGE, "s"},
                {CallLog.Calls.FEATURES, "s"},
                {CallLog.Calls.GEOCODED_LOCATION, "s"},
                {CallLog.Calls.IS_READ, "s"},
                {CallLog.Calls.NUMBER, "s"},
                {CallLog.Calls.NUMBER_PRESENTATION, "s"},
                {CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, "s"},
                {CallLog.Calls.TYPE, "s"},
                {CallLog.Calls.VOICEMAIL_URI, "s"},

                {CallLog.Calls.DATE, "l"},
                {CallLog.Calls.DURATION, "l"},
                {CallLog.Calls.NEW, "i"}
        };

        String tableName = "calls";
        Uri uri = CallLog.Calls.CONTENT_URI;

        RestoreA_DB restoreA_DB = new RestoreA_DB(acceptedFiles, projection, mirror, callsProgress, callsStatusText, tableName, uri, this, CALLS_RESTORE_JOB);
        restoreA_DB.execute();
    }

    long timeInMillis(){
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(startRestoreFromExtraBackups);
        }catch (Exception ignored){}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALLS_PERMISSION_REQUEST){
            nextCalls();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACTS_RESTORE){
            nextContact();
        }
        else if (requestCode == SET_THIS_AS_DEFAULT_SMS_APP){
            nextSms();
        }
        else if (requestCode == SET_ORIGINAL_APP_AS_DEFAULT_SMS_APP) {
            restoreCalls();
        }
    }


    @Override
    public void onDBRestoreComplete(int code) {
        if (code == SMS_RESTORE_JOB){
            // next process 1

            smsDone.setVisibility(View.VISIBLE);
            smsCancel.setVisibility(View.GONE);

            setDefaultSms(actualDefaultSmsAppName, SET_ORIGINAL_APP_AS_DEFAULT_SMS_APP);

        }
        else if (code == CALLS_RESTORE_JOB){
            // next process 2

            callsDone.setVisibility(View.VISIBLE);
            callsCancel.setVisibility(View.GONE);

            triggerRootRestoreTask();

        }
    }

    class FilterAcceptedApps extends AsyncTask{

        int n = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            appsProgress.setIndeterminate(true);

            n = getJsonFromDataPackets.jsonAppPackets.size();

            if (n > 0) {
                headTitle.setText(R.string.applications);
                headProgressBar.setProgress(++intProgressTask);

                appsProgress.setVisibility(View.VISIBLE);
                appsStatusText.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            if (n == 0)
                return null;
            else
                appsProgress.setIndeterminate(false);

            Vector<JSONObject> jsonObjects = getJsonFromDataPackets.jsonAppPackets;

            appsProgress.setMax(jsonObjects.size());

            for (int j = 0, c = 1; j < jsonObjects.size(); j++, c++) {
                JSONObject jsonObject = jsonObjects.get(j);
                try {
                    if (!(jsonObject.getBoolean(APP_CHECK) || jsonObject.getBoolean(DATA_CHECK))) {
                        jsonObjects.remove(jsonObject);
                        j--;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                publishProgress(getString(R.string.filtering), c, n);
            }

            File restoreScript = new File(getFilesDir(), "restoreAppScript.sh");

            BufferedWriter scriptWriter = null;

            try {
                scriptWriter = new BufferedWriter(new FileWriter(restoreScript));

                for (int i = 0; i < jsonObjects.size(); i++) {

                    JSONObject jsonObject = jsonObjects.get(i);

                    publishProgress(getString(R.string.making_script), i, numberOfApps);

                    String appName = jsonObject.getString("app_name");
                    String apkName = jsonObject.getString("apk");
                    String dataName = jsonObject.getString("data");
                    String packageName = jsonObject.getString("package_name");
                    String icon = jsonObject.getString("icon");

                    boolean isApp = jsonObject.getBoolean(APP_CHECK) && !apkName.equals("NULL");
                    boolean isData = jsonObject.getBoolean(DATA_CHECK) && !dataName.equals("NULL");

                    String command = "";

                    if (!isApp && !isData)
                        continue;

                    command += "echo " + DISPLAY_HEAD + appName + " " + icon + "\n";

                    if (isApp) {
                        command += "echo " + INSTALLING_HEAD + apkName + "\n";
                        command += "sh " + installScriptPath + " " + TEMP_DIR_NAME + " " + apkName + "\n";
                    }
                    if (isData) {
                        command += "echo " + RESTORE_DATA_HEAD + apkName + "\n";
                        command += "sh " + restoreDataScriptPath + " " + TEMP_DIR_NAME + " " + dataName + " " + packageName + "\n";
                    }

                    command += "echo  " + "\n";

                    scriptWriter.write(command);
                }

                scriptWriter.close();
                restoreScript.setExecutable(true);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return restoreScript;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            appsProgress.setProgress((int)values[1]);
            appsProgress.setProgress((int)values[2]);
            appsStatusText.setText(values[0] + " " + values[1] + "/" + values[2]);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if (n > 0) {
                appsDone.setVisibility(View.VISIBLE);
                appsStatusText.setText(R.string.please_wait);
                appsProgress.setIndeterminate(true);
            }

            startService(new Intent(ExtraBackupsProgress.this, RestoreService.class));

            RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(ExtraBackupsProgress.this, startTime, installScriptPath, restoreDataScriptPath, numberOfApps);
            RestoreService.ROOT_RESTORE_TASK.execute((File)o);
        }
    }
}
