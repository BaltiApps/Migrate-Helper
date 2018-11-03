package balti.migratehelper;

import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

public class GetJsonFromDataPackets {
    Vector<JSONObject> jsonAppPackets;
    ContactsPacket contactPackets[];
    SmsPacket smsPackets[];
    CallsPacket callsPackets[];
    DpiPacket dpiPacket;
    KeyboardPacket keyboardPacket;
    File package_data;

    public GetJsonFromDataPackets(Vector<JSONObject> jsonAppPackets, File[] vcfFiles, File[] smsDBFiles, File[] callsDBFiles,
                                  File screenDpiFile, File keyboardFile, File package_data) {

        if (jsonAppPackets != null){
            this.jsonAppPackets = jsonAppPackets;
        }
        else this.jsonAppPackets = new Vector<>(0);

        if (vcfFiles != null) {
            contactPackets = new ContactsPacket[vcfFiles.length];
            for (int j = 0; j < vcfFiles.length; j++) {
                contactPackets[j] = new ContactsPacket(vcfFiles[j], true);
            }
        }
        else {
            contactPackets = new ContactsPacket[0];
        }

        if (smsDBFiles != null) {
            smsPackets = new SmsPacket[smsDBFiles.length];
            for (int j = 0; j < smsDBFiles.length; j++) {
                smsPackets[j] = new SmsPacket(smsDBFiles[j], true);
            }
        }
        else {
            smsPackets = new SmsPacket[0];
        }

        if (callsDBFiles != null) {
            callsPackets = new CallsPacket[callsDBFiles.length];
            for (int j = 0; j < callsDBFiles.length; j++) {
                callsPackets[j] = new CallsPacket(callsDBFiles[j], true);
            }
        }
        else {
            callsPackets = new CallsPacket[0];
        }

        if (screenDpiFile == null){
            dpiPacket = new DpiPacket(null, false);
        }
        else dpiPacket = new DpiPacket(screenDpiFile, true);


        if (keyboardFile == null){
            keyboardPacket = new KeyboardPacket(null, false);
        }
        else keyboardPacket = new KeyboardPacket(keyboardFile, true);

        this.package_data = package_data;
    }
}
