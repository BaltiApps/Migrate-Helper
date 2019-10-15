package balti.migratehelper.restoreSelectorActivity.containers

import balti.migratehelper.R
import java.io.File

data class SmsPacketKotlin (val smsDBFile: File, override var isSelected: Boolean): GetterMarker(){
    override var iconResource: Int = R.drawable.ic_sms_icon
    override var displayText: String = smsDBFile.name
}