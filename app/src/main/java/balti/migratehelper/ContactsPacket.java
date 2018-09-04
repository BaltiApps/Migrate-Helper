package balti.migratehelper;

import java.io.File;

public class ContactsPacket {
    File vcfFile;
    boolean selected;

    public ContactsPacket(File vcfFile, boolean selected) {
        this.vcfFile = vcfFile;
        this.selected = selected;
    }
}
