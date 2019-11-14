package balti.migrate.helper.restoreSelectorActivity.containers

import balti.migrate.helper.R
import java.io.File

data class ContactsPacketKotlin(val vcfFile: File, override var isSelected: Boolean): GetterMarker(){
    override var iconResource: Int = R.drawable.ic_contact_icon
    override var displayText: String = vcfFile.name
}