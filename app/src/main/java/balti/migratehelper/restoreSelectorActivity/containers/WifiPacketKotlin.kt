package balti.migratehelper.restoreSelectorActivity.containers

import java.io.File

data class WifiPacketKotlin(val wifiFile: File, override var isSelected: Boolean): GetterMarker()