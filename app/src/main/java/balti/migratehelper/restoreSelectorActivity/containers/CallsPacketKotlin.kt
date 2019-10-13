package balti.migratehelper.restoreSelectorActivity.containers

import java.io.File

data class CallsPacketKotlin(val callDBFile: File, override var isSelected: Boolean): GetterMarker()