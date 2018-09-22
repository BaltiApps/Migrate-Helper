package balti.migratehelper;

import java.io.File;

public class CallsPacket {

    File callsDBFile;
    boolean selected;

    public CallsPacket(File callsDBFile, boolean selected) {
        this.callsDBFile = callsDBFile;
        this.selected = selected;
    }
}
