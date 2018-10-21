package balti.migratehelper;

import java.io.File;

public class KeyboardPacket {
    File keyboardFile;
    boolean selected;

    public KeyboardPacket(File keyboardFile, boolean selected) {
        this.keyboardFile = keyboardFile;
        this.selected = selected;
    }
}
