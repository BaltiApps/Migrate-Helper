package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Vector;

import static balti.migratehelper.CommonTools.DEBUG_TAG;
import static balti.migratehelper.GetJsonFromData.APP_CHECK;
import static balti.migratehelper.GetJsonFromData.DATA_CHECK;
import static balti.migratehelper.GetJsonFromData.IS_PERMISSIBLE;
import static balti.migratehelper.GetJsonFromData.PERM_CHECK;

public class AppSelector extends AppCompatActivity implements OnConvertMetadataToJSON, OnCheck, CompoundButton.OnCheckedChangeListener {

    ImageButton back;
    TextView title;
    CheckBox appAllSelect, dataAllSelect, permissionsAllSelect;
    RelativeLayout waitingLayout;
    TextView waitingStatusMessage;
    TextView waitingMessageDesc;
    ImageButton clearAll;
    ImageButton selectAll;
    Button actionButton;
    ScrollView restoreContent;
    LinearLayout appCheckboxBar;
    ListView appList;
    LinearLayout extrasBar;
    CheckBox extrasSelect;

    ProgressBar justAProgress;
    ImageView errorIcon;

    AppListAdapter adapter;

    Process checkSu;

    private String busyboxBinaryFilePath = "";
    private String installScriptPath = "";
    private String restoreDataScriptPath = "";

    static String TEMP_DIR_NAME = "/data/balti.migrate";
    static String METADATA_HOLDER_DIR = "/sdcard/.migrate_mtd/";

    private String initError;

    private static int SUCCESS = 0;
    private static int CODE_ERROR = 1;
    private static int SCRIPT_ERROR = 2;

    int numberOfApps = 0;
    boolean intentSelectAll = false;
    boolean anyAppSelected = true;

    RootCopyTask rootCopyTask;
    GetJsonFromData getJsonFromData;
    AppUpdate appUpdateTask;

    GetJsonFromDataPackets mainGetJsonFromDataPackets = null;
    boolean extraSelectBoolean = true;

    BroadcastReceiver extraBackupsProgressReadyReceiver;

    class RootCopyTask extends AsyncTask{

        int rootCopyResult;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            waitingStatusMessage.setText(R.string.requesting_root);
            waitingMessageDesc.setText(R.string.initMessageShort);

            actionButton.setText(android.R.string.cancel);
            actionButton.setBackground(getDrawable(R.drawable.cancel_root_request));
            actionButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    try {
                        checkSu.destroy();
                    }catch (Exception ignored){}

                    try {
                        rootCopyTask.cancel(true);
                    }catch (Exception ignored){}

