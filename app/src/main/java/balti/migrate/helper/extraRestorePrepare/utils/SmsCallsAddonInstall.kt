package balti.migrate.helper.extraRestorePrepare.utils

import android.content.Context
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_ASSET_NAME
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_WORKING_DIR

class SmsCallsAddonInstall(override val context: Context, private val filePaths: ArrayList<String>): AddonInstall(context, ADDON_SMS_CALLS_ASSET_NAME){
    fun installAddon(): String {
        val moveCommands = ArrayList<String>(0)
        filePaths.forEach {
            moveCommands.add("cp $it $ADDON_SMS_CALLS_WORKING_DIR")
        }
        setCustomCommands(moveCommands)
        return start()
    }
}