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

        val ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE = "operation_type"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION = "operation_get_sms_permission"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION = "operation_get_calls_permission"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_RESTORE_SMS_PERMISSION = "operation_restore_sms_permission"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_START_RESTORE = "operation_start_restore_sms_calls"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_STOP_RESTORE = "operation_stop_restore_sms_calls"
        val ADDON_SMS_CALLS_EXTRA_OPERATION_JOB_UPDATE = "job_update"

        val ADDON_SMS_CALLS_EXTRA_MASTER_JOBCODE = "smsCalls_master_job_code"

        val ADDON_SMS_CALLS_EXTRA_FILE_NAMES = "value_file_names"

        val ADDON_SMS_CALLS_EXTRA_SMS_GRANTED = "sms_perm_granted"
        val ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED = "calls_perm_granted"

        val ADDON_SMS_CALLS_EXTRA_IS_CANCELLED = "is_smsCalls_cancelled"
        val ADDON_SMS_CALLS_EXTRA_ALL_ERRORS = "smsCalls_errors"
        val ADDON_SMS_CALLS_EXTRA_RESTORE_FINISHED = "smsCalls_restore_finished"

        val ADDON_SMS_CALLS_EXTRA_TITLE = "extra_smsCalls_title"
        val ADDON_SMS_CALLS_EXTRA_BODY = "extra_smsCalls_body"
        val ADDON_SMS_CALLS_EXTRA_PROGRESS = "extra_smsCalls_progress"
        val ADDON_SMS_CALLS_EXTRA_RESTORING_FILE_NAME = "restoring_file_name"
        val ADDON_SMS_CALLS_EXTRA_PROPERLY_RESTORED_FILES = "extra_smsCalls_properly_restored_files"
        val ADDON_SMS_CALLS_EXTRA_FINISHED_TITLE = "extra_finished_title"

        val ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE = "smsCalls_restore_type"
        val ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_SMS = 332
        val ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_CALLS = 333
    }
}