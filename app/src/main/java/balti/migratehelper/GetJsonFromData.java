package balti.migratehelper;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class GetJsonFromData extends AsyncTask<String, String, GetJsonFromDataPackets> {

    private OnConvertMetadataToJSON classContext;
    private Context context;
    private TextView statusDisplayView;

    static String APP_CHECK = "APP_CHECK";
    static String DATA_CHECK = "DATA_CHECK";
    static String PERM_CHECK = "PERM_CHECK";
    static String IS_PERMISSIBLE = "IS_PERMISSIBLE";

    private FileFilter jsonFilter, vcfFilter, smsDBFilter, callsDBFilter, packageDataFilter;

    private String error;

    GetJsonFromData(Context context, TextView statusDisplayView){
        this.context = context;
        classContext = (OnConvertMetadataToJSON) context;
        this.statusDisplayView = statusDisplayView;

        error = "";

        jsonFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".json");
            }
        };

        vcfFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".vcf");
            }
        };

        smsDBFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".sms.db");
            }
        };

        callsDBFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".calls.db");
            }
        };

        packageDataFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("package_data");
            }
        };
    }

    @Override
    protected GetJsonFromDataPackets doInBackground(String... directoryPath) {

        Vector<JSONObject> jsonObjects = new Vector<>(0);

        try {

            File[] files = new File(directoryPath[0]).listFiles(jsonFilter);
            if (files == null) files = new File[0];

            int n = files.length;

            for (int i = 0; i < n; i++) {

                JSONObject jsonObject = getJsonData(files[i]);
                publishProgress((i + 1) + " of " + n);
                if (jsonObject != null)
                    jsonObjects.add(jsonObject);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        File[] vcfFiles = new File[0];
        try {
            vcfFiles = new File(directoryPath[0]).listFiles(vcfFilter);
            if (vcfFiles == null) vcfFiles = new File[0];
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        File[] smsFiles = new File[0];
        try {
            smsFiles = new File(directoryPath[0]).listFiles(smsDBFilter);
            if (smsFiles == null) smsFiles = new File[0];
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        File[] callsFiles = new File[0];
        try {
            callsFiles = new File(directoryPath[0]).listFiles(callsDBFilter);
            if (callsFiles == null) callsFiles = new File[0];
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        File screenDpi = new File(directoryPath[0], "screen.dpi");
        if (!screenDpi.exists())
            screenDpi = null;

        File keyboardFile = new File(directoryPath[0], "default.kyb");
        if (!keyboardFile.exists())
            keyboardFile = null;

        File[] package_datas = new File[0];
        try {
            package_datas = new File(directoryPath[0]).listFiles(packageDataFilter);
            if (package_datas == null) package_datas = new File[0];
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        return new GetJsonFromDataPackets(jsonObjects, vcfFiles, smsFiles, callsFiles, screenDpi, keyboardFile, package_datas);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        statusDisplayView.setText(values[0]);
    }

    @Override
    protected void onPostExecute(GetJsonFromDataPackets dataPackets) {
        super.onPostExecute(dataPackets);
        classContext.onConvertMetadataToJSON(dataPackets, error);
    }

    private JSONObject getJsonData(File file){
        JSONObject mainObject = null;
        String fullFileText = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String l;
            while ((l = reader.readLine()) != null){
                fullFileText = fullFileText + l + "\n";
            }
            mainObject = new JSONObject(fullFileText);
            mainObject.put(RootRestoreTask.METADATA_FILE_FIELD, file.getAbsolutePath());
            mainObject.put(RootRestoreTask.METADATA_FILE_NAME, file.getName());
            mainObject.put(APP_CHECK, !mainObject.getString("apk").equals("NULL"));
            mainObject.put(DATA_CHECK, !mainObject.getString("data").equals("NULL"));

            mainObject.put(PERM_CHECK, mainObject.getBoolean("permissions"));
            mainObject.put(IS_PERMISSIBLE, mainObject.getBoolean("permissions"));

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }
        return mainObject;
    }
}
