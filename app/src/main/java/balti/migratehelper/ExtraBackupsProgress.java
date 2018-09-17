package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

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

    LinearLayout applications;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.extra_backup_progress);

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

        applications = findViewById(R.id.extra_backup_progress_item_applications);

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
        }
        else {
            isShowContacts = false;
            isShowSms = false;
        }

        if (numberOfApps > 0) totalTasks++;

    }

    void triggerRootRestoreTask(){

        if (numberOfApps > 0) {
            headTitle.setText(R.string.applications);
            headProgressBar.setProgress(++intProgressTask);
        }

        startService(new Intent(this, RestoreService.class));

        RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(this, timeInMillis(), installScriptPath, restoreDataScriptPath);
        RestoreService.ROOT_RESTORE_TASK.setNumberOfAppJobs(numberOfApps);
        RestoreService.ROOT_RESTORE_TASK.execute(getJsonFromDataPackets);
    }

    void triggerRestoreProcessStart(){

        try {

            headProgressBar.setMax(totalTasks);

            contactsPackets = getJsonFromDataPackets.contactPackets;
            smsPacket = getJsonFromDataPackets.smsPackets;

            if (numberOfApps > 0) applications.setVisibility(View.VISIBLE);

            if (extraSelectBoolean) {

                if (isShowContacts) contactsView.setVisibility(View.VISIBLE);
                if (isShowSms) smsView.setVisibility(View.VISIBLE);

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
            contactsDone.setVisibility(View.GONE);
            contactsCancel.setVisibility(View.GONE);

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

            smsProgress.setVisibility(View.VISIBLE);
            smsDone.setVisibility(View.GONE);
            smsCancel.setVisibility(View.GONE);

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

                            smsProgress.setVisibility(View.GONE);
                            smsDone.setVisibility(View.GONE);
                            smsCancel.setVisibility(View.VISIBLE);

                            triggerRootRestoreTask();

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

            triggerRootRestoreTask();

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACTS_RESTORE){
            nextContact();
        }
        else if (requestCode == SET_THIS_AS_DEFAULT_SMS_APP){
            nextSms();
        }
        else if (requestCode == SET_ORIGINAL_APP_AS_DEFAULT_SMS_APP) {
            triggerRootRestoreTask();
        }
    }


    @Override
    public void onDBRestoreComplete(int code) {
        if (code == SMS_RESTORE_JOB){
            // next process 1

            setDefaultSms(actualDefaultSmsAppName, SET_ORIGINAL_APP_AS_DEFAULT_SMS_APP);

            smsProgress.setVisibility(View.GONE);
            smsDone.setVisibility(View.VISIBLE);
            smsCancel.setVisibility(View.GONE);
        }
    }
}
