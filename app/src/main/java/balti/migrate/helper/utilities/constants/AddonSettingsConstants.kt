package balti.migrate.helper.utilities.constants

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

class AddonSettingsConstants {

    companion object {

        val ADDON_SETTINGS_EXTRA_OPERATION_DO_START = "operation_start"
        val ADDON_SETTINGS_EXTRA_OPERATION_DUMMY_SU = "operation_dummy_su"

        val ADDON_SETTINGS_EXTRA_WAS_CANCELLED = "settings_was_cancelled"
        val ADDON_SETTINGS_EXTRA_ERRORS = "settings_errors"
        val ADDON_SETTINGS_EXTRA_SU_GRANTED = "settings_su_granted"
        val ADDON_SETTINGS_EXTRA_SU_ERROR = "settings_su_error"

        val ADDON_SETTINGS_EXTRA_VALUE_DPI = "value_dpi"
        val ADDON_SETTINGS_EXTRA_VALUE_ADB = "value_adb"
        val ADDON_SETTINGS_EXTRA_VALUE_KEYBOARD_TEXT = "value_keyboardText"
        val ADDON_SETTINGS_EXTRA_VALUE_FONT_SCALE = "value_fontScale"

        val ADDON_SETTINGS_RECEIVER_PACKAGE_NAME = "balti.migrate.addon.settings"
        val ADDON_SETTINGS_RECEIVER_CLASS = "balti.migrate.addon.settings.DummyActivity"

        val ADDON_SETTINGS_ASSET_NAME = "addonSettings.apk"

        fun getSettingsIntent(bundle: Bundle): Intent {
            return Intent().apply {
                component = ComponentName(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME, ADDON_SETTINGS_RECEIVER_CLASS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtras(bundle)
            }
        }
    }

}