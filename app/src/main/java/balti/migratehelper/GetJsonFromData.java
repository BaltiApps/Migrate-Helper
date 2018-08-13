package balti.migratehelper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class GetJsonFromData extends AsyncTask<String, String, Vector<JSONObject>> {

    private OnConvertMetadataToJSON classContext;
    private Context context;
    private FileFilter fileFilter;
    private TextView statusDisplayView;

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
    }

    @Override
    protected Vector<JSONObject> doInBackground(String... directoryPath) {

        File[] files = new File(directoryPath[0]).listFiles();

        int n = files.length;
        Vector<JSONObject> jsonObjects = new Vector<>(0);

        for (int i = 0; i < n; i++) {

            JSONObject jsonObject = getJsonData(files[i]);
            publishProgress((i+1) + " of " + n);
            if (jsonObject != null)
                jsonObjects.add(jsonObject);
        }

        return jsonObjects;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        statusDisplayView.setText(values[0]);
    }

    @Override
    protected void onPostExecute(Vector<JSONObject> jsonObjects) {
        super.onPostExecute(jsonObjects);
        classContext.onConvertMetadataToJSON(jsonObjects);


    }

    private JSONObject getJsonData(File file){
        JSONObject mainObject = null;
        String fullFileText = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String l;
            while ((l = reader.readLine()) != null){
                if (l.trim().toLowerCase().startsWith("icon"))
                    continue;
                fullFileText = fullFileText + l + "\n";
            }
            mainObject = new JSONObject(fullFileText);
            mainObject.put(RootRestoreTask.METADATA_FILE_FIELD, file.getAbsolutePath());
            mainObject.put(RootRestoreTask.METADATA_FILE_NAME, file.getName());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return mainObject;
    }
}
