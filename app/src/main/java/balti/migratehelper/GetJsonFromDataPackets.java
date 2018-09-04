package balti.migratehelper;

import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

public class GetJsonFromDataPackets {
    Vector<JSONObject> jsonAppPackets;
    ContactsPacket contactPackets[];

    public GetJsonFromDataPackets(Vector<JSONObject> jsonAppPackets, File[] vcfFiles) {
        this.jsonAppPackets = jsonAppPackets;
        contactPackets = new ContactsPacket[vcfFiles.length];
        for (int j = 0; j < vcfFiles.length; j++){
            contactPackets[j] = new ContactsPacket(vcfFiles[j], true);
        }
    }
}
