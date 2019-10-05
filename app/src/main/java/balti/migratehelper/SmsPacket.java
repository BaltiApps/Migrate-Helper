package balti.migratehelper;

import java.io.File;

public class SmsPacket {

    File smsDBFile;
    boolean selected;

    public SmsPacket(File smsDBFile, boolean selected) {
        this.smsDBFile = smsDBFile;
        this.selected = selected;

        File[] f = new File[10];
    }
}
