package balti.migratehelper.restoreSelectorActivity.containers

import java.io.File

data class SmsPacketKotlin (val smsDBFile: File, override var isSelected: Boolean): GetterMarker()