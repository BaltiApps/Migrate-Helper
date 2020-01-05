package balti.migrate.helper.extraRestorePrepare.utils

import android.content.Context
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_ASSET_NAME

class SettingsAddonInstall(override val context: Context): AddonInstall(context, ADDON_SETTINGS_ASSET_NAME) {
    fun installAddon(): String = start()
}