                    finish();
                }
            });
            actionButton.setVisibility(View.VISIBLE);

            restoreContent.setVisibility(View.GONE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            unpackBinaries();
            rootCopyResult = rootCopy();

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if (rootCopyResult == SUCCESS){
                waitingStatusMessage.setText(R.string.reading_metadata);
                waitingMessageDesc.setText("");
                getJsonFromData = new GetJsonFromData(AppSelector.this, waitingMessageDesc);
                getJsonFromData.execute(METADATA_HOLDER_DIR);
            }
            else if (rootCopyResult == SCRIPT_ERROR){
                showError(getString(R.string.are_you_rooted), getString(R.string.are_you_rooted_desc));
                waitingMessageDesc.append("\n\n" + initError);
            }
            else {
                showError(getString(R.string.code_error), initError);
            }

        }
    }

    class AppUpdate extends AsyncTask{

        GetJsonFromDataPackets getJsonFromDataPackets;

        AppUpdate(GetJsonFromDataPackets getJsonFromDataPackets){
            this.getJsonFromDataPackets = getJsonFromDataPackets;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitingLayout.setVisibility(View.VISIBLE);
            restoreContent.setVisibility(View.GONE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            adapter = new AppListAdapter(AppSelector.this, getJsonFromDataPackets.jsonAppPackets);
            return adapter;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            mainGetJsonFromDataPackets = getJsonFromDataPackets;

            if (o != null){
                waitingLayout.setVisibility(View.GONE);
                restoreContent.setVisibility(View.VISIBLE);

                if (mainGetJsonFromDataPackets.jsonAppPackets.size() > 0)
                appList.setAdapter(adapter);

                if (mainGetJsonFromDataPackets.contactPackets.length != 0 || mainGetJsonFromDataPackets.smsPackets.length != 0
                        || mainGetJsonFromDataPackets.callsPackets.length != 0 || mainGetJsonFromDataPackets.dpiPacket.dpiFile != null
                        || mainGetJsonFromDataPackets.keyboardPacket.keyboardFile != null){
                    extrasBar.setVisibility(View.VISIBLE);
                    extrasSelect.setChecked(true);
                    extrasBar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new ExtrasUpdate(mainGetJsonFromDataPackets).execute();
                        }
                    });
                }
                else {
                    extrasBar.setVisibility(View.GONE);
                }

                if (mainGetJsonFromDataPackets.jsonAppPackets.size() != 0){
                    appCheckboxBar.setVisibility(View.VISIBLE);
                    appList.setVisibility(View.VISIBLE);
                }
                else {
                    appCheckboxBar.setVisibility(View.GONE);
                    appList.setVisibility(View.GONE);
                }

                onCheck(mainGetJsonFromDataPackets.jsonAppPackets);

                actionButton.setText(R.string.restore);
                actionButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_next, 0);
                actionButton.setBackground(getResources().getDrawable(R.drawable.next));
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (anyAppSelected || extrasSelect.isChecked()) {
                            waitingStatusMessage.setText(R.string.please_wait);
                            actionButton.setVisibility(View.INVISIBLE);

                            waitingLayout.setVisibility(View.VISIBLE);
                            restoreContent.setVisibility(View.GONE);

                            extraSelectBoolean = extrasSelect.isChecked();

                            new AlertDialog.Builder(AppSelector.this)
                                    .setTitle(R.string.do_not_use)
                                    .setMessage(R.string.do_not_use_desc)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startActivity(new Intent(AppSelector.this, ExtraBackupsProgress.class));
                                        }
                                    })
                                    .setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();

                        }
                    }
                });

            }
            else {
                showError(getString(R.string.code_error), getString(R.string.null_adapter));
            }
        }
    }

    class ExtrasUpdate extends AsyncTask{

        AlertDialog ad;
        View masterView;
        LinearLayout holder;
        ProgressBar progressBar;

        GetJsonFromDataPackets getJsonFromDataPackets;
        ContactsPacket[] contactsPackets;
        SmsPacket[] smsPackets;
        CallsPacket[] callsPackets;
        DpiPacket dpiPacket;
        KeyboardPacket keyboardPacket;

        public ExtrasUpdate(GetJsonFromDataPackets getJsonFromDataPackets) {
            this.getJsonFromDataPackets = getJsonFromDataPackets;
            contactsPackets = getJsonFromDataPackets.contactPackets;
            smsPackets = getJsonFromDataPackets.smsPackets;
            callsPackets = getJsonFromDataPackets.callsPackets;
            dpiPacket = getJsonFromDataPackets.dpiPacket;
            keyboardPacket = getJsonFromDataPackets.keyboardPacket;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            masterView = View.inflate(AppSelector.this, R.layout.extras_picker, null);
            holder = masterView.findViewById(R.id.extras_picker_item_holder);
            progressBar = masterView.findViewById(R.id.extra_picker_round_progress);

            ad = new AlertDialog.Builder(AppSelector.this)
                    .setView(masterView)
                    .create();

            ad.show();

            holder.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            ContactsPacket[] tempContactPackets = new ContactsPacket[contactsPackets.length];
            ArrayList<View> contactsViewItems = new ArrayList<>(0);

            SmsPacket[] tempSmsPackets = new SmsPacket[smsPackets.length];
            ArrayList<View> smsViewItems = new ArrayList<>(0);

            CallsPacket[] tempCallsPackets = new CallsPacket[callsPackets.length];
            ArrayList<View> callsViewItems = new ArrayList<>(0);

            for (int j = 0; j < contactsPackets.length; j++){
                tempContactPackets[j] = new ContactsPacket(contactsPackets[j].vcfFile, contactsPackets[j].selected);
            }

            for (final ContactsPacket packet : tempContactPackets){
                View cView = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = cView.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_contact_icon);

                TextView tv = cView.findViewById(R.id.extra_item_name);
                tv.setText(packet.vcfFile.getName());

                contactsViewItems.add(cView);
            }

            for (int j = 0; j < smsPackets.length; j++){
                tempSmsPackets[j] = new SmsPacket(smsPackets[j].smsDBFile, smsPackets[j].selected);
            }

            for (final SmsPacket packet : tempSmsPackets){
                View sView = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = sView.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_sms_icon);

                TextView tv = sView.findViewById(R.id.extra_item_name);
                tv.setText(packet.smsDBFile.getName());

                smsViewItems.add(sView);
            }

            for (int j = 0; j < callsPackets.length; j++){
                tempCallsPackets[j] = new CallsPacket(callsPackets[j].callsDBFile, callsPackets[j].selected);
            }

            for (final CallsPacket packet : tempCallsPackets){
                View clView = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = clView.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_call_log_icon);

                TextView tv = clView.findViewById(R.id.extra_item_name);
                tv.setText(packet.callsDBFile.getName());

                callsViewItems.add(clView);
            }

            View dpiViewItem = null;
            if (dpiPacket.dpiFile != null) {
                dpiViewItem = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = dpiViewItem.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_dpi_icon);

                TextView tv = dpiViewItem.findViewById(R.id.extra_item_name);
                tv.setText(R.string.dpi);
            }

            DpiPacket tempDpiPacket = new DpiPacket(dpiPacket.dpiFile, dpiPacket.selected);

            View keyboardViewItem = null;
            if (keyboardPacket.keyboardFile != null) {
                keyboardViewItem = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = keyboardViewItem.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_keyboard_icon);

                TextView tv = keyboardViewItem.findViewById(R.id.extra_item_name);
                tv.setText(R.string.keyboard);
            }

            KeyboardPacket tempKeyboardPacket = new KeyboardPacket(keyboardPacket.keyboardFile, keyboardPacket.selected);

            return new Object[]{tempContactPackets, contactsViewItems, tempSmsPackets, smsViewItems, tempCallsPackets, callsViewItems, tempDpiPacket, dpiViewItem,
                    tempKeyboardPacket, keyboardViewItem};
        }

        @Override
        protected void onPostExecute(final Object o) {
            super.onPostExecute(o);

            if (holder.getChildCount() > 0)
                holder.removeAllViews();

            Object received[] = (Object[])o;

            final ContactsPacket[] tempContactPackets = (ContactsPacket[])received[0];
            ArrayList<View> contactViews = (ArrayList<View>)received[1];
            for (int i = 0; i < contactViews.size(); i++) {
                View v = contactViews.get(i);

                CheckBox cb = v.findViewById(R.id.extras_item_select);
                cb.setChecked(tempContactPackets[i].selected);

                final int finalI = i;
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        tempContactPackets[finalI].selected = b;
                    }
                });

                holder.addView(v);
            }

            final SmsPacket[] tempSmsPackets = (SmsPacket[])received[2];
            ArrayList<View> smsViews = (ArrayList<View>)received[3];
            for (int i = 0; i < smsViews.size(); i++) {
                View v = smsViews.get(i);

                CheckBox cb = v.findViewById(R.id.extras_item_select);
                cb.setChecked(tempSmsPackets[i].selected);

                final int finalI = i;
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        tempSmsPackets[finalI].selected = b;
                    }
                });

                holder.addView(v);
            }

            final CallsPacket[] tempCallsPackets = (CallsPacket[])received[4];
            ArrayList<View> callsViews = (ArrayList<View>)received[5];
            for (int i = 0; i < callsViews.size(); i++) {
                View v = callsViews.get(i);

                CheckBox cb = v.findViewById(R.id.extras_item_select);
                cb.setChecked(tempCallsPackets[i].selected);

                final int finalI = i;
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        tempCallsPackets[finalI].selected = b;
                    }
                });

                holder.addView(v);
            }

            final DpiPacket tempDpiPacket[] = new DpiPacket[]{null};

            if (received[7] != null) {
                tempDpiPacket[0] = (DpiPacket) received[6];
                View dpiViewItem = (View)received[7];

                CheckBox dpi_cb = dpiViewItem.findViewById(R.id.extras_item_select);
                dpi_cb.setChecked(tempDpiPacket[0].selected);

                dpi_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        tempDpiPacket[0].selected = b;
                    }
                });

                holder.addView(dpiViewItem);
            }

            final KeyboardPacket tempKeyboardPacket[] = new KeyboardPacket[]{null};

            if (received[9] != null) {
                tempKeyboardPacket[0] = (KeyboardPacket) received[8];
                View keyboardViewItem = (View)received[9];

                CheckBox keyboard_cb = keyboardViewItem.findViewById(R.id.extras_item_select);
                keyboard_cb.setChecked(tempKeyboardPacket[0].selected);

                keyboard_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        tempKeyboardPacket[0].selected = b;
                    }
                });

                holder.addView(keyboardViewItem);
            }


            holder.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            Button ok = masterView.findViewById(R.id.extras_picker_ok);
            Button cancel = masterView.findViewById(R.id.extras_picker_cancel);

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (contactsPackets.length == 0 && smsPackets.length == 0
                            && callsPackets.length == 0 && dpiPacket.dpiFile == null && keyboardPacket.keyboardFile == null)
                        return;

                    boolean anyContact;
                    boolean anySms;
                    boolean anyCalls;
                    boolean isDpi;
                    boolean isKeyboard;

                    if (tempContactPackets.length > 0) {
                         anyContact= tempContactPackets[0].selected;
                        for (int j = 0; j < contactsPackets.length; j++) {
                            contactsPackets[j].selected = tempContactPackets[j].selected;
                            contactsPackets[j].vcfFile = tempContactPackets[j].vcfFile;
                            anyContact = anyContact || contactsPackets[j].selected;
                        }
                    }
                    else {
                        anyContact = false;
                    }

                    if (tempSmsPackets.length > 0) {
                        anySms= tempSmsPackets[0].selected;
                        for (int j = 0; j < smsPackets.length; j++) {
                            smsPackets[j].selected = tempSmsPackets[j].selected;
                            smsPackets[j].smsDBFile = tempSmsPackets[j].smsDBFile;
                            anySms = anySms || smsPackets[j].selected;
                        }
                    }
                    else {
                        anySms = false;
                    }

                    if (tempCallsPackets.length > 0) {
                        anyCalls= tempCallsPackets[0].selected;
                        for (int j = 0; j < callsPackets.length; j++) {
                            callsPackets[j].selected = tempCallsPackets[j].selected;
                            callsPackets[j].callsDBFile = tempCallsPackets[j].callsDBFile;
                            anyCalls = anyCalls || callsPackets[j].selected;
                        }
                    }
                    else {
                        anyCalls = false;
                    }

                    if (tempDpiPacket[0] != null){
                        isDpi = tempDpiPacket[0].selected;
                        dpiPacket.dpiFile = tempDpiPacket[0].dpiFile;
                        dpiPacket.selected = tempDpiPacket[0].selected;
                    }
                    else {
                        isDpi = false;
                    }

                    if (tempKeyboardPacket[0] != null){
                        isKeyboard = tempKeyboardPacket[0].selected;
                        keyboardPacket.keyboardFile = tempKeyboardPacket[0].keyboardFile;
                        keyboardPacket.selected = tempKeyboardPacket[0].selected;
                    }
                    else {
                        isKeyboard = false;
                    }

                    extrasSelect.setChecked(anyContact || anySms || anyCalls || isDpi || isKeyboard);
                    ad.dismiss();
                }
            });

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ad.dismiss();
                }
            });
        }
    }

    class ReadPackageData extends AsyncTask{

        GetJsonFromDataPackets gjfdp;
        int originating_sdk = 0;

        ReadPackageData(GetJsonFromDataPackets getJsonFromDataPackets){
            gjfdp = getJsonFromDataPackets;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitingMessageDesc.setText(R.string.reading_package_data);
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            File package_data = gjfdp.package_data;
            String line;

            try {

                BufferedReader reader = new BufferedReader(new FileReader(package_data));

                while ((line = reader.readLine()) != null){

                    Log.d(DEBUG_TAG, line);

                    String data[] = line.split(" ");
                    if (data.length == 2 && data[0].equals("sdk")){
                        originating_sdk = Integer.parseInt(data[1].trim());
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            int current_sdk = Build.VERSION.SDK_INT;

            Log.d(DEBUG_TAG, current_sdk + " " + originating_sdk);

            if (originating_sdk != 0 && originating_sdk != current_sdk){

                String message = getString(R.string.restore_in_different_version_warning) + "\n\n" +
                        getString(R.string.originating_sdk) + ": " + originating_sdk + "\n" +
                        getString(R.string.current_sdk) + ": " + current_sdk;

                new AlertDialog.Builder(AppSelector.this)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (intentSelectAll){
                                    waitingStatusMessage.setText(R.string.selecting_all);
                                    actionButton.setVisibility(View.INVISIBLE);

                                    numberOfApps = gjfdp.jsonAppPackets.size();
                                    mainGetJsonFromDataPackets = gjfdp;
                                    extraSelectBoolean = true;

                                    startActivity(new Intent(AppSelector.this, ExtraBackupsProgress.class));
                                }
                                else {
                                    appUpdateTask = new AppUpdate(gjfdp);
                                    appUpdateTask.execute();
                                }
                            }
                        })
                        .show();

            }
            else {

                if (intentSelectAll){
                    waitingStatusMessage.setText(R.string.selecting_all);
                    actionButton.setVisibility(View.INVISIBLE);

                    numberOfApps = gjfdp.jsonAppPackets.size();
                    mainGetJsonFromDataPackets = gjfdp;
                    extraSelectBoolean = true;

                    startActivity(new Intent(AppSelector.this, ExtraBackupsProgress.class));
                }
                else {
                    appUpdateTask = new AppUpdate(gjfdp);
                    appUpdateTask.execute();
                }
            }

        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_selector);

        back = findViewById(R.id.app_selecter_back_button);
        title = findViewById(R.id.app_selector_title);

        String externalPasteDir = getExternalCacheDir().getAbsolutePath() + "/Migrate/";
        METADATA_HOLDER_DIR = "/sdcard" + externalPasteDir.substring(externalPasteDir.indexOf("/Android"));

        appAllSelect = findViewById(R.id.appAllSelect);
        dataAllSelect = findViewById(R.id.dataAllSelect);
        permissionsAllSelect = findViewById(R.id.permissionsAllSelect);

        waitingLayout = findViewById(R.id.waiting_layout);
        waitingStatusMessage = findViewById(R.id.waiting_status_text);
        waitingMessageDesc = findViewById(R.id.waiting_messages);

        justAProgress = findViewById(R.id.just_a_progress);
        errorIcon = findViewById(R.id.app_selector_error_icon);

        clearAll = findViewById(R.id.clear_all);
        selectAll = findViewById(R.id.select_all);

        actionButton = findViewById(R.id.restore_selected);

        restoreContent = findViewById(R.id.restore_content);
        appCheckboxBar = findViewById(R.id.app_checkbox_bar);
        appList = findViewById(R.id.app_list);
        extrasBar = findViewById(R.id.extras_bar);
        extrasSelect = findViewById(R.id.extras_select);

        METADATA_HOLDER_DIR = new File(METADATA_HOLDER_DIR).getAbsolutePath() + "/";

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("all?", true)){
            title.setText(R.string.everything);
            intentSelectAll = true;
            restoreContent.setVisibility(View.GONE);
            clearAll.setVisibility(View.INVISIBLE);
            selectAll.setVisibility(View.INVISIBLE);
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapter != null && dataAllSelect.isEnabled()) {
                    dataAllSelect.setChecked(true);
                    permissionsAllSelect.setChecked(true);
                }
            }
        });

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapter != null && dataAllSelect.isEnabled()) {
                    dataAllSelect.setChecked(true);
                    dataAllSelect.setChecked(false);
                    appAllSelect.setChecked(false);
                    permissionsAllSelect.setChecked(false);
                }
            }
        });

        extraBackupsProgressReadyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ExtraBackupsProgress.setData(mainGetJsonFromDataPackets, numberOfApps,
                        installScriptPath, restoreDataScriptPath, extraSelectBoolean, METADATA_HOLDER_DIR);
                LocalBroadcastManager.getInstance(AppSelector.this).sendBroadcast(new Intent("startRestoreFromExtraBackups"));
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(extraBackupsProgressReadyReceiver, new IntentFilter("extraBackupsProgressReady"));

        if (RestoreService.ROOT_RESTORE_TASK != null && RestoreService.ROOT_RESTORE_TASK.getStatus() == AsyncTask.Status.RUNNING){
            Intent toSendIntent = new Intent(AppSelector.this, ProgressActivity.class);
            startActivity(toSendIntent);
            finish();
        }
        else {
            rootCopyTask = new RootCopyTask();
            rootCopyTask.execute();
        }
    }


    private void unpackBinaries(){

        String cpu_abi = Build.SUPPORTED_ABIS[0];

        CommonTools commonTools = new CommonTools(this);

        installScriptPath = commonTools.unpackAssetToInternal("installScript.sh", "installScript.sh");
        restoreDataScriptPath = commonTools.unpackAssetToInternal("restoreDataScript.sh", "restoreDataScript.sh");

        if (cpu_abi.equals("armeabi-v7a") || cpu_abi.equals("arm64-v8a")) {
            busyboxBinaryFilePath = commonTools.unpackAssetToInternal("busybox", "busybox");
        }
        else if (cpu_abi.equals("x86") || cpu_abi.equals("x86_64")) {
            busyboxBinaryFilePath = commonTools.unpackAssetToInternal("busybox-x86", "busybox");
        }

    }

    int rootCopy(){

        unpackBinaries();

        justAProgress.setVisibility(View.VISIBLE);
        errorIcon.setVisibility(View.INVISIBLE);

        File initSu = new File(getFilesDir(), "initSu.sh");

        String moveCommand = "#!sbin/sh\n\n" +
                "pm grant " + getPackageName() + " android.permission.DUMP\n" +
                "pm grant " + getPackageName() + " android.permission.PACKAGE_USAGE_STATS\n" +
                "pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS\n" +
                "mv -f " + busyboxBinaryFilePath + " " + TEMP_DIR_NAME + "/busybox\n" +
                "rm -rf " + METADATA_HOLDER_DIR + "\n" +
                "mkdir -p " + METADATA_HOLDER_DIR + "\n" +
                "cp " + TEMP_DIR_NAME + "/*.json " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/*.vcf " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/*.sms.db " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/*.calls.db " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/*.perm " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/screen.dpi " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/default.kyb " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/package-data " + METADATA_HOLDER_DIR + " 2>/dev/null\n" +
                "echo ROOT_OK\n" +
                "rm " + initSu.getAbsolutePath() + "\n";

        initError = "";

        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(initSu));
            BufferedReader reader = new BufferedReader(new StringReader(moveCommand));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }
            writer.close();

            checkSu = Runtime.getRuntime().exec("su");

            BufferedWriter suWriter = new BufferedWriter(new OutputStreamWriter(checkSu.getOutputStream()));
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(checkSu.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(checkSu.getErrorStream()));

            suWriter.write("sh " + initSu.getAbsolutePath() + "\n");
            suWriter.write("exit\n");
            suWriter.flush();

            String outputLine, output = "";
            while ((outputLine = outputReader.readLine()) != null)
                output = output + outputLine;

            String errLine;
            while ((errLine = err.readLine()) != null) {
                if (errLine.contains("busybox") || errLine.contains("*.json"))
                    continue;
                initError = initError + errLine + "\n";
            }

            if (!output.equals("") && output.contains("ROOT_OK") && initError.equals("")){
                return SUCCESS;
            }
            else {
                return SCRIPT_ERROR;
            }
        }
        catch (Exception e){
            initError = initError + e.getMessage() + "\n";
            return CODE_ERROR;
        }
    }

    @Override
    public void onConvertMetadataToJSON(GetJsonFromDataPackets getJsonFromDataPackets, String error) {

        if (!error.startsWith("")){
            showError(getString(R.string.code_error), error);
            return;
        }

        Vector<JSONObject> appData = getJsonFromDataPackets.jsonAppPackets;

        if (getJsonFromDataPackets.contactPackets.length == 0 && getJsonFromDataPackets.smsPackets.length == 0 && appData.size() == 0 && getJsonFromDataPackets.callsPackets.length == 0){
            showError(getString(R.string.nothing_to_restore), getString(R.string.no_metadata_found));
            return;
        }

        new ReadPackageData(getJsonFromDataPackets).execute();

        /*if (intentSelectAll){
            waitingStatusMessage.setText(R.string.selecting_all);
            actionButton.setVisibility(View.INVISIBLE);

            numberOfApps = appData.size();
            mainGetJsonFromDataPackets = getJsonFromDataPackets;
            extraSelectBoolean = true;

            startActivity(new Intent(AppSelector.this, ExtraBackupsProgress.class));
        }
        else {
            appUpdateTask = new AppUpdate(getJsonFromDataPackets);
            appUpdateTask.execute();
        }*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(extraBackupsProgressReadyReceiver);
        }catch (Exception ignored){}
        cancelAllProcesses();
    }

    void cancelAllProcesses(){
        try {
            rootCopyTask.cancel(true);
        }
        catch (Exception ignored){}

        try {
            getJsonFromData.cancel(true);
        }
        catch (Exception ignored){}

        try {
            appUpdateTask.cancel(true);
        }
        catch (Exception ignored){}
    }

    void showError(String mainMessage, String description){
        justAProgress.setVisibility(View.INVISIBLE);
        errorIcon.setVisibility(View.VISIBLE);
        waitingStatusMessage.setText(mainMessage);
        waitingMessageDesc.setText(description);
        actionButton.setText(R.string.close);
        actionButton.setBackground(getDrawable(R.drawable.next));
        actionButton.setVisibility(View.VISIBLE);

        restoreContent.setVisibility(View.GONE);
    }

    @Override
    public void onCheck(Vector<JSONObject> appList) {
        boolean app, data, permissions = false, isPermissible = false;
        boolean thisEnable;
        boolean enable = false;

        boolean isAppAllowed, isDataAllowed, isPermAllowed;

        numberOfApps = 0;

        if (appList.size() > 0) {
            app = data = permissions = true;
            try { isPermissible = appList.get(0).getBoolean(IS_PERMISSIBLE); } catch (JSONException e) { e.printStackTrace(); }
        }
        else app = data = permissions = false;
        for (int i = 0; i < appList.size(); i++) {
            try {

                isAppAllowed = !appList.elementAt(i).getString("apk").equals("NULL");
                isDataAllowed = !appList.elementAt(i).getString("data").equals("NULL");
                isPermAllowed = appList.elementAt(i).getBoolean("permissions");

                isPermissible = isPermissible || (isPermAllowed && appList.elementAt(i).getBoolean(IS_PERMISSIBLE));

                if (isAppAllowed) app = app && appList.elementAt(i).getBoolean(APP_CHECK);
                if (isDataAllowed) data = data && appList.elementAt(i).getBoolean(DATA_CHECK);
                if (isPermAllowed && appList.elementAt(i).getBoolean(IS_PERMISSIBLE)) {
                    permissions = permissions && appList.elementAt(i).getBoolean(PERM_CHECK);
                }

                thisEnable = appList.elementAt(i).getBoolean(APP_CHECK) || appList.elementAt(i).getBoolean(DATA_CHECK) || appList.elementAt(i).getBoolean(PERM_CHECK);
                enable = enable || thisEnable;
                if (thisEnable) numberOfApps++;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        anyAppSelected = enable;

        dataAllSelect.setOnCheckedChangeListener(null);
        dataAllSelect.setChecked(data);
        dataAllSelect.setOnCheckedChangeListener(this);

        appAllSelect.setOnCheckedChangeListener(null);
        appAllSelect.setChecked(app);

        if (dataAllSelect.isChecked())
            appAllSelect.setEnabled(false);
        else appAllSelect.setEnabled(true);

        appAllSelect.setOnCheckedChangeListener(this);

        permissions = permissions && isPermissible;

        permissionsAllSelect.setEnabled(isPermissible);

        permissionsAllSelect.setOnCheckedChangeListener(null);
        permissionsAllSelect.setChecked(permissions);
        permissionsAllSelect.setOnCheckedChangeListener(this);

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (compoundButton == permissionsAllSelect){
            adapter.checkAllPermissions(b);
            adapter.notifyDataSetChanged();
        }
        else if (compoundButton == appAllSelect) {
            adapter.checkAllApp(b);
            adapter.notifyDataSetChanged();
        } else if (compoundButton == dataAllSelect) {
            if (b){
                appAllSelect.setChecked(true);
                appAllSelect.setEnabled(false);
            }
            else {
                appAllSelect.setEnabled(true);
            }
            adapter.checkAllData(b);
            adapter.notifyDataSetChanged();
        }
    }



}
