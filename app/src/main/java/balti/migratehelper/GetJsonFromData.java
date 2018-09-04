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
    private FileFilter fileFilter;
    private TextView statusDisplayView;

    static String APP_CHECK = "APP_CHECK";
    static String DATA_CHECK = "DATA_CHECK";

    FileFilter jsonFilter, vcfFilter;

    String error;

    GetJsonFromData(Context context, final String metadataExtension, TextView statusDisplayView){
        this.context = context;
        classContext = (OnConvertMetadataToJSON) context;
        fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(metadataExtension);
            }
        };
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
    }

    @Override
    protected GetJsonFromDataPackets doInBackground(String... directoryPath) {

        Vector<JSONObject> jsonObjects = new Vector<>(0);
        try {

            File[] files = new File(directoryPath[0]).listFiles(jsonFilter);

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
        }
        catch (Exception e){
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }

        return new GetJsonFromDataPackets(jsonObjects, vcfFiles);
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
            mainObject.put(APP_CHECK, true);
            mainObject.put(DATA_CHECK, true);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            error = error + e.getMessage() + "\n";
        }
        return mainObject;
    }
}
