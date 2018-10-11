package balti.migratehelper;

import java.io.File;

public class DpiPacket {
    File dpiFile;
    boolean selected;

    public DpiPacket(File dpiFile, boolean selected) {
        this.dpiFile = dpiFile;
        this.selected = selected;
    }
}
