package balti.migrate.helper.restoreSelectorActivity.containers

import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import java.io.File

data class WifiPacketKotlin(val wifiFile: File, override var isSelected: Boolean): GetterMarker(){
    override var iconResource: Int = R.drawable.ic_wifi_icon
    override var displayText: String = AppInstance.appContext.getString(R.string.wifi)
}