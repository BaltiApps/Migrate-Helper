package balti.migratehelper.restoreSelectorActivity.containers

import balti.migratehelper.AppInstance
import balti.migratehelper.R
import java.io.File

data class WifiPacketKotlin(val wifiFile: File, override var isSelected: Boolean): GetterMarker(){
    override var iconResource: Int = R.drawable.ic_wifi_icon
    override var displayText: String = AppInstance.appContext.getString(R.string.wifi)
}