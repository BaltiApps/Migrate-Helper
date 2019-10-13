package balti.migratehelper.restoreSelectorActivity.containers

import java.io.File

data class ContactsPacketKotlin(val vcfFile: File, override var isSelected: Boolean): GetterMarker()