package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Vector;

import static balti.migratehelper.GetJsonFromData.APP_CHECK;
import static balti.migratehelper.GetJsonFromData.DATA_CHECK;

public class AppSelector extends AppCompatActivity implements OnConvertMetadataToJSON, OnCheck, CompoundButton.OnCheckedChangeListener {

    ImageButton back;
    TextView title;
    CheckBox appAllSelect, dataAllSelect;
    RelativeLayout waitingLayout;
    TextView waitingStatusMessage;
    TextView waitingMessageDesc;
    ImageButton clearAll;
    ImageButton selectAll;
    TextView actionButton;
    ScrollView restoreContent;
    LinearLayout appCheckboxBar;
    ListView appList;
    LinearLayout extrasBar;
    CheckBox extrasSelect;

    ProgressBar justAProgress;
    ImageView errorIcon;

    AppListAdapter adapter;

    private String busyboxBinaryFilePath = "";
    private String installScriptPath = "";
    private String restoreDataScriptPath = "";

    static String TEMP_DIR_NAME = "/data/balti.migrate";

    private String mtdDirName;
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
            actionButton.setBackground(getDrawable(R.drawable.ic_cancel_button));
            actionButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
                getJsonFromData = new GetJsonFromData(AppSelector.this, ".json", waitingMessageDesc);
                getJsonFromData.execute(mtdDirName);
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

                if (mainGetJsonFromDataPackets.contactPackets.length != 0){
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

                            startActivity(new Intent(AppSelector.this, ExtraBackupsProgress.class));
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

        public ExtrasUpdate(GetJsonFromDataPackets getJsonFromDataPackets) {
            this.getJsonFromDataPackets = getJsonFromDataPackets;
            contactsPackets = getJsonFromDataPackets.contactPackets;
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
            ArrayList<View> items = new ArrayList<>(0);

            for (int j = 0; j < contactsPackets.length; j++){
                tempContactPackets[j] = new ContactsPacket(contactsPackets[j].vcfFile, contactsPackets[j].selected);
            }

            for (final ContactsPacket packet : tempContactPackets){
                View cView = View.inflate(AppSelector.this, R.layout.extra_item, null);
                ImageView icon = cView.findViewById(R.id.extra_item_icon);
                icon.setImageResource(R.drawable.ic_contact_icon);

                TextView tv = cView.findViewById(R.id.extra_item_name);
                tv.setText(packet.vcfFile.getName());

                CheckBox cb = cView.findViewById(R.id.extras_item_select);
                cb.setChecked(packet.selected);

                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        packet.selected = b;
                    }
                });

                items.add(cView);
            }

            return new Object[]{tempContactPackets, items};
        }

        @Override
        protected void onPostExecute(final Object o) {
            super.onPostExecute(o);

            if (holder.getChildCount() > 0)
                holder.removeAllViews();

            Object received[] = (Object[])o;

            final ContactsPacket[] tempContactPackets = (ContactsPacket[])received[0];
            for (View v : (ArrayList<View>)received[1])
                holder.addView(v);

            holder.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            Button ok = masterView.findViewById(R.id.extras_picker_ok);
            Button cancel = masterView.findViewById(R.id.extras_picker_cancel);

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (contactsPackets.length == 0)
                        return;

                    boolean anyContact = tempContactPackets[0].selected;

                    for (int j = 0; j < contactsPackets.length; j++) {
                        contactsPackets[j].selected = tempContactPackets[j].selected;
                        contactsPackets[j].vcfFile = tempContactPackets[j].vcfFile;
                        anyContact = anyContact || contactsPackets[j].selected;
                    }

                    extrasSelect.setChecked(anyContact);
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_selector);

        back = findViewById(R.id.app_selecter_back_button);
        title = findViewById(R.id.app_selector_title);

        appAllSelect = findViewById(R.id.appAllSelect);
        dataAllSelect = findViewById(R.id.dataAllSelect);

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

        mtdDirName = new File(getExternalCacheDir(), "metadataFiles").getAbsolutePath() + "/";

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
                }
            }
        });

        extraBackupsProgressReadyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ExtraBackupsProgress.setData(mainGetJsonFromDataPackets, numberOfApps, installScriptPath, restoreDataScriptPath, extraSelectBoolean);
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

        busyboxBinaryFilePath = unpackAssetToInternal("busybox", "busybox");
        installScriptPath = unpackAssetToInternal("installScript.sh", "installScript.sh");
        restoreDataScriptPath = unpackAssetToInternal("restoreDataScript.sh", "restoreDataScript.sh");

    }

    private String unpackAssetToInternal(String assetFileName, String targetFileName){

        AssetManager assetManager = getAssets();
        File unpackFile = new File(getFilesDir(), targetFileName);
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

    int rootCopy(){

        unpackBinaries();

        justAProgress.setVisibility(View.VISIBLE);
        errorIcon.setVisibility(View.INVISIBLE);

        File initSu = new File(getFilesDir(), "initSu.sh");

        String moveCommand = "#!sbin/sh\n\n" +
                "mv -f " + busyboxBinaryFilePath + " " + TEMP_DIR_NAME + "/busybox\n" +
                "rm -rf " + mtdDirName + "\n" +
                "mkdir -p " + mtdDirName + "\n" +
                "cp " + TEMP_DIR_NAME + "/*.json " + mtdDirName + " 2>/dev/null\n" +
                "cp " + TEMP_DIR_NAME + "/*.vcf " + mtdDirName + " 2>/dev/null\n" +
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

            String cmd = "su -c sh " + initSu.getAbsolutePath();
            Process checkSu = Runtime.getRuntime().exec(cmd);

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(checkSu.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(checkSu.getErrorStream()));

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
        ContactsPacket[] contactsPackets = getJsonFromDataPackets.contactPackets;

        if (contactsPackets.length == 0 && appData.size() == 0){
            showError(getString(R.string.nothing_to_restore), getString(R.string.no_metadata_found));
            return;
        }

        if (intentSelectAll){
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
        }
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
        boolean app, data;
        boolean thisEnable;
        boolean enable = false;

        numberOfApps = 0;

        if (appList.size() > 0)
            app = data = true;
        else app = data = false;
        for (int i = 0; i < appList.size(); i++) {
            try {
                app = app && appList.elementAt(i).getBoolean(APP_CHECK);
                data = data && appList.elementAt(i).getBoolean(DATA_CHECK);
                thisEnable = appList.elementAt(i).getBoolean(APP_CHECK) || appList.elementAt(i).getBoolean(DATA_CHECK);
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
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (compoundButton == appAllSelect) {
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

        onCheck(adapter.appList);
    }

}
