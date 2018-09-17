package balti.migratehelper;

import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

public class GetJsonFromDataPackets {
    Vector<JSONObject> jsonAppPackets;
    ContactsPacket contactPackets[];
    SmsPacket smsPackets[];

    public GetJsonFromDataPackets(Vector<JSONObject> jsonAppPackets, File[] vcfFiles, File[] smsDBFiles) {
        this.jsonAppPackets = jsonAppPackets;
        contactPackets = new ContactsPacket[vcfFiles.length];
        for (int j = 0; j < vcfFiles.length; j++){
            contactPackets[j] = new ContactsPacket(vcfFiles[j], true);
        }
        smsPackets = new SmsPacket[smsDBFiles.length];
        for (int j = 0; j < smsDBFiles.length; j++){
            smsPackets[j] = new SmsPacket(smsDBFiles[j], true);
        }
    }
}
