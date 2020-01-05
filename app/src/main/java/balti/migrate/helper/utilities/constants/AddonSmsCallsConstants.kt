package balti.migrate.helper.utilities.constants

import balti.migrate.helper.AppInstance

class AddonSmsCallsConstants {

    companion object {

        val ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME = "balti.migrate.addon.smsCalls"
        val ADDON_SMS_CALLS_RECEIVER_CLASS = "balti.migrate.addon.smsCalls.DummyActivity"

        val PREF_MANUAL_SMS_CALLS_ADDON_CACHE = "smsCallsManualCache"

        val ADDON_SMS_CALLS_DEFAULT_WORKING_DIR = "/sdcard/Android/data/$ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME/cache/"
        val ADDON_SMS_CALLS_WORKING_DIR : String
            get() {
                ADDON_SMS_CALLS_DEFAULT_WORKING_DIR.let {default ->
                    AppInstance.sharedPrefs.getString(PREF_MANUAL_SMS_CALLS_ADDON_CACHE, default).run {
                        return this ?: default
                    }
                }
            }

        val ADDON_SMS_CALLS_ASSET_NAME = "addonSmsCalls.apk"
    }
}