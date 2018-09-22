package balti.migratehelper;

import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

public class GetJsonFromDataPackets {
    Vector<JSONObject> jsonAppPackets;
    ContactsPacket contactPackets[];
    SmsPacket smsPackets[];
    CallsPacket callsPackets[];

    public GetJsonFromDataPackets(Vector<JSONObject> jsonAppPackets, File[] vcfFiles, File[] smsDBFiles, File[] callsDBFiles) {
        this.jsonAppPackets = jsonAppPackets;
        contactPackets = new ContactsPacket[vcfFiles.length];
        for (int j = 0; j < vcfFiles.length; j++){
            contactPackets[j] = new ContactsPacket(vcfFiles[j], true);
        }
        smsPackets = new SmsPacket[smsDBFiles.length];
        for (int j = 0; j < smsDBFiles.length; j++){
            smsPackets[j] = new SmsPacket(smsDBFiles[j], true);
        }
        callsPackets = new CallsPacket[callsDBFiles.length];
        for (int j = 0; j < callsDBFiles.length; j++){
            callsPackets[j] = new CallsPacket(callsDBFiles[j], true);
        }
    }
}
