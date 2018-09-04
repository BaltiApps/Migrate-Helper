package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import java.util.Calendar;
import java.util.Objects;

public class ExtraBackupsProgress extends AppCompatActivity {

    TextView headTitle;
    ProgressBar headProgressBar;

    LinearLayout contacts;
    ImageView contactsDone;
    ProgressBar contactsProgress;

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

    BroadcastReceiver startRestoreFromExtraBackups;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    ContactsPacket contactsPackets[];
    int contactCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.extra_backup_progress);

        headTitle = findViewById(R.id.extra_backup_progress_title);
        headProgressBar = findViewById(R.id.extra_backup_progress_head_progress);

        contacts = findViewById(R.id.extra_backup_progress_item_contacts);
        contactsProgress = findViewById(R.id.extra_backup_progress_item_contacts_progress);
        contactsDone = findViewById(R.id.extra_backup_progress_item_contacts_done);

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
        }
        else {
            isShowContacts = false;
        }

        if (numberOfApps > 0) totalTasks++;

    }

    void triggerRootRestoreTask(){

        if (numberOfApps > 0) {
            headTitle.setText(R.string.applications);
            headProgressBar.setProgress(++intProgressTask);
        }

        startService(new Intent(this, RestoreService.class));

        RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(this, timeInMillis(), installScriptPath, restoreDataScriptPath, extraSelectBoolean);
        RestoreService.ROOT_RESTORE_TASK.setNumberOfAppJobs(numberOfApps);
        RestoreService.ROOT_RESTORE_TASK.execute(getJsonFromDataPackets);
    }

    void triggerRestoreProcessStart(){

        try {

            headProgressBar.setMax(totalTasks);
            contactsPackets = getJsonFromDataPackets.contactPackets;

            if (numberOfApps > 0) applications.setVisibility(View.VISIBLE);

            if (extraSelectBoolean) {

                if (isShowContacts) contacts.setVisibility(View.VISIBLE);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACTS_RESTORE){
            nextContact();
        }
    }

    private void restoreContacts() {

        if (isShowContacts) {

            headProgressBar.setProgress(++intProgressTask);
            headTitle.setText(R.string.contacts);

            contactsProgress.setVisibility(View.VISIBLE);
            contactsDone.setVisibility(View.GONE);

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
                            // next process

                            contactsProgress.setVisibility(View.GONE);
                            contactsDone.setVisibility(View.VISIBLE);

                            triggerRootRestoreTask();
                        }
                    })
                    .setCancelable(false)
                    .show();
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

            // next process

            contactsProgress.setVisibility(View.GONE);
            contactsDone.setVisibility(View.VISIBLE);

            triggerRootRestoreTask();
        }
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
}
