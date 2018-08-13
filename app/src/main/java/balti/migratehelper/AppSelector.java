package balti.migratehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

public class AppSelector extends AppCompatActivity implements OnConvertMetadataToJSON {

    ImageButton back;
    TextView title;
    CheckBox appAllSelect, dataAllSelect;
    RelativeLayout waitingLayout;
    TextView waitingStatusMessage;
    TextView waitingMessageDesc;
    ImageButton clearAll;
    ImageButton selectAll;
    TextView actionButton;

    ProgressBar justAProgress;
    ImageView errorIcon;

    FileFilter mtdFilter;

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

    class RootCopyTask extends AsyncTask{

        int rootCopyResult;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            waitingStatusMessage.setText(R.string.requesting_root);
            waitingMessageDesc.setText(R.string.initMessageShort);

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
                GetJsonFromData getJsonFromData = new GetJsonFromData(AppSelector.this, ".json", waitingMessageDesc);
                getJsonFromData.execute(mtdDirName);
            }
            else if (rootCopyResult == SCRIPT_ERROR){
                justAProgress.setVisibility(View.INVISIBLE);
                errorIcon.setVisibility(View.VISIBLE);
                waitingStatusMessage.setText(R.string.are_you_rooted);
                waitingMessageDesc.setText(R.string.are_you_rooted_desc);
                waitingMessageDesc.append("\n\n" + initError);
            }
            else {
                justAProgress.setVisibility(View.INVISIBLE);
                errorIcon.setVisibility(View.VISIBLE);
                waitingStatusMessage.setText(R.string.code_error);
                waitingMessageDesc.setText(initError);
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
        }

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

        new RootCopyTask().execute();
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
        int thisUid = getApplicationInfo().uid;

        String moveCommand = "#!sbin/sh\n\n" +
                "mv -f " + busyboxBinaryFilePath + " " + TEMP_DIR_NAME + "/busybox\n" +
                "rm -rf " + mtdDirName + "\n" +
                "mkdir -p " + mtdDirName + "\n" +
                "cp " + TEMP_DIR_NAME + "/*.json " + mtdDirName + " > /dev/null\n" +
                /*"chmod -R 700 " + mtdDirName + "\n" +
                "chown " + thisUid + ":" + thisUid + " -Rf " + mtdDirName + " > /dev/null\n" +*/
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
            while ((errLine = err.readLine()) != null)
                initError = initError + errLine + "\n";

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
    public void onConvertMetadataToJSON(Vector<JSONObject> appData) {
        if (appData.size() == 0){
            justAProgress.setVisibility(View.INVISIBLE);
            errorIcon.setVisibility(View.VISIBLE);
            waitingStatusMessage.setText(R.string.nothing_to_restore);
            waitingMessageDesc.setText(R.string.no_metadata_found);
            return;
        }

        if (intentSelectAll){
            waitingStatusMessage.setText(R.string.selecting_all);

            startService(new Intent(this, RestoreService.class));

            RestoreService.ROOT_RESTORE_TASK = new RootRestoreTask(this, timeInMillis(), appData.size(), installScriptPath, restoreDataScriptPath);
            RestoreService.ROOT_RESTORE_TASK.execute(appData);
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
    }
}
