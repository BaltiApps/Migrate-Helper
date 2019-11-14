package balti.migrate.helper.restoreSelectorActivity.containers

import balti.migrate.helper.R
import java.io.File

data class CallsPacketKotlin(val callDBFile: File, override var isSelected: Boolean): GetterMarker() {
    override var iconResource: Int = R.drawable.ic_call_log_icon
    override var displayText: String = callDBFile.name
}