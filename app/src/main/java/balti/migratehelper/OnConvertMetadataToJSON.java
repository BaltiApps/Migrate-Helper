package balti.migratehelper;

import org.json.JSONObject;

import java.util.Vector;

public interface OnConvertMetadataToJSON {
    void onConvertMetadataToJSON(Vector<JSONObject> appData, String error);
}
