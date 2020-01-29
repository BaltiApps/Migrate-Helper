package balti.migrate.helper.extraRestorePrepare.utils

import android.content.Context
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_ASSET_NAME
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_WORKING_DIR

class SmsCallsAddonInstall(override val context: Context, private val filePaths: ArrayList<String>, doInstall: Boolean):
        AddonInstall(context, ADDON_SMS_CALLS_ASSET_NAME, doInstall){
    fun installAddon(): String {
        val copyCommands = ArrayList<String>(0)
        copyCommands.add("mkdir -p $ADDON_SMS_CALLS_WORKING_DIR")
        filePaths.forEach {
            copyCommands.add("cp $it $ADDON_SMS_CALLS_WORKING_DIR")
        }
        setCustomCommands(copyCommands)
        return start()
    }
}