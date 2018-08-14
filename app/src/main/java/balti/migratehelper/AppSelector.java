package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Objects;
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
    ListView appList;

    ProgressBar justAProgress;
    ImageView errorIcon;

    FileFilter mtdFilter;
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

    boolean intentSelectAll = false;

    BroadcastReceiver progressReceiver;
    IntentFilter progressReceiverIF;

    RootCopyTask rootCopyTask;
    GetJsonFromData getJsonFromData;
    AppUpdate appUpdateTask;


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

            disableBatchActions();
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

            enableBatchActions();

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

        Vector<JSONObject> appData;

        AppUpdate(Vector<JSONObject> appData){
            this.appData = appData;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitingLayout.setVisibility(View.VISIBLE);
            appList.setVisibility(View.GONE);
            disableBatchActions();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            adapter = new AppListAdapter(AppSelector.this, appData);
            return adapter;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if (o != null){
                waitingLayout.setVisibility(View.GONE);
                appList.setVisibility(View.VISIBLE);
                appList.setAdapter(adapter);

                actionButton.setText(R.string.restore);
                actionButton.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_next, 0);
                actionButton.setBackground(getResources().getDrawable(R.drawable.ic_normal_button));
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        waitingStatusMessage.setText(R.string.please_wait);
                        actionButton.setVisibility(View.INVISIBLE);

                        waitingLayout.setVisibility(View.VISIBLE);
                        appList.setVisibility(View.GONE);

                        disableBatchActions();

                        startService(new Intent(AppSelector.this, RestoreService.class));

                        RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(AppSelector.this, timeInMillis(), appData.size(), installScriptPath, restoreDataScriptPath);
                        RestoreService.ROOT_RESTORE_TASK.execute(appData);
                    }
                });

                enableBatchActions();

                onCheck(appData);
            }
            else {
                showError(getString(R.string.code_error), getString(R.string.null_adapter));
            }
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

        appList = findViewById(R.id.app_list);

        mtdDirName = new File(getExternalCacheDir(), "metadataFiles").getAbsolutePath() + "/";
        mtdFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".json");
            }
        };

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("all?", true)){
            title.setText(R.string.everything);
            intentSelectAll = true;
            (findViewById(R.id.checkbox_bar)).setVisibility(View.GONE);
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

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent toSendIntent = new Intent(AppSelector.this, ProgressActivity.class);
                toSendIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                startActivity(toSendIntent);
                finish();
            }
        };
        progressReceiverIF = new IntentFilter(getString(R.string.actionRestoreOnProgress));
        registerReceiver(progressReceiver, progressReceiverIF);

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

        busyboxBinaryFilePath = unpackAssetToInternal("busybox");
        installScriptPath = unpackAssetToInternal("installScript.sh");
        restoreDataScriptPath = unpackAssetToInternal("restoreDataScript.sh");

    }

    private String unpackAssetToInternal(String filename){

        AssetManager assetManager = getAssets();
        File unpackFile = new File(getFilesDir(), filename);
        String path = "";

        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open(filename);
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
                "cp " + TEMP_DIR_NAME + "/*.json " + mtdDirName + " > /dev/null\n" +
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
    public void onConvertMetadataToJSON(Vector<JSONObject> appData, String error) {

        if (!error.startsWith("")){
            showError(getString(R.string.code_error), error);
            return;
        }

        if (appData.size() == 0){
            showError(getString(R.string.nothing_to_restore), getString(R.string.no_metadata_found));
            return;
        }

        if (intentSelectAll){
            waitingStatusMessage.setText(R.string.selecting_all);
            actionButton.setVisibility(View.INVISIBLE);

            startService(new Intent(this, RestoreService.class));

            RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(this, timeInMillis(), appData.size(), installScriptPath, restoreDataScriptPath);
            RestoreService.ROOT_RESTORE_TASK.execute(appData);
        }
        else {
            appUpdateTask = new AppUpdate(appData);
            appUpdateTask.execute();
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
            unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
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
        actionButton.setBackground(getDrawable(R.drawable.ic_normal_button));
        actionButton.setVisibility(View.VISIBLE);

        disableBatchActions();
    }

    void disableBatchActions(){
        appAllSelect.setEnabled(false);
        dataAllSelect.setEnabled(false);
        selectAll.setEnabled(false);
        clearAll.setEnabled(false);
    }

    void enableBatchActions(){
        appAllSelect.setEnabled(true);
        dataAllSelect.setEnabled(true);
        selectAll.setEnabled(true);
        clearAll.setEnabled(true);
    }

    @Override
    public void onCheck(Vector<JSONObject> appList) {
        boolean app, data;
        boolean enable = false;
        if (appList.size() > 0)
            app = data = true;
        else app = data = false;
        for (int i = 0; i < appList.size(); i++) {
            try {
                app = app && appList.elementAt(i).getBoolean(APP_CHECK);
                data = data && appList.elementAt(i).getBoolean(DATA_CHECK);
                enable = enable || appList.elementAt(i).getBoolean(APP_CHECK) || appList.elementAt(i).getBoolean(DATA_CHECK);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        actionButton.setEnabled(enable);

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

        actionButton.setEnabled(dataAllSelect.isChecked() || appAllSelect.isChecked());
    }
}
